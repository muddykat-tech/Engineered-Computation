package com.muddykat.engineeredcomputation.storage;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class UuidCellGameTests
{
	private static final List<ItemKey> KEYS = List.of(ItemKey.of(new ItemStack(Items.DIRT)),
			ItemKey.of(new ItemStack(Items.STONE)), ItemKey.of(new ItemStack(Items.SAND)),
			ItemKey.of(new ItemStack(Items.GRAVEL)), ItemKey.of(new ItemStack(Items.COBBLESTONE)));

	private UuidCellGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void idIsGivenOnTheFirstRealInsert(GameTestHelper helper)
	{
		for(ComputerEra era : ComputerEra.values())
			for(var type : ECDataCells.cellsOf(era))
			{
				ItemStack stack = new ItemStack(type.get());
				CellInventory cell = CellInventory.of(stack, () -> {
				});
				cell.insert(KEYS.get(0), 8, true);
				StorageGameTests.check(helper, !stack.has(ECDataComponents.CELL_UUID.get()),
						type.getId()+": a simulated insert gave the cell an id");
				cell.insert(KEYS.get(0), 8, false);
				StorageGameTests.check(helper, stack.has(ECDataComponents.CELL_UUID.get())
								&&!stack.has(ECDataComponents.CELL_CONTENTS.get()),
						type.getId()+": a cell should hold an id and no contents");
				cell.extract(KEYS.get(0), 8, false);
			}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void contentsSurviveSaveAndLoad(GameTestHelper helper) throws IOException
	{
		ItemStack stack = new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).getLast().get());
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		cell.insert(KEYS.get(0), 1000, false);
		cell.insert(KEYS.get(1), 3, false);
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());

		CellStorage storage = CellStorage.get(helper.getLevel().getServer());
		storage.save();
		StorageGameTests.check(helper, storage.readBack().get(id).equals(cell.getContents()),
				"cell contents changed through save/load");

		ItemStack copy = stack.copy();
		StorageGameTests.check(helper, CellInventory.of(copy, () -> {
		}).getAmount(KEYS.get(0))==1000, "a copied stack should point at the same contents");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void worksInADrive(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().setStackInSlot(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).getLast().get()));
		CellInventory cell = drive.getCells().getFirst();
		StorageGameTests.check(helper, cell.insert(KEYS.get(0), 500, false)==500, "drive's cell refused items");
		StorageGameTests.check(helper, drive.getCells().getFirst().getAmount(KEYS.get(0))==500,
				"drive's cell lost items between lookups");

		CompoundTag saved = new CompoundTag();
		drive.save(saved, helper.getLevel().registryAccess());
		StorageGameTests.check(helper, !saved.toString().contains("cell_contents"),
				"the drive saved cell contents into the block");
		helper.succeed();
	}
}
