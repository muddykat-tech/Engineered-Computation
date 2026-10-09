package com.muddykat.engineeredcomputation.terminal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.EraDataCell;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

@TestCategory({"network", "stress"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkStressGameTests
{
	static final String BIG = "gametest/empty_24x20x24";
	private static final int RUNS = 15;
	private static final double SCAN_BUDGET_MS = 15;
	private static final double STORAGE_BUDGET_MS = 2;

	private NetworkStressGameTests()
	{
	}

	@GameTest(template = BIG, timeoutTicks = 400)
	public static void scanScalesWithBlocks(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos corner = helper.absolutePos(new BlockPos(1, 1, 1));
		for(int side : new int[]{8, 12, 16})
		{
			for(BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(side-1, side-1, side-1)))
				if(!level.getBlockState(pos).is(ECCables.block().get()))
					level.setBlock(pos, ECCables.block().get().defaultBlockState(), 2);
			long nanos = median(() -> StorageNetwork.scan(level, corner));
			log("scan %d cable blocks: %.3f ms", side*side*side, nanos/1e6);
			StorageGameTests.check(helper, nanos/1e6<SCAN_BUDGET_MS,
					"scanning "+side*side*side+" blocks took "+nanos/1e6+" ms");
		}
		helper.succeed();
	}

	@GameTest(template = BIG, timeoutTicks = 400)
	public static void storageScalesWithCells(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos corner = helper.absolutePos(new BlockPos(1, 1, 1));
		Item[] items = BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell)).toArray(Item[]::new);
		int placed = 0;
		for(int side : new int[]{4, 6, 8, 10, 12})
		{
			for(BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(side-1, side-1, side-1)))
				if(!(level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity))
				{
					level.setBlock(pos, StorageGameTests.terminal(ComputerEra.FLOPPY)
							.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 2);
					TerminalDiskBlockEntity bays = (TerminalDiskBlockEntity)level.getBlockEntity(pos);
					for(int slot = 0; slot<TerminalDiskBlockEntity.SLOTS; slot++)
						bays.getDisks().setStackInSlot(slot, new ItemStack(ECDataCells.terminalDisks().get(2).get()));
					placed++;
				}
			NetworkStorage storage = StorageNetwork.scan(level, corner).getStorage();
			int cells = storage.getCells().size();
			int types = 0;
			for(int i = 0; i<items.length&&types<cells*6; i++)
				if(storage.insert(ItemKey.of(new ItemStack(items[i])), 64, false)>0)
					types++;
			ItemKey cobble = ItemKey.of(new ItemStack(Items.COBBLESTONE));
			long scan = median(() -> StorageNetwork.scan(level, corner));
			long collect = median(() -> storage.collect(new HashMap<>()));
			long stats = median(() -> TerminalStats.of(storage, Set.of(), false));
			long insert = median(() -> {
				storage.insert(cobble, 64, false);
				storage.extract(cobble, 64, false);
			});
			long simulate = median(() -> storage.insert(cobble, 64, true));
			log("%d blocks, %d cells, %d types: scan %.3f ms, collect %.3f ms, stats %.3f ms, insert+extract %.3f ms, "
					+"simulated insert %.3f ms", placed, cells, types, scan/1e6, collect/1e6, stats/1e6, insert/1e6,
					simulate/1e6);
			for(long nanos : new long[]{collect, stats, insert, simulate})
				StorageGameTests.check(helper, nanos/1e6<STORAGE_BUDGET_MS,
						"a storage operation over "+cells+" cells took "+nanos/1e6+" ms");
		}
		helper.succeed();
	}

	private static long median(Runnable run)
	{
		for(int i = 0; i<3; i++)
			run.run();
		long[] times = new long[RUNS];
		for(int i = 0; i<RUNS; i++)
		{
			long start = System.nanoTime();
			run.run();
			times[i] = System.nanoTime()-start;
		}
		Arrays.sort(times);
		return times[RUNS/2];
	}

	private static void log(String format, Object... args)
	{
		EngineeredComputation.LOGGER.info("[stress] "+String.format(Locale.ROOT, format, args));
	}
}
