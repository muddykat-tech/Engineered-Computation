package com.muddykat.engineeredcomputation.terminal;

import java.util.Arrays;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.catalogue.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;
import com.muddykat.engineeredcomputation.catalogue.CatalogueDesk;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.catalogue.ProgramDeckItem;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.CellContents;

/** Filter Cards, Program Decks and the Advanced Tube Station whose filter decides what's deposited with it. */
@TestCategory({"catalogue", "pneumatic"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class FilterCardGameTests
{
	private FilterCardGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aFilterCardTakesTheItemRightClickedOntoIt(GameTestHelper helper)
	{
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		ItemStack card = new ItemStack(ECCatalogue.FILTER_CARD.get());
		FilterCardItem item = (FilterCardItem)card.getItem();
		Slot slot = slotWith(card);
		ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 12);
		StorageGameTests.check(helper, item.overrideOtherStackedOnMe(card, cobblestone, slot, ClickAction.SECONDARY,
				player, access(new ItemStack[]{cobblestone})), "right-clicking an item onto the card wasn't handled");
		StorageGameTests.check(helper, FilterCardItem.filter(card).matches(new ItemStack(Items.COBBLESTONE))
						&&cobblestone.getCount()==12,
				"the card should filter by the cobblestone and leave it be");
		StorageGameTests.check(helper, item.overrideStackedOnOther(card, slotWith(new ItemStack(Items.DIRT)),
				ClickAction.SECONDARY, player), "right-clicking the card onto an item wasn't handled");
		StorageGameTests.check(helper, FilterCardItem.filter(card).matches(new ItemStack(Items.DIRT)),
				"right-clicked onto dirt, the card should filter by dirt");
		Slot deck = slotWith(new ItemStack(ECCatalogue.PROGRAM_DECK.get()));
		StorageGameTests.check(helper, !item.overrideStackedOnOther(card, deck, ClickAction.SECONDARY, player),
				"the card took a Program Deck as its filter, instead of filing");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aProgramDeckFilesFilterCardsOnly(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		ItemStack deck = new ItemStack(ECCatalogue.PROGRAM_DECK.get());
		ProgramDeckItem item = (ProgramDeckItem)deck.getItem();
		Slot slot = slotWith(deck);
		ItemStack[] cursor = {FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.IRON_INGOT)))};
		item.overrideOtherStackedOnMe(deck, cursor[0], slot, ClickAction.SECONDARY, player, access(cursor));
		StorageGameTests.check(helper, cursor[0].isEmpty()&&ProgramDeckItem.filters(deck).size()==1,
				"the Filter Card wasn't filed");
		cursor[0] = CatalogueCardItem.punched(new CatalogueCard(GlobalPos.of(level.dimension(), BlockPos.ZERO),
				Component.literal("Chest"), CellContents.EMPTY, CatalogueCard.NEVER));
		item.overrideOtherStackedOnMe(deck, cursor[0], slot, ClickAction.SECONDARY, player, access(cursor));
		StorageGameTests.check(helper, !cursor[0].isEmpty()&&ProgramDeckItem.filters(deck).size()==1,
				"a Program Deck took a Catalogue Card");
		cursor[0] = ItemStack.EMPTY;
		item.overrideOtherStackedOnMe(deck, ItemStack.EMPTY, slot, ClickAction.SECONDARY, player, access(cursor));
		StorageGameTests.check(helper, FilterCardItem.filter(cursor[0]).matches(new ItemStack(Items.IRON_INGOT))
						&&ProgramDeckItem.filters(deck).isEmpty(),
				"taking the card back out lost its filter");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anAllowListStationTakesOnlyWhatItsCardMatches(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 0);
		setup.station(level).setFilterCard(FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.COBBLESTONE))));
		CatalogueDesk desk = new CatalogueDesk(level, setup.teletype());
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.DIRT, 8)).getCount()==8,
				"dirt went to a station whose card asks for cobblestone");
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.COBBLESTONE, 8)).isEmpty(),
				"the cobblestone the card asks for wasn't sent (the station doesn't take new items itself)");
		StorageGameTests.check(helper, setup.station(level).accept(new ItemStack(Items.DIRT, 3)).getCount()==3,
				"dirt arriving at the station was taken in");
		helper.succeedWhen(() -> helper.assertTrue(setup.chest(level).countItem(Items.COBBLESTONE)==8,
				"the cobblestone should reach the chest"));
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aBlockListKeepsOutEvenWhatTheChestHolds(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		ItemStack deck = new ItemStack(ECCatalogue.PROGRAM_DECK.get());
		((ProgramDeckItem)deck.getItem()).setCards(deck, List.of(
				FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.DIAMOND))),
				FilterCardItem.of(ProgramEntry.of(ItemTags.LOGS))));
		setup.station(level).setFilterCard(deck);
		setup.station(level).setBlocking(true);
		CatalogueDesk desk = new CatalogueDesk(level, setup.teletype());
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.DIAMOND, 4)).getCount()==4,
				"blocked diamonds went to the chest that holds diamonds");
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.OAK_LOG, 4)).getCount()==4,
				"logs went through a block list with the logs tag");
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.COBBLESTONE, 4)).isEmpty(),
				"a block list should take what it doesn't block");
		helper.succeedWhen(() -> helper.assertTrue(setup.chest(level).countItem(Items.COBBLESTONE)==4
				&&setup.chest(level).countItem(Items.DIAMOND)==5, "only the cobblestone should reach the chest"));
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void withoutACardItsAPlainStation(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		CatalogueDesk desk = new CatalogueDesk(level, setup.teletype());
		StorageGameTests.check(helper, setup.station(level).filter()==null, "a station without a card has a filter");
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.DIAMOND, 2)).isEmpty(),
				"diamonds should go where diamonds are kept");
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.COBBLESTONE, 2)).getCount()==2,
				"new items went to a station not taking them");
		setup.station(level).setTakesNewItems(true);
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.COBBLESTONE, 2)).isEmpty(),
				"switched on, it should take new items");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aPlainStationIgnoresAFilter(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		level.setBlockAndUpdate(pos, ECPneumatics.TUBE_STATION.get().defaultBlockState());
		TubeStationBlockEntity station = (TubeStationBlockEntity)level.getBlockEntity(pos);
		station.setFilterCard(FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.DIRT))));
		StorageGameTests.check(helper, !station.filters()&&station.filter()==null,
				"a plain Tube Station shouldn't filter");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theCardAndModeAreSavedAndTheCardDropped(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		level.setBlockAndUpdate(pos, ECPneumatics.ADVANCED_TUBE_STATION.get().defaultBlockState());
		TubeStationBlockEntity station = (TubeStationBlockEntity)level.getBlockEntity(pos);
		station.setFilterCard(FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.DIRT))));
		station.setBlocking(true);
		TubeStationBlockEntity loaded = new TubeStationBlockEntity(pos, station.getBlockState());
		loaded.load(station.saveWithoutMetadata());
		StorageGameTests.check(helper, loaded.isBlocking()&&FilterCardItem.filter(loaded.getFilterCard())
				.matches(new ItemStack(Items.DIRT)), "the card or the block list wasn't saved");
		level.removeBlock(pos, false);
		boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
				.anyMatch(drop -> drop.getItem().getItem() instanceof FilterCardItem);
		StorageGameTests.check(helper, dropped, "breaking the station didn't drop its card");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aFilterCardIsDyedFromABlankCatalogueCard(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		ItemStack punched = CatalogueCardItem.punched(new CatalogueCard(GlobalPos.of(level.dimension(), BlockPos.ZERO),
				Component.literal("Chest"), CellContents.EMPTY, CatalogueCard.NEVER));
		boolean blank = crafts(level, new ItemStack(ECCatalogue.CARD.get()));
		StorageGameTests.check(helper, blank, "a blank Catalogue Card and orange dye don't make a Filter Card");
		StorageGameTests.check(helper, !crafts(level, punched), "a punched Catalogue Card was dyed into a Filter Card");
		helper.succeed();
	}

	private static boolean crafts(ServerLevel level, ItemStack card)
	{
		CraftingContainer input = Pattern.craftingGrid(2, 1, List.of(card, new ItemStack(Items.ORANGE_DYE)));
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.assemble(input, level.registryAccess()))
				.map(result -> result.getItem() instanceof FilterCardItem)
				.orElse(false);
	}

	private record Setup(BlockPos stationPos, BlockPos chestPos, BlockPos teletype)
	{
		TubeStationBlockEntity station(ServerLevel level)
		{
			return (TubeStationBlockEntity)level.getBlockEntity(stationPos);
		}

		ChestBlockEntity chest(ServerLevel level)
		{
			return (ChestBlockEntity)level.getBlockEntity(chestPos);
		}
	}

	/**
	 * CatalogueGameTests' scene with an Advanced Tube Station: a Card Catalogue, a Receiving Station by its teletype,
	 * a tube up to the station and its chest (holding {@code diamonds}), the station's card filed and tallied.
	 */
	private static Setup setup(GameTestHelper helper, int diamonds)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState catalogue = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, Direction.NORTH);
		BlockPos receiver = StorageGameTests.atPort(helper, ComputerEra.PUNCHCARD, Direction.NORTH);
		Direction out = Arrays.stream(Direction.values()).filter(d -> d.getAxis().isHorizontal()
				&&level.getBlockState(receiver.relative(d.getOpposite())).getBlock()!=Blocks.AIR)
				.findFirst().orElseThrow();
		level.setBlockAndUpdate(receiver, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		BlockPos tube = receiver.above();
		level.setBlockAndUpdate(tube, ECPneumatics.TUBE.get().defaultBlockState());
		BlockPos station = tube.above();
		BlockPos chest = station.above();
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(station, ECPneumatics.ADVANCED_TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.UP));
		BlockPos teletype = receiver.relative(out);
		level.setBlockAndUpdate(teletype, StorageGameTests.terminal(ComputerEra.PUNCHCARD));
		if(diamonds>0)
			((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, diamonds));
		TubeStationBlockEntity entity = (TubeStationBlockEntity)level.getBlockEntity(station);
		CatalogueCard card = new CatalogueCard(GlobalPos.of(level.dimension(), station), Component.literal("Chest"),
				CellContents.EMPTY, CatalogueCard.NEVER).withTally(entity.tally(), level.getGameTime());
		ItemStack deck = new ItemStack(ECCatalogue.decks().get(0).get());
		CardDeckItem.setDeck(deck, CardDeck.EMPTY.with(card));
		catalogue.getCellSlots().setStackInSlot(0, deck);
		return new Setup(station, chest, teletype);
	}

	private static Slot slotWith(ItemStack stack)
	{
		Slot slot = new Slot(new SimpleContainer(1), 0, 0, 0);
		slot.set(stack);
		return slot;
	}

	private static SlotAccess access(ItemStack[] cursor)
	{
		return new SlotAccess()
		{
			@Override
			public ItemStack get()
			{
				return cursor[0];
			}

			@Override
			public boolean set(ItemStack stack)
			{
				cursor[0] = stack;
				return true;
			}
		};
	}
}
