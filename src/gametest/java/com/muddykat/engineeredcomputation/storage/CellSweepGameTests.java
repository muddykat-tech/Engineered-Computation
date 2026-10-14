package com.muddykat.engineeredcomputation.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.CellSweep;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveMode;
import com.muddykat.engineeredcomputation.storage.persist.CellLayout;

/** A sweep removes cells the world doesn't hold, so each runs in a batch of its own, apart from other tests' cells. */
@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellSweepGameTests
{
	private static final ItemKey DIRT = ItemKey.of(new ItemStack(Items.DIRT));
	private static final List<Item> ITEMS = List.of(Items.DIRT, Items.STONE, Items.SAND, Items.GRAVEL, Items.OAK_LOG,
			Items.GLASS);

	private CellSweepGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void idListFollowsTheCells(GameTestHelper helper) throws Exception
	{
		Path dir = CellLayout.tempDir();
		try
		{
			UUID id = UUID.randomUUID();
			CellStorage storage = CellStorage.open(dir, CellSaveMode.INDEXED, 128, helper.getLevel().registryAccess());
			storage.setContents(id, contents(5));
			check(helper, storage.idsAssignedBy(System.currentTimeMillis()).contains(id), "a new cell's id isn't listed");
			storage.save();
			storage.close();
			check(helper, Files.exists(dir.resolve("ec_uuid_list.dat")), "the id list wasn't written");

			storage = CellStorage.open(dir, CellSaveMode.INDEXED, 128, helper.getLevel().registryAccess());
			check(helper, storage.idsAssignedBy(System.currentTimeMillis()).contains(id), "the id list didn't load");
			check(helper, !storage.idsAssignedBy(0).contains(id), "a listed id lost when it was assigned");
			storage.close();

			Files.delete(dir.resolve("ec_uuid_list.dat"));
			storage = CellStorage.open(dir, CellSaveMode.INDEXED, 128, helper.getLevel().registryAccess());
			check(helper, storage.idsAssignedBy(0).contains(id), "a cell saved before the list existed wasn't listed");
			storage.setContents(id, CellContents.EMPTY);
			check(helper, !storage.idsAssignedBy(Long.MAX_VALUE).contains(id), "an emptied cell is still listed");
			storage.close();
		}
		finally
		{
			CellLayout.delete(dir);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = "cellSweep", timeoutTicks = 12000)
	public static void sweepKeepsPlacedCellsAndRemovesLostOnes(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().setStackInSlot(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).getLast().get()));
		drive.getCells().getFirst().insert(DIRT, 100, false);
		UUID placed = drive.getCellSlots().getStackInSlot(0).get(ECDataComponents.CELL_UUID.get());
		UUID lost = UUID.randomUUID();
		storage.setContents(lost, contents(3));

		CellSweep sweep = CellSweep.start(server, message -> {
		});
		check(helper, sweep!=null, "a sweep was already running");
		helper.succeedWhen(() -> {
			CellSweep.Result result = sweep.result();
			helper.assertTrue(result!=null, "the sweep hasn't finished");
			check(helper, result.failure()==null, "the sweep failed: "+result.failure());
			check(helper, storage.contents(placed).types()==1, "the sweep removed a cell sitting in a drive");
			check(helper, storage.contents(lost).types()==0, "the sweep kept a cell nothing in the world holds");
			try
			{
				Map<UUID, CellContents> backup = CellSaveMode.BULK.read(result.backup(),
						new CellRecords(server.registryAccess()));
				check(helper, backup.get(lost).equals(contents(3)), "the removed cell wasn't backed up");
				Files.delete(result.backup());
			}
			catch(Exception e)
			{
				throw new IllegalStateException(e);
			}
			drive.getCells().getFirst().extract(DIRT, 100, false);
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = "cellSweepUnreadable", timeoutTicks = 12000)
	public static void sweepRemovesNothingIfAFileCantBeRead(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		UUID lost = UUID.randomUUID();
		storage.setContents(lost, contents(2));
		Path garbage = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("ec_sweep_test_garbage.dat");
		Files.write(garbage, ("not nbt "+ECDataComponents.CELL_UUID.getId()+" at all").getBytes(StandardCharsets.UTF_8));

		CellSweep sweep = CellSweep.start(server, message -> {
		});
		check(helper, sweep!=null, "a sweep was already running");
		helper.succeedWhen(() -> {
			CellSweep.Result result = sweep.result();
			helper.assertTrue(result!=null, "the sweep hasn't finished");
			check(helper, result.failure()!=null&&result.removed()==0, "the sweep carried on past an unreadable file");
			check(helper, storage.contents(lost).types()==2, "a failed sweep removed a cell");
			storage.setContents(lost, CellContents.EMPTY);
			try
			{
				Files.delete(garbage);
			}
			catch(Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
	}

	private static CellContents contents(int types)
	{
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(int i = 0; i<types; i++)
			amounts.put(ItemKey.of(new ItemStack(ITEMS.get(i))), 10L+i);
		return CellContents.of(amounts);
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		StorageGameTests.check(helper, condition, failure);
	}
}
