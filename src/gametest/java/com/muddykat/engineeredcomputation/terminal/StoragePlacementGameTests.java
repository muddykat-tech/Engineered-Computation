package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.storage.Archivist;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.EraDataCell;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class StoragePlacementGameTests
{
	private StoragePlacementGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void newTypesGoToVarietyFirst(GameTestHelper helper)
	{
		CellInventory floppy = floppy();
		CellInventory tape = tape();
		CellInventory disk = disk();
		NetworkStorage storage = new NetworkStorage(List.of(floppy, tape, disk));
		ItemKey key = key(Items.COBBLESTONE);
		storage.insert(key, 10, false);
		StorageGameTests.check(helper, disk.getAmount(key)==10, "Disk should hold 10 cobblestone");
		StorageGameTests.check(helper, floppy.getAmount(key)==0, "Floppy should hold 0 cobblestone");
		StorageGameTests.check(helper, tape.getAmount(key)==0, "Tape should hold 0 cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void tapeTakesNewTypesOnlyWhenNothingElseCan(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory disk = disk();
		NetworkStorage storage = new NetworkStorage(List.of(tape, disk));
		List<Item> many = items(33);
		ItemKey key;

		for(int i = 0; i<32; i++)
		{
			key = key(many.get(i));
			storage.insert(key, 1, false);
		}

		for(int i = 0; i<32; i++)
		{
			key = key(many.get(i));
			StorageGameTests.check(helper, disk.getAmount(key)==1, "Disk should hold 1 of each item");
		}

		StorageGameTests.check(helper, tape.getAmount(key(many.get(32)))==0, "Tape should hold 0 of the 33rd item");

		storage.insert(key(many.get(32)), 1, false);
		StorageGameTests.check(helper, tape.getAmount(key(many.get(32)))==1, "Tape should hold 1 of the 33rd item");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void storedItemsFollowTheirCellsTapeFirst(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory disk = disk();
		tape.insert(key(Items.COBBLESTONE), 100, false);
		disk.insert(key(Items.COBBLESTONE), 10, false);
		NetworkStorage storage = new NetworkStorage(List.of(disk, tape));
		storage.insert(key(Items.COBBLESTONE), 50, false);
		StorageGameTests.check(helper, tape.getAmount(key(Items.COBBLESTONE))==150, "Tape should hold 150 cobblestone");
		StorageGameTests.check(helper, disk.getAmount(key(Items.COBBLESTONE))==10, "Disk should hold 10 cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void priorityComesBeforeRole(GameTestHelper helper)
	{
		CellInventory floppy = floppy();
		CellInventory disk = disk();
		CellInventory.setPriority(floppy.getStack(), 5);
		NetworkStorage storage = new NetworkStorage(List.of(disk, floppy));
		ItemKey key = key(Items.COBBLESTONE);
		storage.insert(key, 10, false);
		StorageGameTests.check(helper, floppy.getAmount(key)==10, "Floppy should hold 10 cobblestone");
		StorageGameTests.check(helper, disk.getAmount(key)==0, "Disk should hold 0 cobblestone");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void takingEmptiesNonTapeLowPriorityFirst(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory high = disk();
		CellInventory low = disk();
		CellInventory.setPriority(high.getStack(), 3);
		tape.insert(key(Items.COBBLESTONE), 100, false);
		high.insert(key(Items.COBBLESTONE), 10, false);
		low.insert(key(Items.COBBLESTONE), 10, false);
		NetworkStorage storage = new NetworkStorage(List.of(tape, high, low));
		storage.extract(key(Items.COBBLESTONE), 15, false);
		StorageGameTests.check(helper, low.getAmount(key(Items.COBBLESTONE))==0, "Low priority cell should be empty");
		StorageGameTests.check(helper, high.getAmount(key(Items.COBBLESTONE))==5, "High priority cell should hold 5");
		StorageGameTests.check(helper, tape.getAmount(key(Items.COBBLESTONE))==100, "Tape should hold 100");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void bigStocksAreArchivedOntoTape(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory disk = disk();
		disk.insert(key(Items.COBBLESTONE), 2000, false);
		disk.insert(key(Items.DIRT), 10, false);
		NetworkStorage storage = new NetworkStorage(List.of(disk, tape));
		StorageGameTests.check(helper, Archivist.run(storage, List.of(tape), 128)==128, "Should archive 128 items");
		StorageGameTests.check(helper, tape.getAmount(key(Items.COBBLESTONE))==128, "Tape should hold 128 cobblestone");
		StorageGameTests.check(helper, disk.getAmount(key(Items.COBBLESTONE))==1872, "Disk should hold 1872 cobblestone");

		for(int i = 0; i<20; i++)
		{
			Archivist.run(storage, List.of(tape), 128);
		}

		StorageGameTests.check(helper, tape.getAmount(key(Items.COBBLESTONE))==2000, "Tape should hold 2000 cobblestone");
		StorageGameTests.check(helper, disk.getAmount(key(Items.COBBLESTONE))==0, "Disk should hold 0 cobblestone");
		StorageGameTests.check(helper, tape.getAmount(key(Items.DIRT))==0, "Tape should hold 0 dirt");
		StorageGameTests.check(helper, disk.getAmount(key(Items.DIRT))==10, "Disk should hold 10 dirt");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void smallStocksComeBackOffTape(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory disk = disk();
		tape.insert(key(Items.DIRT), 50, false);
		NetworkStorage storage = new NetworkStorage(List.of(disk, tape));
		Archivist.run(storage, List.of(tape), 128);
		StorageGameTests.check(helper, tape.getAmount(key(Items.DIRT))==0, "Tape should hold 0 dirt");
		StorageGameTests.check(helper, disk.getAmount(key(Items.DIRT))==50, "Disk should hold 50 dirt");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void higherPriorityCellsKeepTheirStock(GameTestHelper helper)
	{
		CellInventory tape = tape();
		CellInventory disk = disk();
		CellInventory.setPriority(disk.getStack(), 5);
		disk.insert(key(Items.COBBLESTONE), 2000, false);
		NetworkStorage storage = new NetworkStorage(List.of(disk, tape));
		StorageGameTests.check(helper, Archivist.run(storage, List.of(tape), 128)==0, "Should archive 0 items");
		StorageGameTests.check(helper, tape.getAmount(key(Items.COBBLESTONE))==0, "Tape should hold 0 cobblestone");
		helper.succeed();
	}

	private static CellInventory disk()
	{
		return cell(ComputerEra.DISK);
	}

	private static CellInventory floppy()
	{
		return cell(ComputerEra.FLOPPY);
	}

	private static CellInventory tape()
	{
		return cell(ComputerEra.TAPE);
	}

	private static CellInventory cell(ComputerEra era)
	{
		return CellInventory.of(new ItemStack(ECDataCells.cellsOf(era).get(0).get()), () -> {
		});
	}

	private static ItemKey key(Item item)
	{
		return ItemKey.of(new ItemStack(item));
	}

	private static List<Item> items(int count)
	{
		return BuiltInRegistries.ITEM.stream().filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell))
				.limit(count).toList();
	}
}
