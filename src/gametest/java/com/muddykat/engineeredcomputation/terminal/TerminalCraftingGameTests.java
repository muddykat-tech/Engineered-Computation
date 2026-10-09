package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;

@TestCategory("terminal")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class TerminalCraftingGameTests
{
	private TerminalCraftingGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void terminalFeaturesFollowTheEra(GameTestHelper helper)
	{
		for(ComputerEra era : new ComputerEra[]{ComputerEra.PUNCHCARD, ComputerEra.DRUM})
			StorageGameTests.check(helper, !TerminalFeatures.of(era).search()&&!TerminalFeatures.of(era).crafting(),
					era+"'s terminal (the teletype, the typewriter) has a search bar or crafting grid");
		StorageGameTests.check(helper, TerminalFeatures.of(ComputerEra.TAPE).search()
						&&!TerminalFeatures.of(ComputerEra.TAPE).crafting(),
				"the Uniscope should search but not craft");
		for(ComputerEra era : new ComputerEra[]{ComputerEra.DISK, ComputerEra.FLOPPY})
			StorageGameTests.check(helper, TerminalFeatures.of(era).search()&&TerminalFeatures.of(era).crafting(),
					era+"'s terminal should search and craft");

		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		StorageGameTests.check(helper, new TerminalMenu(1, player.getInventory(), pos, TerminalFeatures.GRID)
						.slots.size()==36,
				"a terminal without a crafting grid has slots beyond the inventory");
		StorageGameTests.check(helper, new TerminalMenu(1, player.getInventory(), pos, TerminalFeatures.CRAFTING)
						.slots.size()==36+1+9,
				"a crafting terminal doesn't have a result slot and nine grid slots");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void linkedGridCraftsStraightFromStorage(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		StorageGameTests.check(helper, setup.menu().isLinked(), "a new terminal's grid isn't linked");
		sticksRecipe(setup.menu());
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK), "result is not sticks");
		setup.menu().clicked(TerminalMenu.RESULT_SLOT, 0, ClickType.QUICK_MOVE, setup.player());
		StorageGameTests.check(helper, count(setup.player(), Items.STICK)==64, "first craft didn't work");
		setup.menu().clicked(TerminalMenu.RESULT_SLOT, 0, ClickType.QUICK_MOVE, setup.player());
		StorageGameTests.check(helper, count(setup.player(), Items.STICK)==128, "second craft didn't work");
		setup.menu().clicked(TerminalMenu.RESULT_SLOT, 0, ClickType.QUICK_MOVE, setup.player());
		StorageGameTests.check(helper, count(setup.player(), Items.STICK)==128, "third craft shouldn't have worked");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==0, "planks should be gone");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST).getItem().is(Items.OAK_PLANKS), "link should still exist");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void clickingALinkedSlotLinksWithoutTakingTheStack(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().setCarried(new ItemStack(Items.OAK_PLANKS, 10));
		setup.menu().clicked(TerminalMenu.CRAFT_GRID_FIRST+4, 0, ClickType.PICKUP, setup.player());
		StorageGameTests.check(helper, setup.menu().getCarried().is(Items.OAK_PLANKS)
						&&setup.menu().getCarried().getCount()==10,
				"linking a slot took the stack off the cursor");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==64, "linking a slot changed storage");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+4).getItem().is(Items.OAK_PLANKS)
						&&setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+4).getItem().getCount()==1,
				"the slot didn't link to one plank");
		setup.menu().clicked(TerminalMenu.CRAFT_GRID_FIRST+7, 1, ClickType.PICKUP, setup.player());
		StorageGameTests.check(helper, setup.menu().getCarried().getCount()==10
						&&setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+7).getItem().is(Items.OAK_PLANKS),
				"right-clicking a second slot didn't link it, or took a plank");
		setup.menu().setCarried(ItemStack.EMPTY);
		setup.menu().clicked(TerminalMenu.CRAFT_GRID_FIRST+4, 0, ClickType.PICKUP, setup.player());
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+4).getItem().isEmpty(),
				"clicking a linked slot with an empty hand didn't clear it");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void missingIngredientsCantBeCrafted(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST).set(new ItemStack(Items.BIRCH_PLANKS));
		setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+3).set(new ItemStack(Items.BIRCH_PLANKS));
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK), "result is not sticks");
		StorageGameTests.check(helper, !setup.menu().getSlot(TerminalMenu.RESULT_SLOT).mayPickup(setup.player()), "result should not be pickupable");
		setup.menu().clicked(TerminalMenu.RESULT_SLOT, 0, ClickType.PICKUP, setup.player());
		StorageGameTests.check(helper, setup.menu().getCarried().isEmpty(), "carried should be empty");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK), "result should still be sticks");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theTerminalKeepsItsGrid(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		sticksRecipe(setup.menu());
		setup.menu().removed(setup.player());
		TerminalMenu again = open(setup.player(), setup.terminalPos(), setup.grid());
		StorageGameTests.check(helper, again.getSlot(TerminalMenu.CRAFT_GRID_FIRST).getItem().is(Items.OAK_PLANKS), "grid should still have items");
		StorageGameTests.check(helper, again.getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK), "result should be sticks");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==64, "planks should still be stored");
		again.clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		StorageGameTests.check(helper, !again.isLinked(), "should be plain now");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==62, "one plank should be taken per link");
		StorageGameTests.check(helper, again.getSlot(TerminalMenu.CRAFT_GRID_FIRST).getItem().is(Items.OAK_PLANKS), "grid should have real item");
		again.removed(setup.player());
		TerminalMenu third = open(setup.player(), setup.terminalPos(), setup.grid());
		StorageGameTests.check(helper, !third.isLinked(), "should not be linked");
		StorageGameTests.check(helper, third.getSlot(TerminalMenu.CRAFT_GRID_FIRST).getItem().is(Items.OAK_PLANKS), "grid should have items");
		StorageGameTests.check(helper, third.getSlot(TerminalMenu.CRAFT_GRID_FIRST+3).getItem().is(Items.OAK_PLANKS), "grid should have items");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==62, "planks should still be stored");
		StorageGameTests.check(helper, count(setup.player(), Items.OAK_PLANKS)==0, "player should not have planks");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void plainItemsGoToStorageWhenLinked(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+8).set(new ItemStack(Items.OAK_PLANKS, 7));
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		StorageGameTests.check(helper, setup.menu().isLinked(), "should be linked");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==71, "planks should be stored");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+8).getItem().is(Items.OAK_PLANKS), "slot should have item");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+8).getItem().getCount()==1, "slot should have count 1");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void clearButtonEmptiesEitherMode(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		sticksRecipe(setup.menu());
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.CLEAR_GRID);
		for(int i = 0; i<9; i++)
			StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+i).getItem().isEmpty(), "grid slot should be empty");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().isEmpty(), "result should be empty");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==64, "planks should be stored");
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+8).set(new ItemStack(Items.OAK_PLANKS, 7));
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.CLEAR_GRID);
		for(int i = 0; i<9; i++)
			StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+i).getItem().isEmpty(), "grid slot should be empty");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==64+7, "planks should be stored");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void breakingDropsOnlyRealItems(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		sticksRecipe(setup.menu());
		setup.level().removeBlock(setup.terminalPos(), false);
		StorageGameTests.check(helper, dropped(setup.level(), setup.terminalPos(), Items.OAK_PLANKS)==0, "planks should not be dropped");
		setup.level().setBlockAndUpdate(setup.terminalPos(), StorageGameTests.terminal(ComputerEra.DISK));
		CraftingGrid newGrid = ((CraftingTerminalBlockEntity)setup.level().getBlockEntity(setup.terminalPos())).getCraftingGrid();
		TerminalMenu menu = open(setup.player(), setup.terminalPos(), newGrid);
		menu.clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		menu.getSlot(TerminalMenu.CRAFT_GRID_FIRST+4).set(new ItemStack(Items.OAK_PLANKS, 5));
		setup.level().removeBlock(setup.terminalPos(), false);
		StorageGameTests.check(helper, dropped(setup.level(), setup.terminalPos(), Items.OAK_PLANKS)==5, "planks should be dropped");
		helper.succeed();
	}

	private record Setup(StorageGameTests.MenuRecordingPlayer player, TerminalMenu menu, BlockPos terminalPos,
			ServerLevel level, CraftingGrid grid)
	{
	}

	private static Setup setup(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos terminalPos = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(terminalPos, StorageGameTests.terminal(ComputerEra.DISK));
		CraftingGrid grid = ((CraftingTerminalBlockEntity)level.getBlockEntity(terminalPos)).getCraftingGrid();
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = open(player, terminalPos, grid);
		menu.setCarried(new ItemStack(Items.OAK_PLANKS, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		if(!menu.getCarried().isEmpty())
			helper.fail("couldn't store the planks");
		return new Setup(player, menu, terminalPos, level, grid);
	}

	private static TerminalMenu open(StorageGameTests.MenuRecordingPlayer player, BlockPos terminalPos, CraftingGrid grid)
	{
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminalPos, TerminalFeatures.CRAFTING, grid);
		menu.broadcastChanges();
		return menu;
	}

	private static void sticksRecipe(TerminalMenu menu)
	{
		menu.getSlot(TerminalMenu.CRAFT_GRID_FIRST).set(new ItemStack(Items.OAK_PLANKS));
		menu.getSlot(TerminalMenu.CRAFT_GRID_FIRST+3).set(new ItemStack(Items.OAK_PLANKS));
	}

	private static long stored(Setup setup, Item item)
	{
		return StorageNetwork.scan(setup.level(), setup.terminalPos()).getStorage().getAmount(ItemKey.of(new ItemStack(item)));
	}

	private static int count(StorageGameTests.MenuRecordingPlayer player, Item item)
	{
		return player.getInventory().countItem(item);
	}

	private static int dropped(ServerLevel level, BlockPos pos, Item item)
	{
		int count = 0;
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3));
		for(ItemEntity drop : drops)
			if(drop.getItem().is(item))
				count += drop.getItem().getCount();
		return count;
	}
}
