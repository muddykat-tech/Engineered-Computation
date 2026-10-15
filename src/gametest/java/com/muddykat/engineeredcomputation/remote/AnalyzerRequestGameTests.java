package com.muddykat.engineeredcomputation.remote;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeckItem;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerGameTests;
import com.muddykat.engineeredcomputation.crafting.CraftingNetwork;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.NetworkStressGameTests;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECRemote;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;

@TestCategory("remote")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class AnalyzerRequestGameTests
{
	private static final BlockPos TABULATOR_AT = new BlockPos(3, 1, 12);

	private AnalyzerRequestGameTests()
	{
	}

	/**
	 * A Crafting Computer's network, and beside it a Pneumatic Network with one catalogued chest, whose Receiving
	 * Station an analyzer on the network faces.
	 */
	private record Rig(CraftingComputerGameTests.Setup crafting, BlockPos receiver, BlockPos chest,
			StorageAnalyzerBlockEntity analyzer)
	{
		ServerLevel level()
		{
			return crafting.level();
		}

		ReceivingStationBlockEntity receiverEntity()
		{
			return (ReceivingStationBlockEntity)level().getBlockEntity(receiver);
		}

		int inChest(Item item)
		{
			return ((ChestBlockEntity)level().getBlockEntity(chest)).countItem(item);
		}

		int inTray(Item item)
		{
			return receiverEntity().getTray().countItem(item);
		}
	}

	private static Rig rig(GameTestHelper helper, ItemStack inChest)
	{
		CraftingComputerGameTests.Setup crafting = CraftingComputerGameTests.setup(helper);
		ServerLevel level = helper.getLevel();
		EraDriveState tabulator = StorageGameTests.formDriveAt(helper, EraDrive.PUNCHCARD_VAULT, Direction.NORTH,
				TABULATOR_AT);
		BlockPos receiver = CraftingComputerGameTests.outsidePort(helper, EraDrive.PUNCHCARD_VAULT.geometry(),
				TABULATOR_AT, Direction.NORTH);
		level.setBlockAndUpdate(receiver, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		level.setBlockAndUpdate(receiver.above(), ECPneumatics.TUBE.get().defaultBlockState());
		BlockPos station = receiver.above(2);
		BlockPos chest = station.above();
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(station, ECPneumatics.TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.UP));
		((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, inChest);
		CatalogueCard card = new CatalogueCard(GlobalPos.of(level.dimension(), station), Component.literal("Chest"),
				CellContents.EMPTY, CatalogueCard.NEVER)
				.withTally(((TubeStationBlockEntity)level.getBlockEntity(station)).tally(), level.getGameTime());
		ItemStack deck = new ItemStack(ECCatalogue.decks().get(0).get());
		CardDeckItem.setDeck(deck, CardDeck.EMPTY.with(card));
		tabulator.getCellSlots().setStackInSlot(0, deck);

		Direction side = StorageAnalyzerGameTests.freeSide(level, receiver);
		StorageAnalyzerBlockEntity analyzer = analyzerOnNetwork(helper, crafting, receiver.relative(side),
				side.getOpposite());
		return new Rig(crafting, receiver, chest, analyzer);
	}

	private static StorageAnalyzerBlockEntity analyzerOnNetwork(GameTestHelper helper,
			CraftingComputerGameTests.Setup crafting, BlockPos pos, Direction facing)
	{
		ServerLevel level = helper.getLevel();
		CraftingComputerGameTests.cable(level, pos.relative(facing.getOpposite()), crafting.drivePort(),
				helper.absolutePos(new BlockPos(0, CraftingComputerGameTests.CABLE_Y, 0)).getY());
		level.setBlockAndUpdate(pos, ECRemote.ANALYZER.get().defaultBlockState().setValue(RemoteLinkBlock.FACING, facing));
		StorageAnalyzerBlockEntity analyzer = (StorageAnalyzerBlockEntity)level.getBlockEntity(pos);
		StorageGameTests.check(helper, crafting.network().getAnalyzers().contains(analyzer),
				"the analyzer should be on the Crafting Computer's network");
		return analyzer;
	}

	private static ItemKey key(Item item)
	{
		return ItemKey.of(new ItemStack(item));
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void anExplicitRequestIsStoredAndTheRestIsLeftAlone(GameTestHelper helper)
	{
		Rig rig = rig(helper, new ItemStack(Items.DIAMOND, 40));
		rig.receiverEntity().getTray().setItem(8, new ItemStack(Items.COBBLESTONE, 5));
		helper.runAfterDelay(2, () -> {
			StorageNetwork network = rig.crafting().network();
			long reachable = RemoteSources.of(rig.level(), network).stream()
					.mapToLong(source -> source.stock().getOrDefault(key(Items.DIAMOND), 0L)).sum();
			StorageGameTests.check(helper, reachable==40, "the analyzer should reach 40 diamonds, got "+reachable);
			StorageGameTests.check(helper, CraftingNetwork.request(rig.level(), network, network.getStorage(),
					key(Items.DIAMOND), 30).isEmpty(), "the request for 30 diamonds was refused");
			helper.succeedWhen(() -> {
				helper.assertTrue(rig.crafting().stored(Items.DIAMOND)==30,
						"30 diamonds should be stored, not "+rig.crafting().stored(Items.DIAMOND));
				helper.assertTrue(rig.inChest(Items.DIAMOND)==10, "10 diamonds should be left in the chest");
				helper.assertTrue(rig.crafting().computer().getOrders().isEmpty(), "the request should finish");
				helper.assertTrue(rig.inTray(Items.DIAMOND)==0&&rig.inTray(Items.COBBLESTONE)==5,
						"only what was requested should be taken from the Receiving Station");
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void cancellingStillStoresWhatsComing(GameTestHelper helper)
	{
		Rig rig = rig(helper, new ItemStack(Items.DIAMOND, 40));
		helper.runAfterDelay(2, () -> {
			StorageNetwork network = rig.crafting().network();
			StorageGameTests.check(helper, CraftingNetwork.request(rig.level(), network, network.getStorage(),
					key(Items.DIAMOND), 40).isEmpty(), "the request for 40 diamonds was refused");
			helper.runAfterDelay(15, () -> {
				int id = rig.crafting().computer().getOrders().get(0).id();
				StorageGameTests.check(helper, CraftingNetwork.cancel(rig.crafting().network(),
						rig.crafting().computer().getMasterPos(), id), "cancelling didn't find the request");
				helper.succeedWhen(() -> {
					helper.assertTrue(rig.crafting().computer().getOrders().isEmpty(), "the request should be gone");
					helper.assertTrue(rig.crafting().stored(Items.DIAMOND)==40&&rig.inChest(Items.DIAMOND)==0
							&&rig.inTray(Items.DIAMOND)==0, "every diamond already sent should still be stored, got "
							+rig.crafting().stored(Items.DIAMOND));
				});
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void anOrderFetchesItsIngredientsFromThePneumaticNetwork(GameTestHelper helper)
	{
		Rig rig = rig(helper, new ItemStack(Items.OAK_PLANKS, 8));
		rig.crafting().computer().getPatternSlots().setStackInSlot(0,
				CraftingComputerGameTests.sticksFloppy(rig.level()));
		helper.runAfterDelay(2, () -> {
			StorageGameTests.check(helper, rig.crafting().order(Items.STICK, 4).isEmpty(),
					"the order for sticks was refused with the planks reachable");
			helper.succeedWhen(() -> {
				helper.assertTrue(rig.crafting().stored(Items.STICK)==4,
						"4 sticks should be crafted, not "+rig.crafting().stored(Items.STICK));
				helper.assertTrue(rig.inChest(Items.OAK_PLANKS)==6, "2 planks should have been fetched");
				helper.assertTrue(rig.crafting().computer().getOrders().isEmpty(), "the order should finish");
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void anInventoryRequestIsImportedStraightAway(GameTestHelper helper)
	{
		CraftingComputerGameTests.Setup crafting = CraftingComputerGameTests.setup(helper);
		ServerLevel level = helper.getLevel();
		BlockPos chest = helper.absolutePos(new BlockPos(3, 1, 12));
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.EMERALD, 20));
		analyzerOnNetwork(helper, crafting, chest.north(), Direction.SOUTH);
		helper.runAfterDelay(2, () -> {
			StorageNetwork network = crafting.network();
			StorageGameTests.check(helper, CraftingNetwork.request(level, network, network.getStorage(),
					key(Items.EMERALD), 15).isEmpty(), "the request for 15 emeralds was refused");
			helper.runAfterDelay(3, () -> {
				StorageGameTests.check(helper, crafting.stored(Items.EMERALD)==15,
						"15 emeralds should be imported straight away, not "+crafting.stored(Items.EMERALD));
				StorageGameTests.check(helper, ((ChestBlockEntity)level.getBlockEntity(chest)).countItem(Items.EMERALD)==5,
						"5 emeralds should be left in the chest");
				StorageGameTests.check(helper, crafting.computer().getOrders().isEmpty(), "the request should finish");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void requestsOnlyKeepsPlansOffThePneumaticNetwork(GameTestHelper helper)
	{
		Rig rig = rig(helper, new ItemStack(Items.OAK_PLANKS, 8));
		rig.analyzer().setPriority(StorageAnalyzerBlockEntity.Priority.REQUESTS_ONLY);
		rig.crafting().computer().getPatternSlots().setStackInSlot(0,
				CraftingComputerGameTests.sticksFloppy(rig.level()));
		helper.runAfterDelay(2, () -> {
			StorageGameTests.check(helper, rig.crafting().order(Items.STICK, 4).isPresent(),
					"the order for sticks should be refused, as plans can't use this analyzer");
			helper.succeed();
		});
	}
}
