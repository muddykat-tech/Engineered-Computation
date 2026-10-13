package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

import static com.muddykat.engineeredcomputation.storage.persist.GrowthScenarios.*;

// results go to the log under [cell-scenario].
@TestCategory({"storage", "stress"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CellPersistenceScenarioGameTests
{
	private CellPersistenceScenarioGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void cellsGainingTypes(GameTestHelper helper) throws IOException
	{
		compare(helper, GAINING_TYPES);
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void countsChangingOnly(GameTestHelper helper) throws IOException
	{
		compare(helper, COUNTS_ONLY);
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void manyNewCells(GameTestHelper helper) throws IOException
	{
		compare(helper, NEW_CELLS);
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void mixedWorkload(GameTestHelper helper) throws IOException
	{
		compare(helper, MIXED);
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void interleavedGrowth(GameTestHelper helper) throws IOException
	{
		compare(helper, INTERLEAVED_GROWTH);
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void smallCellsReplacedByLarge(GameTestHelper helper) throws IOException
	{
		compare(helper, SMALL_REPLACED);
	}

	private static List<Kind> kinds(HolderLookup.Provider registries)
	{
		return List.of(
				new Kind("per drive", (work, dir) -> new GroupedFileStrategy("per drive", dir, registries,
						work::driveOf)),
				new Kind("per network", (work, dir) -> new GroupedFileStrategy("per network", dir, registries,
						work::networkOf)),
				new Kind("per chunk", (work, dir) -> new GroupedFileStrategy("per chunk", dir, registries,
						work::chunkOf)),
				new Kind("indexed, original", (work, dir) -> new RegionFileStrategy(dir.resolve("cells.ecc"),
						RegionFileStrategy.Options.NONE, registries)),
				new Kind("indexed, fixed", (work, dir) -> new RegionFileStrategy(dir.resolve("cells.ecc"),
						RegionFileStrategy.Options.BEST, registries)),
				new Kind("bulk file", (work, dir) -> new GroupedFileStrategy("bulk file", dir, registries,
						id -> "cells")));
	}

	private static void compare(GameTestHelper helper, NamedScenario scenario) throws IOException
	{
		log("%s", scenario.title());
		log("  %-18s | %10s | %10s | %9s | %11s | %10s | %12s | %13s | %8s", "strategy", "written", "save time",
				"per save", "worst save", "on disk", "after reopen", "files", "load");
		Path root = CellLayout.tempDir();
		try
		{
			for(Kind kind : kinds(helper.getLevel().registryAccess()))
			{
				Path dir = root.resolve(kind.name.replaceAll("[^a-z]+", "_"));
				Workload work = new Workload();
				work.strategy = kind.make.make(work, dir);
				CellSaveStrategy reopened = null;
				try
				{
					scenario.run().run(work);
					StorageGameTests.check(helper, work.strategy.load().equals(work.live),
							kind.name+" lost or changed cells: "+scenario.title());
					CellSaveStrategy.DiskUsage before = work.strategy.usage();
					work.strategy.close();
					reopened = kind.make.make(work, dir);
					long loadStart = System.nanoTime();
					StorageGameTests.check(helper, reopened.load().equals(work.live),
							kind.name+" lost or changed cells reopening: "+scenario.title());
					long loadNanos = System.nanoTime()-loadStart;
					CellSaveStrategy.DiskUsage after = reopened.usage();
					log("  %-18s | %10s | %8.0f ms | %6.2f ms | %8.2f ms | %10s | %12s | %13s | %5.0f ms", kind.name,
							size(work.written), work.saveNanos/1e6, work.saveNanos/1e6/work.saves,
							work.worstSaveNanos/1e6, size(before.bytes()), size(after.bytes()), after.files()+" files",
							loadNanos/1e6);
				}
				finally
				{
					work.strategy.close();
					if(reopened!=null)
						reopened.close();
				}
			}
		}
		finally
		{
			CellLayout.delete(root);
		}
		helper.succeed();
	}

	private static String size(long bytes)
	{
		return bytes<10_000? bytes+" B": bytes<10_000_000? bytes/1024+" KB": String.format(Locale.ROOT, "%.1f MB",
				bytes/(1024.0*1024));
	}

	private static void log(String format, Object... args)
	{
		EngineeredComputation.LOGGER.info("[cell-scenario] "+String.format(Locale.ROOT, format, args));
	}

	private record Kind(String name, Factory make)
	{
	}

	private interface Factory
	{
		CellSaveStrategy make(Workload work, Path dir) throws IOException;
	}
}
