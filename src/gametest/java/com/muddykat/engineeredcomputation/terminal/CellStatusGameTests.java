package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.CellStatus;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellStatusGameTests
{
	private CellStatusGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void emptyAndFreshCells(GameTestHelper helper)
	{
		CellStatus status = CellStatus.of(ItemStack.EMPTY);
		StorageGameTests.check(helper, status==CellStatus.EMPTY,
				"empty stack should be EMPTY, got "+status);

		status = CellStatus.of(new ItemStack(ECDataCells.terminalDisks().get(0).get()));
		StorageGameTests.check(helper, status==CellStatus.SPACE,
				"a fresh disk should be SPACE, got "+status);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void typesFullIsNearlyFull(GameTestHelper helper)
	{
		ItemStack disk = new ItemStack(ECDataCells.terminalDisks().get(0).get());
		CellInventory cell = CellInventory.of(disk, () -> {
		});
		ItemKey[] keys = {ItemKey.of(new ItemStack(Items.COBBLESTONE)),
				ItemKey.of(new ItemStack(Items.DIRT)),
				ItemKey.of(new ItemStack(Items.SAND)),
				ItemKey.of(new ItemStack(Items.GRAVEL)),
				ItemKey.of(new ItemStack(Items.STONE)),
				ItemKey.of(new ItemStack(Items.OAK_LOG)),
				ItemKey.of(new ItemStack(Items.GLASS)),
				ItemKey.of(new ItemStack(Items.CLAY_BALL))};
		for(ItemKey key : keys)
			cell.insert(key, 1L, false);
		CellStatus status = CellStatus.of(disk);
		StorageGameTests.check(helper, status==CellStatus.NEARLY_FULL,
				"disk with 8 types should be NEARLY_FULL, got "+status);

		cell.insert(keys[0], 1_000_000L, false);
		status = CellStatus.of(disk);
		StorageGameTests.check(helper, status==CellStatus.FULL,
				"disk with 8 types and 1M items should be FULL, got "+status);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void bytesFullIsNearlyFull(GameTestHelper helper)
	{
		ItemStack disk = new ItemStack(ECDataCells.terminalDisks().get(0).get());
		CellInventory cell = CellInventory.of(disk, () -> {
		});
		ItemKey key = ItemKey.of(new ItemStack(Items.COBBLESTONE));
		cell.insert(key, 1_000_000L, false);
		CellStatus status = CellStatus.of(disk);
		StorageGameTests.check(helper, status==CellStatus.NEARLY_FULL,
				"disk with 1M items should be NEARLY_FULL, got "+status);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void driveSyncsCellStatus(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_WORKSTATION, Direction.NORTH,
				new BlockPos(3, 1, 3));
		drive.getCellSlots().insertItem(1, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()),
				false);
		CompoundTag tag = new CompoundTag();
		drive.writeSyncData(tag);
		CellStatus status = CellStatus.unpack(tag.getInt(EraDriveState.CELL_STATUS), 0);
		StorageGameTests.check(helper, status==CellStatus.EMPTY,
				"empty slot should be EMPTY, got "+status);
		status = CellStatus.unpack(tag.getInt(EraDriveState.CELL_STATUS), 1);
		StorageGameTests.check(helper, status==CellStatus.SPACE,
				"fresh cell should be SPACE, got "+status);

		drive.getCells().get(0).insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 100_000_000L, false);
		CompoundTag tag2 = new CompoundTag();
		drive.writeSyncData(tag2);
		status = CellStatus.unpack(tag2.getInt(EraDriveState.CELL_STATUS), 1);
		StorageGameTests.check(helper, status==CellStatus.NEARLY_FULL,
				"filled cell should be NEARLY_FULL, got "+status);
		helper.succeed();
	}
}
