package com.muddykat.engineeredcomputation.storage;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.storage.cell.persist.BulkCellFile;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveMode;
import com.muddykat.engineeredcomputation.storage.cell.persist.IndexedCellFile;
import com.muddykat.engineeredcomputation.storage.persist.CellLayout;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellStorageGameTests
{
	private static final int SECTOR = 128;

	private CellStorageGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void roundTripsInEachMode(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Map<UUID, CellContents> cells = sample(40, 0);
		Path root = CellLayout.tempDir();
		try
		{
			for(CellSaveMode mode : CellSaveMode.values())
			{
				Path dir = root.resolve(mode.name());
				CellStorage storage = CellStorage.open(dir, mode, SECTOR, registries);
				cells.forEach(storage::setContents);
				storage.save();
				storage.close();
				expect(helper, CellStorage.open(dir, mode, SECTOR, registries), cells, mode+" round trip");
			}
		}
		finally
		{
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void switchingModeMovesTheCells(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Map<UUID, CellContents> cells = sample(40, 0);
		Path dir = CellLayout.tempDir();
		try
		{
			CellStorage indexed = CellStorage.open(dir, CellSaveMode.INDEXED, SECTOR, registries);
			cells.forEach(indexed::setContents);
			indexed.save();
			indexed.close();

			CellStorage bulk = CellStorage.open(dir, CellSaveMode.BULK, SECTOR, registries);
			expectFiles(helper, dir, CellSaveMode.BULK);
			Map<UUID, CellContents> more = sample(5, 100);
			more.forEach(bulk::setContents);
			cells.putAll(more);
			expect(helper, bulk, cells, "indexed to bulk");

			expect(helper, CellStorage.open(dir, CellSaveMode.INDEXED, SECTOR, registries), cells, "bulk to indexed");
			expectFiles(helper, dir, CellSaveMode.INDEXED);
		}
		finally
		{
			CellLayout.delete(dir);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void interruptedSwitchKeepsTheConfiguredFile(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Path dir = CellLayout.tempDir();
		try
		{
			CellStorage indexed = CellStorage.open(dir, CellSaveMode.INDEXED, SECTOR, registries);
			sample(10, 0).forEach(indexed::setContents);
			indexed.save();
			indexed.close();
			Map<UUID, CellContents> finished = sample(10, 50);
			new BulkCellFile(dir.resolve(CellSaveMode.BULK.fileName()), new CellRecords(registries))
					.save(finished, finished.keySet());

			expect(helper, CellStorage.open(dir, CellSaveMode.BULK, SECTOR, registries), finished,
					"a finished switch to bulk");
			expectFiles(helper, dir, CellSaveMode.BULK);
		}
		finally
		{
			CellLayout.delete(dir);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void newSectorSizeRewritesTheFile(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Map<UUID, CellContents> cells = sample(40, 0);
		Path dir = CellLayout.tempDir();
		try
		{
			CellStorage storage = CellStorage.open(dir, CellSaveMode.INDEXED, 512, registries);
			cells.forEach(storage::setContents);
			storage.save();
			storage.close();
			check(helper, sectorSizeOf(storage.file())==512, "file wasn't made with 512 byte sectors");

			expect(helper, CellStorage.open(dir, CellSaveMode.INDEXED, SECTOR, registries), cells, "a new sector size");
			check(helper, sectorSizeOf(storage.file())==SECTOR, "file wasn't rewritten to "+SECTOR+" byte sectors");
		}
		finally
		{
			CellLayout.delete(dir);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void compactingGivesBackGaps(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Map<UUID, CellContents> cells = sample(300, 0);
		Path root = CellLayout.tempDir();
		try
		{
			CellStorage storage = CellStorage.open(root.resolve("indexed"), CellSaveMode.INDEXED, SECTOR, registries);
			cells.forEach(storage::setContents);
			storage.save();
			List<UUID> ids = List.copyOf(cells.keySet());
			for(int i = 0; i<ids.size(); i += 2)
			{
				storage.setContents(ids.get(i), CellContents.EMPTY);
				cells.remove(ids.get(i));
			}
			CellStorage.Compaction compaction = storage.compact(SECTOR);
			check(helper, compaction!=null&&compaction.bytesAfter()<compaction.bytesBefore(),
					"compacting a half-emptied file didn't shrink it: "+compaction);
			expect(helper, storage, cells, "compacting");

			CellStorage bulk = CellStorage.open(root.resolve("bulk"), CellSaveMode.BULK, SECTOR, registries);
			check(helper, bulk.compact(SECTOR)==null, "bulk storage claimed to compact");
			bulk.close();
		}
		finally
		{
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void compactingUsesTheGivenSectorSize(GameTestHelper helper) throws Exception
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		Map<UUID, CellContents> cells = sample(40, 0);
		Path dir = CellLayout.tempDir();
		try
		{
			CellStorage storage = CellStorage.open(dir, CellSaveMode.INDEXED, SECTOR, registries);
			cells.forEach(storage::setContents);
			CellStorage.Compaction compaction = storage.compact(512);
			check(helper, compaction.sectorBefore()==SECTOR&&compaction.sectorAfter()==512,
					"compaction reported the wrong sector sizes: "+compaction);
			check(helper, sectorSizeOf(storage.file())==512, "compacting didn't rewrite the file to 512 byte sectors");
			Map<UUID, CellContents> more = sample(5, 100);
			more.forEach(storage::setContents);
			cells.putAll(more);
			expect(helper, storage, cells, "compacting to a new sector size");
			check(helper, sectorSizeOf(storage.file())==512, "saving after compacting went back to the old sector size");
		}
		finally
		{
			CellLayout.delete(dir);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void compactCommandKeepsTheWorldsCells(GameTestHelper helper) throws Exception
	{
		MinecraftServer server = helper.getLevel().getServer();
		CellStorage storage = CellStorage.get(server);
		UUID id = UUID.randomUUID();
		CellContents contents = sample(1, 7).values().iterator().next();
		storage.setContents(id, contents);
		int result = server.getCommands().getDispatcher().execute("engineeredcomputation cells compact",
				server.createCommandSourceStack());
		check(helper, result==1, "the compact command failed in "+storage.mode()+" mode");
		check(helper, storage.readBack().get(id).equals(contents), "the compact command lost a cell");
		storage.setContents(id, CellContents.EMPTY);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void indexedFileGrowsAndReusesSpace(GameTestHelper helper) throws Exception
	{
		CellLayout layout = CellLayout.synthetic(200, 8);
		Path root = CellLayout.tempDir();
		try(IndexedCellFile file = IndexedCellFile.open(root.resolve("cells.ecc"), IndexedCellFile.DEFAULT_SECTOR,
				new CellRecords(helper.getLevel().registryAccess())))
		{
			file.save(layout.live, Set.copyOf(layout.live.keySet()));
			expectRead(helper, file, layout.live, "growing the header");
			long size = file.usage().bytes();

			List<UUID> ids = List.copyOf(layout.live.keySet());
			Set<UUID> dirty = new HashSet<>();
			for(int i = 0; i<ids.size(); i += 2)
			{
				layout.live.remove(ids.get(i));
				dirty.add(ids.get(i));
			}
			file.save(layout.live, dirty);
			expectRead(helper, file, layout.live, "destroying half");

			dirty.clear();
			for(CellContents contents : CellLayout.synthetic(100, 8).live.values())
			{
				UUID fresh = UUID.randomUUID();
				layout.live.put(fresh, contents);
				dirty.add(fresh);
			}
			file.save(layout.live, dirty);
			expectRead(helper, file, layout.live, "refilling");
			StorageGameTests.check(helper, file.usage().bytes()<=size*11/10,
					"refilling freed space grew the file from "+size+" to "+file.usage().bytes());

			dirty.clear();
			CellContents extra = layout.live.values().iterator().next();
			for(UUID id : List.copyOf(layout.live.keySet()).subList(0, 20))
			{
				Map<ItemKey, Long> merged = new LinkedHashMap<>();
				for(CellContents contents : List.of(layout.live.get(id), extra))
					contents.entries().forEach(entry -> merged.merge(entry.key(), entry.count(), Long::sum));
				layout.live.put(id, CellContents.of(merged));
				dirty.add(id);
			}
			file.save(layout.live, dirty);
			expectRead(helper, file, layout.live, "records outgrowing their sectors");
		}
		finally
		{
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	private static Map<UUID, CellContents> sample(int count, int offset)
	{
		Item[] items = BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell)).toArray(Item[]::new);
		Map<UUID, CellContents> cells = new LinkedHashMap<>();
		for(int i = 0; i<count; i++)
		{
			Map<ItemKey, Long> amounts = new LinkedHashMap<>();
			for(int type = 0; type<4+i%20; type++)
				amounts.put(ItemKey.of(new ItemStack(items[(offset+i*7+type)%items.length])), 1L+i*31+type);
			cells.put(new UUID(offset, i), CellContents.of(amounts));
		}
		return cells;
	}

	private static void expect(GameTestHelper helper, CellStorage storage, Map<UUID, CellContents> cells, String step)
			throws Exception
	{
		storage.save();
		Map<UUID, CellContents> read = storage.readBack();
		storage.close();
		check(helper, read.equals(cells), "cells changed after "+step+": "+read.size()+" read, "+cells.size()+" expected");
	}

	private static void expectFiles(GameTestHelper helper, Path dir, CellSaveMode mode)
	{
		for(CellSaveMode other : CellSaveMode.values())
			check(helper, Files.exists(dir.resolve(other.fileName()))==(other==mode),
					"after switching to "+mode+", "+other+"'s file "+(other==mode? "is missing": "is still there"));
	}

	private static void expectRead(GameTestHelper helper, IndexedCellFile file, Map<UUID, CellContents> live,
			String step) throws Exception
	{
		check(helper, file.load().equals(live), "the indexed file lost or changed cells after "+step);
	}

	private static int sectorSizeOf(Path file) throws Exception
	{
		return ByteBuffer.wrap(Files.readAllBytes(file)).getInt(12);
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		StorageGameTests.check(helper, condition, failure);
	}
}
