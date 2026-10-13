package com.muddykat.engineeredcomputation.crafting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerMenu;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.order.CraftingOrder;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternEditPayload;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFloppyMenu;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternOffer;
import com.muddykat.engineeredcomputation.crafting.processor.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.NetworkStressGameTests;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("crafting")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingComputerGameTests
{
	private static final BlockPos DRIVE_AT = new BlockPos(3, 1, 3);
	private static final BlockPos COMPUTER_AT = new BlockPos(13, 1, 3);
	private static final int CABLE_Y = 12;

	private CraftingComputerGameTests()
	{
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void directOrderCraftsFromStorage(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.OAK_PLANKS, 64);
		setup.computer.getPatternSlots().setStackInSlot(0, sticksFloppy(setup.level));
		StorageGameTests.check(helper, CraftingNetwork.craftables(setup.level, setup.network()).contains(key(Items.STICK)),
				"sticks aren't craftable with their pattern in the computer");
		StorageGameTests.check(helper, setup.order(Items.STICK, 8).isEmpty(), "the order for 8 sticks was refused");
		StorageGameTests.check(helper, setup.stored(Items.OAK_PLANKS)==60, "placing the order didn't take its planks");
		StorageGameTests.check(helper, CraftingNetwork.isProcessing(setup.network()), "the network isn't processing");
		// One craft every craftingTicks(): four sticks first, the rest after the wait.
		helper.runAfterDelay(3, () -> StorageGameTests.check(helper, setup.stored(Items.STICK)==4,
				"one craft should have made 4 sticks so far, not "+setup.stored(Items.STICK)));
		helper.runAfterDelay(ECConfig.craftingTicks()+5, () -> {
			StorageGameTests.check(helper, setup.stored(Items.STICK)==8, "the order didn't put 8 sticks in storage");
			StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(), "the finished order is still running");
			helper.succeed();
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void anOrderForACellFinishes(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		Item small = ECDataCells.cellsOf(ComputerEra.DISK).get(0).get();
		Item medium = ECDataCells.cellsOf(ComputerEra.DISK).get(1).get();
		Item large = ECDataCells.cellsOf(ComputerEra.DISK).get(2).get();
		setup.store(medium, 1);
		setup.store(small, 1);
		StorageGameTests.check(helper, setup.stored(medium)==1&&setup.stored(small)==1, "empty cells weren't stored");
		setup.computer.getPatternSlots().setStackInSlot(0, directFloppy(setup.level, new ItemStack(medium),
				new ItemStack(small)));
		StorageGameTests.check(helper, setup.order(large, 1).isEmpty(), "the order for a cell was refused");
		helper.runAfterDelay(ECConfig.craftingTicks()+5, () -> {
			StorageGameTests.check(helper, setup.stored(large)==1, "the crafted cell didn't reach storage");
			StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(), "the order for a cell never finished");
			helper.succeed();
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void chainedOrderCraftsIntermediates(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.OAK_LOG, 2);
		setup.computer.getPatternSlots().setStackInSlot(0, sticksFloppy(setup.level));
		setup.computer.getPatternSlots().setStackInSlot(1, directFloppy(setup.level, new ItemStack(Items.OAK_LOG)));
		StorageGameTests.check(helper, setup.order(Items.STICK, 8).isEmpty(), "the chained order was refused");
		helper.runAfterDelay(3*ECConfig.craftingTicks()+5, () -> {
			StorageGameTests.check(helper, setup.stored(Items.STICK)==8, "the chained order didn't make 8 sticks");
			StorageGameTests.check(helper, setup.stored(Items.OAK_LOG)==1&&setup.stored(Items.OAK_PLANKS)==0,
					"the chained order should have used one log, its planks all made into sticks");
			helper.succeed();
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void missingIngredientsAreRefused(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.computer.getPatternSlots().setStackInSlot(0, sticksFloppy(setup.level));
		StorageGameTests.check(helper, setup.order(Items.STICK, 4).equals(Optional.of(StatusCode.NOT_FOUND)),
				"an order with nothing to make it from wasn't refused with 404");
		StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(), "a refused order is running");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void abstractOrderRunsThroughAProcessor(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		StorageGameTests.check(helper, setup.order(Items.STONE, 3).isEmpty(), "the abstract order was refused");
		helper.runAfterDelay(5, () -> {
			StorageGameTests.check(helper, rig.count(rig.machine(), Items.COBBLESTONE)==3,
					"the processor didn't send the order's 3 cobblestone into the machine its top faces");
			StorageGameTests.check(helper, rig.state().status()==StatusCode.PROCESSING,
					"the processor running the order isn't 102, but "+rig.state().status());
			rig.clear(rig.machine(), Items.COBBLESTONE);
			rig.put(rig.results(), new ItemStack(Items.STONE, 3));
			helper.runAfterDelay(5, () -> {
				StorageGameTests.check(helper, setup.stored(Items.STONE)==3, "the stone didn't end up in storage");
				StorageGameTests.check(helper, rig.count(rig.results(), Items.STONE)==0,
						"the processor didn't pull the stone in");
				StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(), "the abstract order didn't finish");
				StorageGameTests.check(helper, setup.stored(Items.COBBLESTONE)==7, "more cobblestone was used than ordered");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void resultsPushedIntoTheProcessorCount(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		StorageGameTests.check(helper, setup.order(Items.STONE, 3).isEmpty(), "the abstract order was refused");
		helper.runAfterDelay(5, () -> {
			IItemHandler bottom = setup.level.getCapability(Capabilities.ItemHandler.BLOCK, rig.bottom(), Direction.UP);
			StorageGameTests.check(helper, bottom!=null&&bottom.insertItem(0, new ItemStack(Items.STONE, 3), false)
					.isEmpty(), "the processor's bottom didn't take the pushed-in stone");
			helper.runAfterDelay(3, () -> {
				StorageGameTests.check(helper, setup.computer.getOrders().isEmpty()&&setup.stored(Items.STONE)==3,
						"the pushed-in stone didn't finish the order");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void passiveImportsArentTakenForResults(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		StorageGameTests.check(helper, setup.order(Items.STONE, 3).isEmpty(), "the abstract order was refused");
		helper.runAfterDelay(5, () -> {
			setup.store(Items.STONE, 3);
			helper.runAfterDelay(3, () -> {
				StorageGameTests.check(helper, setup.computer.getOrders().size()==1
								&&setup.computer.getOrders().get(0).progress()==0,
						"stone imported into storage was taken for the order's results");
				rig.put(rig.results(), new ItemStack(Items.STONE, 3));
				helper.runAfterDelay(5, () -> {
					StorageGameTests.check(helper, setup.computer.getOrders().isEmpty()&&setup.stored(Items.STONE)==6,
							"the order's own stone didn't finish it, beside the imported stone: "
									+setup.stored(Items.STONE)+" stored");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void processorOnlyTakesWhatsWaitedFor(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		IItemHandler bottom = setup.level.getCapability(Capabilities.ItemHandler.BLOCK, rig.bottom(), Direction.UP);
		StorageGameTests.check(helper, bottom!=null&&bottom.insertItem(0, new ItemStack(Items.STONE), false).getCount()==1,
				"the processor took stone in with no order waiting for it");
		StorageGameTests.check(helper, setup.order(Items.STONE, 3).isEmpty(), "the abstract order was refused");
		rig.put(rig.results(), new ItemStack(Items.DIRT, 5));
		helper.runAfterDelay(5, () -> {
			StorageGameTests.check(helper, bottom.insertItem(0, new ItemStack(Items.DIRT), false).getCount()==1,
					"the processor took in dirt no order is waiting for");
			StorageGameTests.check(helper, rig.count(rig.results(), Items.DIRT)==5,
					"the processor pulled dirt no order is waiting for out of the machine");
			StorageGameTests.check(helper, bottom.insertItem(0, new ItemStack(Items.STONE, 5), false).getCount()==2,
					"the processor took more stone than the order is waiting for");
			helper.succeed();
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void orderedOutputTricklesInCounted(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		StorageGameTests.check(helper, setup.order(Items.STONE, 3).isEmpty(), "the abstract order was refused");
		helper.runAfterDelay(5, () -> {
			rig.put(rig.results(), new ItemStack(Items.STONE));
			helper.runAfterDelay(3, () -> {
				StorageGameTests.check(helper, setup.stored(Items.STONE)==1&&setup.computer.getOrders().size()==1,
						"after one stone: "+setup.stored(Items.STONE)+" stored, "+setup.computer.getOrders().size()
								+" orders");
				StorageGameTests.check(helper, Math.abs(setup.computer.getOrders().get(0).progress()-1/3F)<0.01,
						"one stone of three in, but the order's "+setup.computer.getOrders().get(0).progress()+" done");
				rig.put(rig.results(), new ItemStack(Items.STONE, 2));
				helper.runAfterDelay(3, () -> {
					StorageGameTests.check(helper, setup.stored(Items.STONE)==3&&setup.computer.getOrders().isEmpty(),
							"after three stone the order should be done, with all three stored");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void processorNeedsAComputer(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDriveAt(helper, EraDrive.DRUM_COMPUTER, Direction.NORTH, DRIVE_AT);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos port = outsidePort(helper, EraDrive.DRUM_COMPUTER.geometry(), DRIVE_AT, Direction.NORTH);
		BlockPos top = cable(level, port, port.above(2), port.above(2).getY());
		Setup setup = new Setup(level, drive, null, port, top);
		setup.store(Items.COBBLESTONE, 10);
		Rig rig = setup.processor(helper, smelting());
		StorageGameTests.check(helper, rig.state().status()==StatusCode.FAILED_DEPENDENCY,
				"a processor with no Crafting Computer on its network isn't 424, but "+rig.state().status());
		StorageGameTests.check(helper, setup.order(Items.STONE, 1).equals(Optional.of(StatusCode.FAILED_DEPENDENCY)),
				"an order on a network with no Crafting Computer wasn't refused with 424");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void tagOutputTakesAnyOfTheTag(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.RAW_COPPER, 4);
		Pattern pattern = new Pattern(Pattern.Mode.ABSTRACT, List.of(new ItemStack(Items.RAW_COPPER)), List.of())
				.withTag(true, 0, ItemTags.create(ResourceLocation.parse("c:ingots")));
		Rig rig = setup.processor(helper, pattern);
		List<PatternOffer> offers = CraftingNetwork.offers(setup.level, setup.network());
		StorageGameTests.check(helper, offers.size()==1&&offers.get(0).pattern().equals(pattern),
				"the pattern with a tag isn't offered: "+offers);
		StorageGameTests.check(helper, CraftingNetwork.craftables(setup.level, setup.network()).isEmpty(),
				"a pattern with a tag is craftable as an ingredient");
		StorageGameTests.check(helper, CraftingNetwork.orderOffer(setup.level, setup.network(), setup.storage(),
				new PatternOffer.Choice(offers.get(0), Map.of()), 2).isEmpty(), "the offer's order was refused");
		helper.runAfterDelay(5, () -> {
			StorageGameTests.check(helper, rig.count(rig.machine(), Items.RAW_COPPER)==2,
					"the processor didn't send 2 raw copper");
			rig.put(rig.results(), new ItemStack(Items.COPPER_INGOT, 2));
			helper.runAfterDelay(5, () -> {
				StorageGameTests.check(helper, setup.computer.getOrders().isEmpty()&&setup.stored(Items.COPPER_INGOT)==2,
						"two copper ingots didn't finish an order for two of #c:ingots");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void tagInputSendsThePick(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.RAW_IRON, 5);
		setup.store(Items.RAW_GOLD, 5);
		Pattern pattern = new Pattern(Pattern.Mode.ABSTRACT, List.of(), List.of(new ItemStack(Items.GOLD_INGOT)))
				.withTag(false, 0, ItemTags.create(ResourceLocation.parse("c:raw_materials")));
		Rig rig = setup.processor(helper, pattern);
		PatternOffer offer = CraftingNetwork.offers(setup.level, setup.network()).get(0);
		StorageGameTests.check(helper, CraftingNetwork.orderOffer(setup.level, setup.network(), setup.storage(),
						new PatternOffer.Choice(offer, Map.of()), 2).equals(Optional.of(StatusCode.NOT_FOUND)),
				"an offer was ordered with its input tag unpicked");
		StorageGameTests.check(helper, CraftingNetwork.orderOffer(setup.level, setup.network(), setup.storage(),
						new PatternOffer.Choice(offer, Map.of(0, Items.DIRT)), 2).equals(Optional.of(StatusCode.NOT_FOUND)),
				"an offer was ordered with a pick its tag doesn't hold");
		StorageGameTests.check(helper, CraftingNetwork.orderOffer(setup.level, setup.network(), setup.storage(),
				new PatternOffer.Choice(offer, Map.of(0, Items.RAW_GOLD)), 2).isEmpty(), "the picked order was refused");
		helper.runAfterDelay(5, () -> {
			StorageGameTests.check(helper, rig.count(rig.machine(), Items.RAW_GOLD)==2
							&&rig.count(rig.machine(), Items.RAW_IRON)==0,
					"the processor didn't send the 2 raw gold picked");
			StorageGameTests.check(helper, setup.stored(Items.RAW_GOLD)==3&&setup.stored(Items.RAW_IRON)==5,
					"the order took the wrong raw materials from storage");
			helper.succeed();
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void cancellingGivesTheItemsBack(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.OAK_PLANKS, 320);
		setup.computer.getPatternSlots().setStackInSlot(0, sticksFloppy(setup.level));
		StorageGameTests.check(helper, setup.order(Items.STICK, 640).isEmpty(), "the big order was refused");
		helper.runAfterDelay(5, () -> {
			StorageGameTests.check(helper, setup.computer.getOrders().size()==1, "the big order isn't running");
			int id = setup.computer.getOrders().get(0).id();
			StorageGameTests.check(helper, CraftingNetwork.cancel(setup.network(), setup.computer.getMasterPos(), id),
					"cancelling the order didn't find it");
			helper.runAfterDelay(2, () -> {
				StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(), "the cancelled order is still running");
				long planks = setup.stored(Items.OAK_PLANKS)+setup.stored(Items.STICK)/2;
				StorageGameTests.check(helper, planks==320, "cancelling lost items: "+planks+" planks' worth back of 320");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void theComputerScreenTimesAndCancelsOrders(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.store(Items.OAK_PLANKS, 320);
		setup.computer.getPatternSlots().setStackInSlot(0, sticksFloppy(setup.level));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(setup.level);
		BlockPos trigger = EraMachine.CRAFTING_COMPUTER.geometry().trigger();
		BlockHitResult hit = hitAt(helper, new Vec3(trigger.getX()+0.5, trigger.getY()+0.5, trigger.getZ()+0.99));
		setup.level.getBlockState(hit.getBlockPos()).useWithoutItem(setup.level, player, hit);
		StorageGameTests.check(helper, player.opened!=null, "using the computer didn't open its screen");
		CraftingComputerMenu menu = (CraftingComputerMenu)player.opened.createMenu(1, player.getInventory(), player);
		StorageGameTests.check(helper, setup.order(Items.STICK, 640).isEmpty(), "the big order was refused");
		helper.runAfterDelay(10, () -> {
			StorageGameTests.check(helper, setup.computer.getOrders().size()==1, "the big order isn't running");
			CraftingOrder order = setup.computer.getOrders().get(0);
			StorageGameTests.check(helper, order.age()>=9&&order.age()<=11,
					"the order should be about 10 ticks old, not "+order.age());
			menu.cancelOrder(order.id());
			helper.runAfterDelay(2, () -> {
				StorageGameTests.check(helper, setup.computer.getOrders().isEmpty(),
						"cancelling from the computer's screen didn't stop the order");
				long planks = setup.stored(Items.OAK_PLANKS)+setup.stored(Items.STICK)/2;
				StorageGameTests.check(helper, planks==320, "cancelling lost items: "+planks+" planks' worth back of 320");
				helper.succeed();
			});
		});
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void baysTakeFloppiesWhereTheyAre(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(setup.level);
		for(int slot : new int[]{0, 5})
		{
			Vec3 front = bayFront(slot);
			player.setShiftKeyDown(true);
			player.setItemInHand(InteractionHand.MAIN_HAND, sticksFloppy(setup.level));
			InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
					hitAt(helper, front)));
			StorageGameTests.check(helper, result.consumesAction()
							&&!setup.computer.getPatternSlots().getStackInSlot(slot).isEmpty()&&player.getMainHandItem().isEmpty(),
					"sneak-using a pattern floppy on bay "+slot+" didn't put it in slot "+slot);
			BlockHitResult hit = hitAt(helper, front);
			setup.level.getBlockState(hit.getBlockPos()).useWithoutItem(setup.level, player, hit);
			StorageGameTests.check(helper, player.getMainHandItem().getItem()==ECCrafting.PATTERN_FLOPPY.get()
							&&setup.computer.getPatternSlots().getStackInSlot(slot).isEmpty(),
					"sneak-clicking bay "+slot+" with an empty hand didn't take its floppy");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void patternSlotsTakeAllOrOneOfTheHeldItem(GameTestHelper helper)
	{
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECCrafting.PATTERN_FLOPPY.get()));
		PatternFloppyMenu menu = new PatternFloppyMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 10));
		menu.edit(PatternEditPayload.Action.SET, 0, 0);
		StorageGameTests.check(helper, menu.pattern().input(0).getCount()==1, "a direct slot didn't take one");
		menu.edit(PatternEditPayload.Action.TOGGLE_MODE, 0, 0);
		menu.edit(PatternEditPayload.Action.SET, 0, 0);
		menu.edit(PatternEditPayload.Action.SET, 1, 1);
		StorageGameTests.check(helper, menu.pattern().input(0).getCount()==10&&menu.pattern().input(1).getCount()==1,
				"abstract slots took "+menu.pattern().input(0).getCount()+" and "+menu.pattern().input(1).getCount()
						+", not 10 and 1");
		StorageGameTests.check(helper, menu.getCarried().getCount()==10, "setting slots took from the held stack");
		helper.succeed();
	}

	private static Vec3 bayFront(int slot)
	{
		AABB bay = EraMachine.CRAFTING_COMPUTER.geometry().bayZones().get(slot);
		return new Vec3(bay.getCenter().x, bay.getCenter().y, bay.maxZ-0.01);
	}

	private static BlockHitResult hitAt(GameTestHelper helper, Vec3 templatePoint)
	{
		BlockPos origin = helper.absolutePos(COMPUTER_AT);
		BlockPos block = StorageGameTests.world(origin, BlockPos.containing(templatePoint), Direction.NORTH);
		Vec3 hit = Vec3.atLowerCornerOf(origin).add(StorageGameTests.worldOffset(templatePoint, Direction.NORTH));
		return new BlockHitResult(hit, Direction.SOUTH, block, false);
	}

	private record Setup(ServerLevel level, EraDriveState drive, CraftingComputerState computer, BlockPos drivePort,
			BlockPos cableTop)
	{
		StorageNetwork network()
		{
			return StorageNetwork.scan(level, drivePort);
		}

		NetworkStorage storage()
		{
			return network().getStorage();
		}

		void store(Item item, int count)
		{
			storage().insert(key(item), count, false);
		}

		long stored(Item item)
		{
			return storage().getAmount(key(item));
		}

		Optional<StatusCode> order(Item item, long amount)
		{
			StorageNetwork network = network();
			return CraftingNetwork.order(level, network, network.getStorage(), key(item), amount);
		}

		Rig processor(GameTestHelper helper, Pattern pattern)
		{
			BlockPos bottom = cableTop.south();
			Block casing = ECContent.PROCESSOR_CASING.get();
			EraDriveGeometry geometry = EraMachine.DATA_PROCESSOR.geometry();
			for(BlockPos pos : geometry.shapes().keySet())
				level.setBlockAndUpdate(StorageGameTests.world(bottom, pos, Direction.NORTH), casing.defaultBlockState());
			if(!ECMultiblocks.DATA_PROCESSOR.tryForm(level, bottom, Direction.SOUTH))
				helper.fail("the processor didn't form");
			level.setBlockAndUpdate(bottom.south(), Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(bottom.above(2).south(), Blocks.CHEST.defaultBlockState());
			DataProcessorState state = DataProcessorState.at(level, bottom);
			if(state==null)
				helper.fail("the formed processor has no state");
			ItemStack floppy = new ItemStack(ECCrafting.PATTERN_FLOPPY.get());
			floppy.set(ECDataComponents.PATTERN.get(), pattern);
			state.getPatternSlots().setStackInSlot(0, floppy);
			StorageGameTests.check(helper, network().getDataProcessors().contains(state),
					"the processor isn't on the drive's network");
			return new Rig(level, state, bottom, bottom.above(2));
		}
	}

	private record Rig(ServerLevel level, DataProcessorState state, BlockPos bottom, BlockPos top)
	{
		BlockPos machine()
		{
			return top.south();
		}

		BlockPos results()
		{
			return bottom.south();
		}

		int count(BlockPos chestPos, Item item)
		{
			ChestBlockEntity chest = (ChestBlockEntity)level.getBlockEntity(chestPos);
			int count = 0;
			for(int i = 0; i<chest.getContainerSize(); i++)
				if(chest.getItem(i).is(item))
					count += chest.getItem(i).getCount();
			return count;
		}

		void put(BlockPos chestPos, ItemStack stack)
		{
			ChestBlockEntity chest = (ChestBlockEntity)level.getBlockEntity(chestPos);
			for(int i = 0; i<chest.getContainerSize(); i++)
				if(chest.getItem(i).isEmpty())
				{
					chest.setItem(i, stack);
					return;
				}
		}

		void clear(BlockPos chestPos, Item item)
		{
			ChestBlockEntity chest = (ChestBlockEntity)level.getBlockEntity(chestPos);
			for(int i = 0; i<chest.getContainerSize(); i++)
				if(chest.getItem(i).is(item))
					chest.setItem(i, ItemStack.EMPTY);
		}
	}

	private static Pattern smelting()
	{
		return new Pattern(Pattern.Mode.ABSTRACT, List.of(new ItemStack(Items.COBBLESTONE)),
				List.of(new ItemStack(Items.STONE)));
	}

	private static Setup setup(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDriveAt(helper, EraDrive.DRUM_COMPUTER, Direction.NORTH, DRIVE_AT);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		CraftingComputerState computer = formComputer(helper, COMPUTER_AT, Direction.NORTH);
		BlockPos from = outsidePort(helper, EraDrive.DRUM_COMPUTER.geometry(), DRIVE_AT, Direction.NORTH);
		BlockPos to = outsidePort(helper, EraMachine.CRAFTING_COMPUTER.geometry(), COMPUTER_AT, Direction.NORTH);
		int top = helper.absolutePos(new BlockPos(0, CABLE_Y, 0)).getY();
		BlockPos cableTop = cable(level, from, to, top);
		StorageGameTests.check(helper, StorageNetwork.scan(level, from).getCraftingComputers().size()==1,
				"the Crafting Computer isn't on the drive's network");
		return new Setup(level, drive, computer, from, cableTop);
	}

	private static CraftingComputerState formComputer(GameTestHelper helper, BlockPos at, Direction facing)
	{
		ServerLevel level = helper.getLevel();
		EraDriveGeometry geometry = EraMachine.CRAFTING_COMPUTER.geometry();
		BlockPos origin = helper.absolutePos(at);
		Block casing = ECContent.casing(ComputerEra.FLOPPY).get();
		for(BlockPos pos : geometry.shapes().keySet())
			level.setBlockAndUpdate(StorageGameTests.world(origin, pos, facing), casing.defaultBlockState());
		BlockPos trigger = StorageGameTests.world(origin, geometry.trigger(), facing);
		if(!ECMultiblocks.CRAFTING_COMPUTER.tryForm(level, trigger, facing.getOpposite()))
			helper.fail("the Crafting Computer didn't form");
		if(level.getBlockEntity(StorageGameTests.world(origin, geometry.master(), facing)) instanceof MultiblockPartBlockEntity master
				&&master.getState() instanceof CraftingComputerState state)
			return state;
		helper.fail("the formed Crafting Computer has no state");
		throw new IllegalStateException();
	}

	private static BlockPos outsidePort(GameTestHelper helper, EraDriveGeometry geometry, BlockPos at, Direction facing)
	{
		NetworkPort port = geometry.ports().get(0);
		return StorageGameTests.world(helper.absolutePos(at), port.pos(), facing)
				.relative(MultiblockOrientation.rotationFor(facing).rotate(port.side()));
	}

	private static BlockPos cable(ServerLevel level, BlockPos from, BlockPos to, int top)
	{
		BlockPos pos = from;
		place(level, pos);
		while(pos.getY()<top)
			place(level, pos = pos.above());
		BlockPos cableTop = pos;
		while(pos.getX()!=to.getX())
			place(level, pos = pos.relative(pos.getX()<to.getX()? Direction.EAST: Direction.WEST));
		while(pos.getZ()!=to.getZ())
			place(level, pos = pos.relative(pos.getZ()<to.getZ()? Direction.SOUTH: Direction.NORTH));
		while(pos.getY()>to.getY())
			place(level, pos = pos.below());
		return cableTop;
	}

	private static void place(ServerLevel level, BlockPos pos)
	{
		level.setBlockAndUpdate(pos, ECCables.block().get().defaultBlockState());
	}

	private static ItemKey key(Item item)
	{
		return ItemKey.of(new ItemStack(item));
	}

	private static ItemStack directFloppy(ServerLevel level, ItemStack... grid)
	{
		ItemStack floppy = new ItemStack(ECCrafting.PATTERN_FLOPPY.get());
		floppy.set(ECDataComponents.PATTERN.get(),
				new Pattern(Pattern.Mode.DIRECT, List.of(grid), List.of()).withCraftedOutput(level));
		return floppy;
	}

	private static ItemStack sticksFloppy(ServerLevel level)
	{
		return directFloppy(level, new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY,
				new ItemStack(Items.OAK_PLANKS));
	}
}
