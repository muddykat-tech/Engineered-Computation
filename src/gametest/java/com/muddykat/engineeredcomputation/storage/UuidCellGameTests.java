package com.muddykat.engineeredcomputation.storage;

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
import com.muddykat.engineeredcomputation.storage.cell.CellStatus;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;

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
	public static void everyStorageEraHasAUuidCell(GameTestHelper helper)
	{
		for(ComputerEra era : ComputerEra.values())
		{
			boolean has = ECDataCells.uuidCells().containsKey(era);
			StorageGameTests.check(helper, has==!ECDataCells.cellsOf(era).isEmpty(),
					era+" should have a UUID cell exactly when it has data cells");
			if(!has)
				continue;
			EraDataCell uuid = ECDataCells.uuidCells().get(era).get();
			EraDataCell top = ECDataCells.cellsOf(era).getLast().get();
			StorageGameTests.check(helper, uuid.getTotalBytes()==top.getTotalBytes()
							&&uuid.getBytesPerType()==top.getBytesPerType()&&uuid.getTotalTypes()==top.getTotalTypes(),
					era+" UUID cell isn't sized like the top tier");
			StorageGameTests.check(helper, EraDataCell.isCellOfEra(new ItemStack(uuid), era),
					era+" UUID cell isn't accepted as a cell of its era");
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void behavesLikeTheTopTier(GameTestHelper helper)
	{
		for(ComputerEra era : ECDataCells.uuidCells().keySet())
		{
			ItemStack componentStack = new ItemStack(ECDataCells.cellsOf(era).getLast().get());
			ItemStack uuidStack = new ItemStack(ECDataCells.uuidCells().get(era).get());
			int[] saves = {0, 0};
			CellInventory component = CellInventory.of(componentStack, () -> saves[0]++);
			CellInventory uuid = CellInventory.of(uuidStack, () -> saves[1]++);

			uuid.insert(KEYS.get(0), 10, true);
			StorageGameTests.check(helper, !uuidStack.has(ECDataComponents.CELL_UUID.get()),
					era+": a simulated insert gave the cell an id");

			long big = component.getTotalBytes()*CellInventory.ITEMS_PER_BYTE;
			for(int i = 0; i<KEYS.size(); i++)
			{
				ItemKey key = KEYS.get(i);
				long amount = i==0? big: 100+i;
				StorageGameTests.check(helper, component.insert(key, amount, false)==uuid.insert(key, amount, false),
						era+": insert of "+key+" differed");
				StorageGameTests.check(helper, component.extract(key, 7, false)==uuid.extract(key, 7, false),
						era+": extract of "+key+" differed");
			}
			for(ItemKey key : KEYS)
				StorageGameTests.check(helper, component.getAmount(key)==uuid.getAmount(key),
						era+": amount of "+key+" differed");
			StorageGameTests.check(helper, component.getUsedBytes()==uuid.getUsedBytes()
							&&component.getContents().equals(uuid.getContents())
							&&CellStatus.of(componentStack)==CellStatus.of(uuidStack)&&saves[0]==saves[1],
					era+": cells disagree after the same operations");
			StorageGameTests.check(helper, uuidStack.has(ECDataComponents.CELL_UUID.get())
							&&!uuidStack.has(ECDataComponents.CELL_CONTENTS.get()),
					era+": UUID cell should hold an id and no contents");

			for(ItemKey key : KEYS)
				uuid.extract(key, Long.MAX_VALUE, false);
			StorageGameTests.check(helper, uuid.getContents().types()==0,
					era+": UUID cell didn't empty");
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void contentsSurviveSaveAndLoad(GameTestHelper helper)
	{
		ItemStack stack = new ItemStack(ECDataCells.uuidCells().get(ComputerEra.DISK).get());
		CellInventory cell = CellInventory.of(stack, () -> {
		});
		cell.insert(KEYS.get(0), 1000, false);
		cell.insert(KEYS.get(1), 3, false);
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());

		CellStorage storage = CellStorage.get(helper.getLevel().getServer());
		var registries = helper.getLevel().registryAccess();
		CellStorage loaded = CellStorage.load(storage.save(new CompoundTag(), registries), registries);
		StorageGameTests.check(helper, loaded.contents(id).equals(cell.getContents()),
				"UUID cell contents changed through save/load");

		ItemStack copy = stack.copy();
		StorageGameTests.check(helper, CellInventory.of(copy, () -> {
		}).getAmount(KEYS.get(0))==1000, "a copied stack should point at the same contents");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void worksInADrive(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().setStackInSlot(0, new ItemStack(ECDataCells.uuidCells().get(ComputerEra.DRUM).get()));
		CellInventory cell = drive.getCells().getFirst();
		StorageGameTests.check(helper, cell.insert(KEYS.get(0), 500, false)==500, "drive's UUID cell refused items");
		StorageGameTests.check(helper, drive.getCells().getFirst().getAmount(KEYS.get(0))==500,
				"drive's UUID cell lost items between lookups");

		CompoundTag saved = new CompoundTag();
		drive.save(saved, helper.getLevel().registryAccess());
		StorageGameTests.check(helper, !saved.toString().contains("cell_contents"),
				"the drive saved UUID cell contents into the block");
		helper.succeed();
	}
}
