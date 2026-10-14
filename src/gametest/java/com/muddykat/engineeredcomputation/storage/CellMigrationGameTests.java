package com.muddykat.engineeredcomputation.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.CellStatus;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.CellSummary;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;

/** Cells saved by older versions keep their contents on the stack; these move into cell storage on first use. */
@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellMigrationGameTests
{
	private static final List<Item> ITEMS = List.of(Items.DIRT, Items.STONE, Items.SAND, Items.GRAVEL, Items.OAK_LOG);

	private CellMigrationGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void readingLeavesAnOldCellAlone(GameTestHelper helper)
	{
		ItemStack legacy = legacy(ECDataCells.cellsOf(ComputerEra.DRUM).getLast().get(), 3);
		CellContents contents = legacy.get(ECDataComponents.CELL_CONTENTS.get());
		CellInventory peeked = CellInventory.peek(legacy);
		check(helper, peeked.getContents().equals(contents)&&peeked.getTypes()==3,
				"an old cell's contents couldn't be read in place");
		CellStatus.of(legacy);
		CellInventory.getContents(legacy);
		check(helper, legacy.has(ECDataComponents.CELL_CONTENTS.get())&&!legacy.has(ECDataComponents.CELL_UUID.get()),
				"reading an old cell changed it");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void firstUseMovesContentsIntoStorage(GameTestHelper helper)
	{
		CellStorage storage = CellStorage.get(helper.getLevel().getServer());
		List<ItemStack> migrated = new ArrayList<>();
		List<EraDataCell> cells = new ArrayList<>();
		for(ComputerEra era : ComputerEra.values())
			ECDataCells.cellsOf(era).forEach(cell -> cells.add(cell.get()));
		ECDataCells.terminalDisks().forEach(disk -> cells.add(disk.get()));
		for(EraDataCell type : cells)
		{
			ItemStack stack = legacy(type, Math.min(2, type.getTotalTypes()));
			CellContents contents = stack.get(ECDataComponents.CELL_CONTENTS.get());
			int[] saves = {0};
			CellInventory cell = CellInventory.of(stack, () -> saves[0]++);
			UUID id = stack.get(ECDataComponents.CELL_UUID.get());
			String name = type.getDescriptionId();
			check(helper, !stack.has(ECDataComponents.CELL_CONTENTS.get())&&id!=null,
					name+" kept its old contents or got no id");
			check(helper, saves[0]==1, name+" didn't tell its holder to save after moving its contents");
			check(helper, cell.getContents().equals(contents)&&storage.contents(id).equals(contents),
					name+" changed its contents moving into storage");
			check(helper, new CellSummary(contents.types(), contents.totalCount())
					.equals(stack.get(ECDataComponents.CELL_SUMMARY.get())), name+" has the wrong fill summary");
			migrated.add(stack);
		}
		migrated.forEach(CellMigrationGameTests::empty);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aDriveMovesItsOldCellsIn(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		ItemStack legacy = legacy(ECDataCells.cellsOf(ComputerEra.DRUM).getLast().get(), 4);
		CellContents contents = legacy.get(ECDataComponents.CELL_CONTENTS.get());
		drive.getCellSlots().setStackInSlot(0, legacy);

		Map<ItemKey, Long> seen = new HashMap<>();
		drive.getCells().forEach(cell -> cell.collect(seen));
		Map<ItemKey, Long> expected = new HashMap<>();
		contents.entries().forEach(entry -> expected.put(entry.key(), entry.count()));
		check(helper, seen.equals(expected), "the drive saw different contents after moving them in");
		CompoundTag saved = new CompoundTag();
		drive.save(saved, helper.getLevel().registryAccess());
		check(helper, saved.toString().contains("cell_uuid")&&!saved.toString().contains("cell_contents"),
				"the drive still saves its cell the old way");
		empty(drive.getCellSlots().getStackInSlot(0));
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anEmptiedCellMatchesANewOne(GameTestHelper helper)
	{
		EraDataCell type = ECDataCells.cellsOf(ComputerEra.DISK).getFirst().get();
		ItemStack stack = legacy(type, 2);
		CellInventory.of(stack, () -> {
		});
		empty(stack);
		check(helper, ItemKey.of(stack).equals(ItemKey.of(new ItemStack(type))),
				"an emptied cell still differs from a new one: "+stack.getComponentsPatch());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cellsAreSavedBeforeChunks(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		ItemStack stack = new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).getFirst().get());
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		cell.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 640, false);
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		server.overworld().getDataStorage().save();
		Map<UUID, CellContents> onDisk = CellStorage.get(server).readBack();
		check(helper, onDisk.containsKey(id)&&onDisk.get(id).equals(cell.getContents()),
				"the overworld's saved data pass didn't write the cells");
		empty(stack);
		helper.succeed();
	}

	private static ItemStack legacy(EraDataCell type, int types)
	{
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(int i = 0; i<types; i++)
			amounts.put(ItemKey.of(new ItemStack(ITEMS.get(i))), 8L*(i+1));
		ItemStack stack = new ItemStack(type);
		stack.set(ECDataComponents.CELL_CONTENTS.get(), CellContents.of(amounts));
		return stack;
	}

	private static void empty(ItemStack stack)
	{
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		cell.getContents().entries().forEach(entry -> cell.extract(entry.key(), entry.count(), false));
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		StorageGameTests.check(helper, condition, failure);
	}
}
