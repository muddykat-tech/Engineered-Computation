package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyMenu;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;

/** Filling a terminal's crafting grid and a pattern floppy from a whole recipe at once (what JEI's [+] sends). */
@TestCategory({"terminal", "crafting"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeFillGameTests
{
	private RecipeFillGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aLinkedFillLinksWhatStorageHasMostOf(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().fillGrid(sticks(Items.BIRCH_PLANKS, Items.OAK_PLANKS), false);
		ItemStack top = grid(setup, 0), below = grid(setup, 3);
		StorageGameTests.check(helper, top.is(Items.OAK_PLANKS)&&top.getCount()==1&&below.is(Items.OAK_PLANKS),
				"the grid didn't link oak planks (the stored option), one each: "+top+", "+below);
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==64, "linking took planks out of storage");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK),
				"the filled grid doesn't make sticks");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aLinkedFillLinksMissingItemsToo(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().fillGrid(sticks(Items.BIRCH_PLANKS), false);
		StorageGameTests.check(helper, grid(setup, 0).is(Items.BIRCH_PLANKS)&&grid(setup, 3).is(Items.BIRCH_PLANKS),
				"slots with nothing in storage weren't linked to their first option");
		StorageGameTests.check(helper, setup.menu().getSlot(TerminalMenu.RESULT_SLOT).getItem().is(Items.STICK),
				"the linked grid should still show its result");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anUnlinkedFillTakesOneOfEach(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.menu().fillGrid(sticks(Items.OAK_PLANKS), false);
		StorageGameTests.check(helper, grid(setup, 0).getCount()==1&&grid(setup, 3).getCount()==1,
				"an unlinked fill should take one plank per slot");
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==62, "the planks didn't come out of storage");
		setup.menu().fillGrid(sticks(Items.OAK_PLANKS), false);
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==62,
				"filling again didn't put the grid's planks back before taking more");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anUnlinkedMaxFillTakesAsManySetsAsThereAre(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.menu().fillGrid(sticks(Items.OAK_PLANKS), true);
		StorageGameTests.check(helper, grid(setup, 0).getCount()==32&&grid(setup, 3).getCount()==32,
				"64 stored planks should make 32 sets, got "+grid(setup, 0).getCount()+" and "
						+grid(setup, 3).getCount());
		StorageGameTests.check(helper, stored(setup, Items.OAK_PLANKS)==0, "storage should be out of planks");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anUnlinkedFillUsesTheInventoryToo(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.player().getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 5));
		setup.menu().fillGrid(sticks(Items.BIRCH_PLANKS), true);
		StorageGameTests.check(helper, grid(setup, 0).is(Items.BIRCH_PLANKS)&&grid(setup, 0).getCount()==2
						&&grid(setup, 3).getCount()==2,
				"5 carried planks should make 2 sets, got "+grid(setup, 0)+" and "+grid(setup, 3));
		StorageGameTests.check(helper, setup.player().getInventory().countItem(Items.BIRCH_PLANKS)==1,
				"the carried planks weren't taken");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anUnlinkedFillPutsAwayWhatWasThere(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.menu().clickMenuButton(setup.player(), TerminalMenu.TOGGLE_LINKED);
		setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+8).set(new ItemStack(Items.COBBLESTONE, 7));
		setup.menu().fillGrid(sticks(Items.OAK_PLANKS), false);
		StorageGameTests.check(helper, grid(setup, 8).isEmpty(), "the old cobblestone is still in the grid");
		StorageGameTests.check(helper, stored(setup, Items.COBBLESTONE)==7, "the old cobblestone didn't go to storage");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aCraftingPatternFillTakesOneOfEachAndWorksOutItsOutput(GameTestHelper helper)
	{
		PatternFloppyMenu menu = patternMenu(helper);
		List<ItemStack> inputs = new ArrayList<>(List.of(new ItemStack(Items.OAK_PLANKS, 5), ItemStack.EMPTY,
				ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS, 3)));
		menu.fill(new Pattern(Pattern.Mode.DIRECT, inputs, List.of(new ItemStack(Items.DIAMOND))));
		Pattern pattern = menu.pattern();
		StorageGameTests.check(helper, pattern.mode()==Pattern.Mode.DIRECT&&pattern.input(0).getCount()==1
						&&pattern.input(3).getCount()==1,
				"a crafting pattern should hold one of each input, got "+pattern.inputs());
		StorageGameTests.check(helper, pattern.output(0).is(Items.STICK)&&pattern.output(0).getCount()==4,
				"the output wasn't worked out from the recipe (4 sticks): "+pattern.output(0));
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aProcessingPatternFillKeepsCountsAndTags(GameTestHelper helper)
	{
		PatternFloppyMenu menu = patternMenu(helper);
		menu.fill(new Pattern(Pattern.Mode.ABSTRACT, List.of(new ItemStack(Items.OAK_PLANKS, 100),
				new ItemStack(Items.COAL)), List.of(new ItemStack(Items.CHARCOAL, 3)), Map.of(0, ItemTags.PLANKS),
				Map.of()));
		Pattern pattern = menu.pattern();
		StorageGameTests.check(helper, pattern.mode()==Pattern.Mode.ABSTRACT, "the pattern isn't a processing one");
		StorageGameTests.check(helper, pattern.input(0).getCount()==64&&pattern.input(1).getCount()==1,
				"input counts should be kept within a stack, got "+pattern.inputs());
		StorageGameTests.check(helper, pattern.inputTag(0).filter(ItemTags.PLANKS::equals).isPresent(),
				"the planks tag was lost");
		StorageGameTests.check(helper, pattern.output(0).is(Items.CHARCOAL)&&pattern.output(0).getCount()==3,
				"the output wasn't kept: "+pattern.output(0));
		helper.succeed();
	}

	private record Setup(StorageGameTests.MenuRecordingPlayer player, TerminalMenu menu, BlockPos terminalPos,
			ServerLevel level)
	{
	}

	/** A disk terminal on a drum drive holding 64 oak planks, its crafting grid linked (as a new one is). */
	private static Setup setup(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos terminalPos = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(terminalPos, StorageGameTests.terminal(ComputerEra.DISK));
		CraftingGrid grid = ((CraftingTerminalBlockEntity)level.getBlockEntity(terminalPos)).getCraftingGrid();
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminalPos, TerminalFeatures.CRAFTING, grid);
		menu.broadcastChanges();
		menu.setCarried(new ItemStack(Items.OAK_PLANKS, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		if(!menu.getCarried().isEmpty())
			helper.fail("couldn't store the planks");
		return new Setup(player, menu, terminalPos, level);
	}

	/** The sticks recipe's slots (top left and the one below it), each taking any of {@code planks}. */
	private static List<List<ItemStack>> sticks(Item... planks)
	{
		List<ItemStack> options = new ArrayList<>();
		for(Item plank : planks)
			options.add(new ItemStack(plank));
		List<List<ItemStack>> slots = new ArrayList<>();
		for(int i = 0; i<CraftingGrid.SIZE; i++)
			slots.add(i==0||i==3? options: List.of());
		return slots;
	}

	private static ItemStack grid(Setup setup, int slot)
	{
		return setup.menu().getSlot(TerminalMenu.CRAFT_GRID_FIRST+slot).getItem();
	}

	private static long stored(Setup setup, Item item)
	{
		return StorageNetwork.scan(setup.level(), setup.terminalPos()).getStorage()
				.getAmount(ItemKey.of(new ItemStack(item)));
	}

	private static PatternFloppyMenu patternMenu(GameTestHelper helper)
	{
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECCrafting.PATTERN_FLOPPY.get()));
		return new PatternFloppyMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
	}
}
