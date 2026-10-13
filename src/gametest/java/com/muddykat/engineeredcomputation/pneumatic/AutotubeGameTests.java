package com.muddykat.engineeredcomputation.pneumatic;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeMenu;
import com.muddykat.engineeredcomputation.pneumatic.carrier.Carrier;
import com.muddykat.engineeredcomputation.pneumatic.carrier.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

@TestCategory("pneumatic")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class AutotubeGameTests
{
	private static final String TEMPLATE = "gametest/empty_8x8x8";
	private static final int Z = 3;

	private AutotubeGameTests()
	{
	}

	@GameTest(template = TEMPLATE)
	public static void aPulseSendsTheSetAmount(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 20, true, true);
		fork.autotube(level).setAmount(5);
		BlockPos power = helper.absolutePos(new BlockPos(1, 2, Z));
		level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
		helper.runAfterDelay(30, () -> {
			check(helper, count(level, fork.source(), Items.COBBLESTONE)==15,
					"one pulse should have taken 5 cobblestone, not "+(20-count(level, fork.source(), Items.COBBLESTONE)));
			check(helper, count(level, fork.north(), Items.COBBLESTONE)+count(level, fork.south(), Items.COBBLESTONE)==5,
					"the 5 should have reached a station's chest");
			level.removeBlock(power, false);
			helper.runAfterDelay(2, () -> {
				level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
				helper.succeedWhen(() -> helper.assertTrue(count(level, fork.source(), Items.COBBLESTONE)==10,
						"a second pulse should take 5 more"));
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void forksTakeTurns(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 4, true, true);
		for(int pulse = 0; pulse<4; pulse++)
			check(helper, fork.autotube(level).fire(), "pulse "+pulse+" sent nothing");
		helper.succeedWhen(() -> {
			helper.assertTrue(count(level, fork.north(), Items.COBBLESTONE)==2,
					"the north station should have 2, not "+count(level, fork.north(), Items.COBBLESTONE));
			helper.assertTrue(count(level, fork.south(), Items.COBBLESTONE)==2,
					"the south station should have 2, not "+count(level, fork.south(), Items.COBBLESTONE));
		});
	}

	@GameTest(template = TEMPLATE)
	public static void stationsAskingForTheItemComeFirst(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 4, true, true);
		BlockPos advanced = helper.absolutePos(new BlockPos(3, 1, 5));
		level.setBlockAndUpdate(advanced, ECPneumatics.ADVANCED_TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.SOUTH));
		((TubeStationBlockEntity)level.getBlockEntity(advanced)).setFilterCard(
				FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.COBBLESTONE))));
		for(int pulse = 0; pulse<4; pulse++)
			fork.autotube(level).fire();
		helper.succeedWhen(() -> {
			helper.assertTrue(count(level, fork.south(), Items.COBBLESTONE)==4,
					"all 4 should go to the station whose filter asks for cobblestone");
			helper.assertTrue(count(level, fork.north(), Items.COBBLESTONE)==0, "the plain station took some");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void unwantedItemsGoToTheNearestReceiver(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 3, false, false);
		BlockPos near = place(helper, 4, 1, Z, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		for(int x = 4; x<=5; x++)
			place(helper, x, 1, 2, tube());
		BlockPos far = place(helper, 6, 1, 2, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		fork.autotube(level).setAmount(3);
		check(helper, fork.autotube(level).fire(), "nothing was sent");
		helper.succeedWhen(() -> {
			helper.assertTrue(((ReceivingStationBlockEntity)level.getBlockEntity(near)).getTray()
					.countItem(Items.COBBLESTONE)==3, "the nearest Receiving Station should have the 3");
			helper.assertTrue(((ReceivingStationBlockEntity)level.getBlockEntity(far)).getTray().isEmpty(),
					"the farther one got some");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void receiversTakeTurnsNearestFirst(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Receivers receivers = receivers(helper, 4);
		BlockPos at = receivers.at();
		AutotubeBlockEntity autotube = receivers.autotube(level);
		List<BlockPos> expected = List.of(receivers.near(), receivers.middle(), receivers.far(), receivers.near());
		for(int pulse = 0; pulse<expected.size(); pulse++)
		{
			check(helper, autotube.fire(), "pulse "+pulse+" sent nothing");
			List<Carrier> sent = TubeCarriers.of(level).getCarriers().stream()
					.filter(carrier -> carrier.getPath().get(0).equals(at)).toList();
			BlockPos went = sent.get(sent.size()-1).getDestination();
			check(helper, went.equals(expected.get(pulse)), "pulse "+pulse+" went to "+went.subtract(at)
					+", not "+expected.get(pulse).subtract(at));
		}
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void stackSplitSharesBetweenEveryReceiver(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Receivers receivers = receivers(helper, 20);
		AutotubeBlockEntity autotube = receivers.autotube(level);
		autotube.setSplit(true);
		autotube.setAmount(10);
		check(helper, autotube.fire(), "nothing was sent");
		int near = flying(level, receivers.near()), middle = flying(level, receivers.middle());
		int far = flying(level, receivers.far());
		check(helper, near==4&&middle==3&&far==3, "10 should split 4, 3, 3 nearest first, not "+near+", "+middle+", "+far);
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void stackSplitWithFewerItemsThanReceiversSendsToTheNearest(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Receivers receivers = receivers(helper, 2);
		AutotubeBlockEntity autotube = receivers.autotube(level);
		autotube.setSplit(true);
		autotube.setAmount(64);
		check(helper, autotube.fire(), "nothing was sent");
		check(helper, flying(level, receivers.near())==1&&flying(level, receivers.middle())==1
				&&flying(level, receivers.far())==0, "2 items should go one each to the nearest 2");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void theScreenTogglesStackSplit(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Receivers receivers = receivers(helper, 0);
		AutotubeBlockEntity autotube = receivers.autotube(level);
		AutotubeMenu menu = new AutotubeMenu(1, autotube.getBlockPos(), autotube, new SimpleContainerData(2));
		check(helper, !autotube.splits(), "an Autotube should start without stack split");
		check(helper, menu.clickMenuButton(FakePlayerFactory.getMinecraft(level), AutotubeMenu.TOGGLE_SPLIT)
				&&autotube.splits(), "the switch didn't turn stack split on");
		AutotubeBlockEntity loaded = new AutotubeBlockEntity(autotube.getBlockPos(), autotube.getBlockState());
		loaded.loadWithComponents(autotube.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
		check(helper, loaded.splits(), "stack split wasn't saved");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void anotherAutotubeFillsItsInventory(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 6, false, false);
		BlockPos chest = place(helper, 5, 1, Z, Blocks.CHEST.defaultBlockState());
		place(helper, 4, 1, Z, autotube(Direction.EAST));
		fork.autotube(level).setAmount(6);
		check(helper, fork.autotube(level).fire(), "nothing was sent");
		helper.succeedWhen(() -> helper.assertTrue(count(level, chest, Items.COBBLESTONE)==6,
				"the other Autotube should have put the 6 in its chest"));
	}

	@GameTest(template = TEMPLATE)
	public static void anAutotubeBesideTakesItemsStraightIn(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos first = place(helper, 1, 1, Z, autotube(Direction.WEST));
		place(helper, 2, 1, Z, autotube(Direction.EAST));
		BlockPos chest = place(helper, 3, 1, Z, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		AutotubeBlockEntity autotube = (AutotubeBlockEntity)level.getBlockEntity(first);
		autotube.setAmount(4);
		check(helper, autotube.fire(), "nothing was sent");
		check(helper, count(level, chest, Items.COBBLESTONE)==4&&count(level, source, Items.COBBLESTONE)==6,
				"the Autotube beside it should have put the 4 in its chest at once");
		check(helper, TubeCarriers.of(level).getCarriers().stream().noneMatch(carrier -> carrier.getPath().contains(first)),
				"a carrier was sent next door");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void anInventoryBesideTakesItemsStraightIn(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos at = place(helper, 1, 1, Z, autotube(Direction.WEST));
		BlockPos barrel = place(helper, 1, 1, Z+1, Blocks.BARREL.defaultBlockState());
		BlockState state = level.getBlockState(at);
		check(helper, state.getValue(AutotubeBlock.PORTS.get(Direction.SOUTH))==AutotubeBlock.Port.JOINED,
				"the Autotube should join the barrel beside it");
		check(helper, state.getValue(AutotubeBlock.PORTS.get(Direction.WEST))==AutotubeBlock.Port.NONE,
				"the Autotube shouldn't join the chest its intake is on");
		((ChestBlockEntity)level.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		AutotubeBlockEntity autotube = (AutotubeBlockEntity)level.getBlockEntity(at);
		autotube.setAmount(3);
		check(helper, autotube.fire(), "nothing was sent");
		check(helper, barrelCount(level, barrel)==3, "the barrel beside it should have the 3");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void inventoriesBesideTakeTurns(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos at = place(helper, 1, 1, Z, autotube(Direction.WEST));
		BlockPos north = place(helper, 1, 1, Z-1, Blocks.BARREL.defaultBlockState());
		BlockPos south = place(helper, 1, 1, Z+1, Blocks.BARREL.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		AutotubeBlockEntity autotube = (AutotubeBlockEntity)level.getBlockEntity(at);
		for(int pulse = 0; pulse<4; pulse++)
			autotube.fire();
		check(helper, barrelCount(level, north)==2&&barrelCount(level, south)==2,
				"the barrels should have 2 each, not "+barrelCount(level, north)+" and "+barrelCount(level, south));
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void stationsComeBeforeAnInventoryBeside(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 1, true, true);
		BlockPos barrel = place(helper, 1, 1, Z+1, Blocks.BARREL.defaultBlockState());
		check(helper, fork.autotube(level).fire(), "nothing was sent");
		helper.succeedWhen(() -> {
			helper.assertTrue(barrelCount(level, barrel)==0, "the barrel beside it took the item");
			helper.assertTrue(count(level, fork.north(), Items.COBBLESTONE)+count(level, fork.south(), Items.COBBLESTONE)==1,
					"the item should have gone to a station");
		});
	}

	@GameTest(template = TEMPLATE)
	public static void withNowhereToGoItemsStay(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 5, false, false);
		check(helper, !fork.autotube(level).fire(), "it sent items with nowhere to take them");
		check(helper, count(level, fork.source(), Items.COBBLESTONE)==5, "items left the chest");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void itemsAStationCantTakeComeBack(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 4, true, false);
		fork.autotube(level).setAmount(4);
		check(helper, fork.autotube(level).fire(), "nothing was sent");
		// Filling the chest while the carrier travels leaves it no room when it lands.
		ChestBlockEntity full = (ChestBlockEntity)level.getBlockEntity(fork.north());
		for(int slot = 0; slot<full.getContainerSize(); slot++)
			full.setItem(slot, new ItemStack(Items.DIRT, 64));
		helper.succeedWhen(() -> {
			helper.assertTrue(count(level, fork.source(), Items.COBBLESTONE)==4, "the 4 should be back in the chest");
			helper.assertTrue(TubeCarriers.of(level).getCarriers().stream()
					.noneMatch(carrier -> carrier.getPath().contains(fork.autotubePos())), "a carrier is still going");
		});
	}

	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void anObservingAutotubeFiresOncePerChange(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 10, true, true);
		level.setBlockAndUpdate(fork.autotubePos(), ECPneumatics.OBSERVING_AUTOTUBE.get().defaultBlockState()
				.setValue(AutotubeBlock.FACING, Direction.WEST));
		ChestBlockEntity source = (ChestBlockEntity)level.getBlockEntity(fork.source());
		helper.runAfterDelay(20, () -> {
			check(helper, source.countItem(Items.COBBLESTONE)==10, "it fired with nothing changed");
			source.setItem(5, new ItemStack(Items.DIRT));
			helper.runAfterDelay(20, () -> {
				check(helper, source.countItem(Items.COBBLESTONE)+source.countItem(Items.DIRT)==10,
						"adding an item should have fired it once");
				helper.runAfterDelay(20, () -> {
					check(helper, source.countItem(Items.COBBLESTONE)+source.countItem(Items.DIRT)==10,
							"what it sent itself counted as a change");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = TEMPLATE)
	public static void anAutotubeAnObserverAndAVacuumTubeMakeAnObservingAutotube(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(ECPneumatics.AUTOTUBE.get()),
				new ItemStack(Items.OBSERVER), new ItemStack(ECContent.component(ComputerEra.DRUM).get())));
		ItemStack made = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
		check(helper, made.is(ECPneumatics.OBSERVING_AUTOTUBE.get().asItem()), "the recipe made "+made);
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void theWrenchCutsAndReopensATubePort(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 4, true, true);
		BlockPos at = fork.autotubePos();
		BlockPos tube = at.east();
		check(helper, port(level, at, Direction.EAST)==AutotubeBlock.Port.JOINED, "the tube's port should be joined");
		check(helper, AutotubeBlock.wrench(level, at, new Vec3(15/16.0, 0.5, 0.5)), "wrenching the arm did nothing");
		check(helper, port(level, at, Direction.EAST)==AutotubeBlock.Port.CAPPED, "the cut port should be capped");
		check(helper, !level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.WEST)),
				"the tube still joins the cut port");
		check(helper, !fork.autotube(level).fire(), "it sent items through a cut port");
		check(helper, AutotubeBlock.wrench(level, at, new Vec3(13.5/16, 0.5, 0.5)), "wrenching the cap did nothing");
		check(helper, port(level, at, Direction.EAST)==AutotubeBlock.Port.JOINED, "the reopened port should be joined");
		check(helper, level.getBlockState(tube).getValue(PneumaticTubeBlock.CONNECTIONS.get(Direction.WEST)),
				"the tube should join the reopened port");
		check(helper, fork.autotube(level).fire(), "it didn't send through the reopened port");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aCutInventoryPortStaysCut(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos at = place(helper, 1, 1, Z, autotube(Direction.WEST));
		place(helper, 1, 1, Z+1, Blocks.BARREL.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		check(helper, AutotubeBlock.wrench(level, at, new Vec3(0.5, 0.5, 15/16.0)), "wrenching the arm did nothing");
		check(helper, port(level, at, Direction.SOUTH)==AutotubeBlock.Port.CAPPED, "the barrel's port should be capped");
		check(helper, !((AutotubeBlockEntity)level.getBlockEntity(at)).fire(), "it sent items into a cut port");
		AutotubeBlockEntity autotube = (AutotubeBlockEntity)level.getBlockEntity(at);
		AutotubeBlockEntity loaded = new AutotubeBlockEntity(at, autotube.getBlockState());
		loaded.loadWithComponents(autotube.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
		check(helper, loaded.isCut(Direction.SOUTH)&&!loaded.isCut(Direction.EAST), "the cut wasn't saved");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void aCutPortGoesWithItsNeighbour(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos at = place(helper, 1, 1, Z, autotube(Direction.WEST));
		BlockPos barrel = place(helper, 1, 1, Z+1, Blocks.BARREL.defaultBlockState());
		AutotubeBlock.wrench(level, at, new Vec3(0.5, 0.5, 15/16.0));
		level.setBlockAndUpdate(barrel, Blocks.AIR.defaultBlockState());
		check(helper, port(level, at, Direction.SOUTH)==AutotubeBlock.Port.NONE
				&&!((AutotubeBlockEntity)level.getBlockEntity(at)).isCut(Direction.SOUTH),
				"the cap stayed with nothing left to cut off");
		level.setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState());
		check(helper, port(level, at, Direction.SOUTH)==AutotubeBlock.Port.JOINED,
				"a barrel put back where the cut one was should join");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void theWrenchLeavesTheIntakeAndBodyAlone(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 0, false, false);
		BlockPos at = fork.autotubePos();
		check(helper, !AutotubeBlock.wrench(level, at, new Vec3(0.5/16, 0.5, 0.5)), "the wrench worked on the intake");
		check(helper, !AutotubeBlock.wrench(level, at, new Vec3(0.5, 0.5, 13/16.0)), "the wrench worked on a bare face");
		check(helper, port(level, at, Direction.WEST)==AutotubeBlock.Port.NONE
				&&port(level, at, Direction.SOUTH)==AutotubeBlock.Port.NONE, "a port changed");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void theScreenSetsTheAmount(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Fork fork = fork(helper, 0, false, false);
		AutotubeBlockEntity autotube = fork.autotube(level);
		check(helper, autotube.getAmount()==1, "an Autotube should start at 1 a pulse");
		AutotubeMenu menu = new AutotubeMenu(1, autotube.getBlockPos(), autotube, new SimpleContainerData(1));
		var player = FakePlayerFactory.getMinecraft(level);
		check(helper, menu.clickMenuButton(player, 32)&&autotube.getAmount()==32, "the screen didn't set 32");
		check(helper, !menu.clickMenuButton(player, 65)&&!menu.clickMenuButton(player, 0)&&autotube.getAmount()==32,
				"the screen set an amount outside 1-64");
		helper.succeed();
	}

	private record Fork(BlockPos source, BlockPos autotubePos, BlockPos north, BlockPos south)
	{
		AutotubeBlockEntity autotube(ServerLevel level)
		{
			return (AutotubeBlockEntity)level.getBlockEntity(autotubePos);
		}
	}

	private static Fork fork(GameTestHelper helper, int cobblestone, boolean northTakes, boolean southTakes)
	{
		ServerLevel level = helper.getLevel();
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos autotube = place(helper, 1, 1, Z, autotube(Direction.WEST));
		place(helper, 2, 1, Z, tube());
		place(helper, 3, 1, Z, tube());
		place(helper, 3, 1, Z-1, tube());
		place(helper, 3, 1, Z+1, tube());
		BlockPos northChest = place(helper, 3, 1, Z-3, Blocks.CHEST.defaultBlockState());
		BlockPos northStation = place(helper, 3, 1, Z-2, station(Direction.NORTH));
		BlockPos southChest = place(helper, 3, 1, Z+3, Blocks.CHEST.defaultBlockState());
		BlockPos southStation = place(helper, 3, 1, Z+2, station(Direction.SOUTH));
		((TubeStationBlockEntity)level.getBlockEntity(northStation)).setTakesNewItems(northTakes);
		((TubeStationBlockEntity)level.getBlockEntity(southStation)).setTakesNewItems(southTakes);
		ChestBlockEntity chest = (ChestBlockEntity)level.getBlockEntity(source);
		for(int slot = 0; cobblestone>0; slot++, cobblestone -= 64)
			chest.setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(64, cobblestone)));
		return new Fork(source, autotube, northChest, southChest);
	}

	private static int count(ServerLevel level, BlockPos chest, Item item)
	{
		return ((ChestBlockEntity)level.getBlockEntity(chest)).countItem(item);
	}

	private record Receivers(BlockPos at, BlockPos near, BlockPos middle, BlockPos far)
	{
		AutotubeBlockEntity autotube(ServerLevel level)
		{
			return (AutotubeBlockEntity)level.getBlockEntity(at);
		}
	}

	private static Receivers receivers(GameTestHelper helper, int cobblestone)
	{
		BlockPos source = place(helper, 0, 1, Z, Blocks.CHEST.defaultBlockState());
		BlockPos at = place(helper, 1, 1, Z, autotube(Direction.WEST));
		for(int x = 2; x<=5; x++)
			place(helper, x, 1, Z, tube());
		BlockPos far = place(helper, 6, 1, Z, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		BlockPos middle = place(helper, 4, 1, Z+1, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		BlockPos near = place(helper, 2, 1, Z+1, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		ChestBlockEntity chest = (ChestBlockEntity)helper.getLevel().getBlockEntity(source);
		for(int slot = 0; cobblestone>0; slot++, cobblestone -= 64)
			chest.setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(64, cobblestone)));
		return new Receivers(at, near, middle, far);
	}

	private static int flying(ServerLevel level, BlockPos destination)
	{
		return TubeCarriers.of(level).getCarriers().stream().filter(carrier -> carrier.getDestination().equals(destination))
				.mapToInt(carrier -> carrier.getItems().getCount()).sum();
	}

	private static AutotubeBlock.Port port(ServerLevel level, BlockPos at, Direction side)
	{
		return level.getBlockState(at).getValue(AutotubeBlock.PORTS.get(side));
	}

	private static int barrelCount(ServerLevel level, BlockPos barrel)
	{
		return ((BarrelBlockEntity)level.getBlockEntity(barrel)).countItem(Items.COBBLESTONE);
	}

	private static BlockPos place(GameTestHelper helper, int x, int y, int z, BlockState state)
	{
		BlockPos absolute = helper.absolutePos(new BlockPos(x, y, z));
		helper.getLevel().setBlockAndUpdate(absolute, state);
		return absolute;
	}

	private static BlockState tube()
	{
		return ECPneumatics.TUBE.get().defaultBlockState();
	}

	private static BlockState autotube(Direction facing)
	{
		return ECPneumatics.AUTOTUBE.get().defaultBlockState().setValue(AutotubeBlock.FACING, facing);
	}

	private static BlockState station(Direction facing)
	{
		return ECPneumatics.TUBE_STATION.get().defaultBlockState().setValue(TubeStationBlock.FACING, facing);
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}
}
