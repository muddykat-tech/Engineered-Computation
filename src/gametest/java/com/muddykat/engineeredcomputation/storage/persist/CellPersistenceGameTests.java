package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.NetworkStressGameTests;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellPersistenceGameTests
{
	private CellPersistenceGameTests()
	{
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void floppyDrivesRoundTripInEveryStrategy(GameTestHelper helper) throws IOException
	{
		CellLayout layout = CellLayout.floppyWorld(helper, 3, 4);
		Path root = CellLayout.tempDir();
		List<CellSaveStrategy> strategies = layout.strategies(root, helper.getLevel().registryAccess());
		try
		{
			for(CellSaveStrategy strategy : strategies)
			{
				strategy.save(layout.live, Set.copyOf(layout.live.keySet()));
				expectLoaded(helper, strategy, layout.live, "first save");
			}

			Set<UUID> dirty = new HashSet<>();
			UUID touched = layout.live.keySet().iterator().next();
			layout.touch(touched);
			dirty.add(touched);
			UUID moved = layout.cellsIn(layout.network, "network_0").getLast();
			layout.moveTo(moved, layout.cellsIn(layout.network, "network_2").getFirst());
			dirty.add(moved);
			UUID destroyed = layout.cellsIn(layout.network, "network_1").getFirst();
			layout.live.remove(destroyed);
			dirty.add(destroyed);

			for(CellSaveStrategy strategy : strategies)
			{
				strategy.save(layout.live, dirty);
				expectLoaded(helper, strategy, layout.live, "a touch, a move and a destroy");
			}
		}
		finally
		{
			for(CellSaveStrategy strategy : strategies)
				strategy.close();
			layout.emptyWorldCells();
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void indexedFileGrowsAndReusesSpace(GameTestHelper helper) throws IOException
	{
		CellLayout layout = CellLayout.synthetic(200, 8);
		Path root = CellLayout.tempDir();
		try(RegionFileStrategy file = new RegionFileStrategy(root.resolve("cells.ecc"),
				helper.getLevel().registryAccess()))
		{
			file.save(layout.live, Set.copyOf(layout.live.keySet()));
			StorageGameTests.check(helper, file.headerSectors()>1, "200 cells should have grown the header");
			expectLoaded(helper, file, layout.live, "growing the header");
			long size = file.usage().bytes();

			List<UUID> ids = List.copyOf(layout.live.keySet());
			Set<UUID> dirty = new HashSet<>();
			for(int i = 0; i<ids.size(); i += 2)
			{
				layout.live.remove(ids.get(i));
				dirty.add(ids.get(i));
			}
			file.save(layout.live, dirty);
			expectLoaded(helper, file, layout.live, "destroying half");

			dirty.clear();
			for(CellContents contents : CellLayout.synthetic(100, 8).live.values())
			{
				UUID fresh = UUID.randomUUID();
				layout.live.put(fresh, contents);
				dirty.add(fresh);
			}
			file.save(layout.live, dirty);
			expectLoaded(helper, file, layout.live, "refilling");
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
			expectLoaded(helper, file, layout.live, "records outgrowing their sectors");
		}
		finally
		{
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	private static void expectLoaded(GameTestHelper helper, CellSaveStrategy strategy, Map<UUID, CellContents> live,
			String step) throws IOException
	{
		Map<UUID, CellContents> loaded = strategy.load();
		StorageGameTests.check(helper, loaded.equals(live), strategy.name()+" loaded "+loaded.size()+" of "
				+live.size()+" cells, or different contents, after "+step);
	}
}
