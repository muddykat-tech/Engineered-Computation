package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

// Times each save strategy over the same changes; results go to the log under [cell-persist]. */
@TestCategory({"storage", "stress"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellPersistenceStressGameTests
{
	private static final int RUNS = 5;
	private static final int DRIVES_PER_NETWORK = 16;

	private CellPersistenceStressGameTests()
	{
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void floppyWorkstationWorld(GameTestHelper helper) throws IOException
	{
		CellLayout layout = CellLayout.floppyWorld(helper, 4, 5);
		try
		{
			compare(helper, layout, "floppy world: 4 networks x 5 Floppy Workstations x 2 bays");
		}
		finally
		{
			layout.emptyWorldCells();
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void strategiesAtScale(GameTestHelper helper) throws IOException
	{
		for(int cells : new int[]{256, 1024, 4096})
			compare(helper, CellLayout.synthetic(cells, DRIVES_PER_NETWORK), cells+" cells, "+DRIVES_PER_NETWORK
					+" drives per network, 4 drives per chunk");
		helper.succeed();
	}

	private static void compare(GameTestHelper helper, CellLayout layout, String title) throws IOException
	{
		List<UUID> ids = List.copyOf(layout.live.keySet());
		Set<UUID> oneCell = Set.of(ids.get(ids.size()/2));
		Set<UUID> busyNetwork = Set.copyOf(layout.cellsIn(layout.network, "network_0"));
		int networks = (int)distinct(layout.network);
		int busyCount = Math.min(8, networks);
		Set<UUID> busyNetworks = new HashSet<>();
		for(int i = 0; i<busyCount; i++)
			busyNetworks.addAll(layout.cellsIn(layout.network, "network_"+i*networks/busyCount));
		List<UUID> scattered = new ArrayList<>();
		for(int i = 0; i<ids.size(); i += 20)
			scattered.add(ids.get(i));

		log("%s (%d cells, %d drives, %d networks, %d chunks)", title, ids.size(), distinct(layout.drive),
				distinct(layout.network), distinct(layout.chunk));
		log("  %-20s | %-24s | %-24s | %-24s | %-24s | %-24s | %-15s | %-9s | %s", "strategy", "first save (all)",
				"1 cell changed", "1 network busy ("+busyNetwork.size()+")",
				busyCount+" networks busy ("+busyNetworks.size()+")", "5% scattered ("+scattered.size()+")",
				"move 1 cell", "load all", "on disk");
		Path root = CellLayout.tempDir();
		List<CellSaveStrategy> strategies = layout.strategies(root, helper.getLevel().registryAccess());
		try
		{
			for(CellSaveStrategy strategy : strategies)
			{
				Timed full = timeOnce(() -> strategy.save(layout.live, Set.copyOf(ids)));
				Timed one = timed(layout, strategy, oneCell);
				Timed busy = timed(layout, strategy, busyNetwork);
				Timed busyMany = timed(layout, strategy, busyNetworks);
				Timed spread = timed(layout, strategy, Set.copyOf(scattered));

				UUID moving = ids.getFirst();
				UUID home = ids.get(1);
				layout.moveTo(moving, ids.getLast());
				Timed move = timeOnce(() -> strategy.save(layout.live, Set.of(moving)));
				layout.moveTo(moving, home);

				long load = median(3, () -> {
					try
					{
						strategy.load();
					}
					catch(IOException e)
					{
						throw new UncheckedIOException(e);
					}
				});
				CellSaveStrategy.DiskUsage disk = strategy.usage();
				log("  %-20s | %s | %s | %s | %s | %s | %15s | %6.1f ms | %d files, %s", strategy.name(), full, one,
						busy, busyMany, spread, move.bytes(), load/1e6, disk.files(), size(disk.bytes()));
			}
		}
		finally
		{
			for(CellSaveStrategy strategy : strategies)
				strategy.close();
			CellLayout.delete(root);
		}
	}

	private static Timed timed(CellLayout layout, CellSaveStrategy strategy, Set<UUID> dirty) throws IOException
	{
		long[] times = new long[RUNS];
		CellSaveStrategy.SaveResult result = null;
		for(int i = 0; i<RUNS; i++)
		{
			dirty.forEach(layout::touch);
			long start = System.nanoTime();
			result = strategy.save(layout.live, dirty);
			times[i] = System.nanoTime()-start;
		}
		Arrays.sort(times);
		return new Timed(times[RUNS/2], result);
	}

	private static Timed timeOnce(IoAction action) throws IOException
	{
		long start = System.nanoTime();
		CellSaveStrategy.SaveResult result = action.run();
		return new Timed(System.nanoTime()-start, result);
	}

	private static long median(int runs, Runnable run)
	{
		long[] times = new long[runs];
		for(int i = 0; i<runs; i++)
		{
			long start = System.nanoTime();
			run.run();
			times[i] = System.nanoTime()-start;
		}
		Arrays.sort(times);
		return times[runs/2];
	}

	private static long distinct(Map<UUID, String> grouping)
	{
		return grouping.values().stream().distinct().count();
	}

	private static String size(long bytes)
	{
		return bytes<10_000? bytes+" B": bytes<10_000_000? bytes/1024+" KB": bytes/(1024*1024)+" MB";
	}

	private static void log(String format, Object... args)
	{
		EngineeredComputation.LOGGER.info("[cell-persist] "+String.format(Locale.ROOT, format, args));
	}

	private interface IoAction
	{
		CellSaveStrategy.SaveResult run() throws IOException;
	}

	private record Timed(long nanos, CellSaveStrategy.SaveResult result)
	{
		String bytes()
		{
			return result.filesWritten()+" files, "+size(result.bytesWritten());
		}

		@Override
		public String toString()
		{
			return String.format(Locale.ROOT, "%8.2f ms %15s", nanos/1e6, bytes());
		}
	}
}
