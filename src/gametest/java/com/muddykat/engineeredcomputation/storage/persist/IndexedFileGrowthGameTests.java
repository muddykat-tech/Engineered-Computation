package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

/** Stress Tests for the indexed file method, ensures that it grows past its live data under each workload;
 * results go to [indexed-growth].
 **/
@TestCategory({"storage", "stress"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class IndexedFileGrowthGameTests
{
	private IndexedFileGrowthGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void cellsGainingTypes(GameTestHelper helper) throws IOException
	{
		compare(helper, GAINING_TYPES, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void countsChangingOnly(GameTestHelper helper) throws IOException
	{
		compare(helper, COUNTS_ONLY, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void manyNewCells(GameTestHelper helper) throws IOException
	{
		compare(helper, NEW_CELLS, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void mixedWorkload(GameTestHelper helper) throws IOException
	{
		compare(helper, MIXED, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void interleavedGrowth(GameTestHelper helper) throws IOException
	{
		compare(helper, INTERLEAVED_GROWTH, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void smallCellsReplacedByLarge(GameTestHelper helper) throws IOException
	{
		compare(helper, SMALL_REPLACED, fixes());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 1200)
	public static void sectorSizes(GameTestHelper helper) throws IOException
	{
		for(NamedScenario scenario : ALL)
			compare(helper, new NamedScenario("SECTORS: "+scenario.title(), scenario.run()), sectorSizes());
		helper.succeed();
	}

	private static List<Variant> fixes()
	{
		return List.of(new Variant("baseline", RegionFileStrategy.Options.NONE),
				new Variant("1 grow in place", new RegionFileStrategy.Options(true, 0, false, false, 0, 512)),
				new Variant("1+2 room to grow (25%)", new RegionFileStrategy.Options(true, 25, false, false, 0, 512)),
				new Variant("1+3 keep space on shrink", new RegionFileStrategy.Options(true, 0, false, true, 0, 512)),
				new Variant("1+2+3", new RegionFileStrategy.Options(true, 25, false, true, 0, 512)),
				new Variant("1+3+4 compact on open", new RegionFileStrategy.Options(true, 0, false, true, 25, 512)),
				new Variant("1+3+4 compact above 10%", new RegionFileStrategy.Options(true, 0, false, true, 10, 512)),
				new Variant("1+2+3+4", new RegionFileStrategy.Options(true, 25, false, true, 25, 512)),
				new Variant("1+2b+3 room once grown", new RegionFileStrategy.Options(true, 25, true, true, 0, 512)),
				new Variant("1+2b+3+4", new RegionFileStrategy.Options(true, 25, true, true, 25, 512)));
	}

	private static List<Variant> sectorSizes()
	{
		List<Variant> variants = new ArrayList<>();
		variants.add(new Variant("original, 512 B", RegionFileStrategy.Options.NONE));
		for(int size : new int[]{64, 128, 256, 512, 1024, 4096})
			variants.add(new Variant("fixed, "+size+" B", new RegionFileStrategy.Options(true, 0, false, true, 25, size)));
		variants.add(new Variant("fixed, 128 B, compact >10%", new RegionFileStrategy.Options(true, 0, false, true, 10,
				128)));
		return variants;
	}

	private static void compare(GameTestHelper helper, NamedScenario scenario, List<Variant> variants)
			throws IOException
	{
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		log("%s", scenario.title());
		log("  %-28s | %9s | %9s | %9s | %9s | %9s | %8s | %7s | %10s | %8s | %s", "variant", "file", "live data",
				"padding", "gaps", "header", "overhead", "moves", "written", "time", "after reopen");
		Path root = CellLayout.tempDir();
		try
		{
			for(Variant variant : variants)
			{
				Path file = root.resolve(variant.name.replaceAll("[^a-z0-9]+", "_")+".ecc");
				Workload work = new Workload();
				RegionFileStrategy indexed = new RegionFileStrategy(file, variant.options, registries);
				work.strategy = indexed;
				RegionFileStrategy reopened = null;
				try
				{
					long start = System.nanoTime();
					scenario.run().run(work);
					long nanos = System.nanoTime()-start;
					RegionFileStrategy.Growth growth = indexed.growth();
					StorageGameTests.check(helper, indexed.load().equals(work.live),
							variant.name+" lost or changed cells: "+scenario.title());
					indexed.close();
					long reopenStart = System.nanoTime();
					reopened = new RegionFileStrategy(file, variant.options, registries);
					long reopenNanos = System.nanoTime()-reopenStart;
					StorageGameTests.check(helper, reopened.load().equals(work.live),
							variant.name+" lost or changed cells reopening: "+scenario.title());
					log("  %-28s | %9s | %9s | %9s | %9s | %9s | %7.1f%% | %7d | %10s | %6.0f ms | %s in %.0f ms",
							variant.name,
							size(growth.fileBytes()), size(growth.liveBytes()), size(growth.paddingBytes()),
							size(growth.gapBytes()), size(growth.headerBytes()),
							100.0*(growth.fileBytes()-growth.headerBytes()-growth.liveBytes())/growth.liveBytes(),
							growth.moves(), size(work.written), nanos/1e6, size(reopened.usage().bytes()), reopenNanos/1e6);
				}
				finally
				{
					indexed.close();
					if(reopened!=null)
						reopened.close();
				}
			}
		}
		finally
		{
			CellLayout.delete(root);
		}
	}

	private static String size(long bytes)
	{
		return bytes<10_000? bytes+" B": bytes<10_000_000? bytes/1024+" KB": String.format(Locale.ROOT, "%.1f MB",
				bytes/(1024.0*1024));
	}

	private static void log(String format, Object... args)
	{
		EngineeredComputation.LOGGER.info("[indexed-growth] "+String.format(Locale.ROOT, format, args));
	}

	private record Variant(String name, RegionFileStrategy.Options options)
	{
	}

}
