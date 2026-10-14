package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
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
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveStrategy;

@TestCategory("persistence")
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

	private static void expectLoaded(GameTestHelper helper, CellSaveStrategy strategy, Map<UUID, CellContents> live,
			String step) throws IOException
	{
		Map<UUID, CellContents> loaded = strategy.load();
		StorageGameTests.check(helper, loaded.equals(live), strategy.name()+" loaded "+loaded.size()+" of "
				+live.size()+" cells, or different contents, after "+step);
	}
}
