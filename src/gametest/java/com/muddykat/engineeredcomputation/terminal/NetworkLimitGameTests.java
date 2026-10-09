package com.muddykat.engineeredcomputation.terminal;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterStatus;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalDiskBlockEntity;

@TestCategory("network")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkLimitGameTests
{
	private NetworkLimitGameTests()
	{
	}

	private static BlockPos cables(GameTestHelper helper, int x, int y, int z)
	{
		BlockPos corner = helper.absolutePos(new BlockPos(1, 1, 1));
		for(BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(x-1, y-1, z-1)))
			helper.getLevel().setBlock(pos, ECCables.block().get().defaultBlockState(), 2);
		return corner;
	}

	private static DataAdapterBlockEntity exporter(GameTestHelper helper, BlockPos at)
	{
		ServerLevel level = helper.getLevel();
		level.setBlockAndUpdate(at, ECAdapters.of(ComputerEra.FLOPPY).get(AdapterKind.EXPORTER).get().defaultBlockState()
				.setValue(DataAdapterBlock.FACING, Direction.EAST));
		level.setBlockAndUpdate(at.east(), Blocks.CHEST.defaultBlockState());
		DataAdapterBlockEntity adapter = (DataAdapterBlockEntity)level.getBlockEntity(at);
		adapter.setFilter(new ItemStack(Items.COBBLESTONE));
		return adapter;
	}

	private static void storage(GameTestHelper helper, BlockPos at)
	{
		ServerLevel level = helper.getLevel();
		level.setBlockAndUpdate(at, StorageGameTests.terminal(ComputerEra.FLOPPY)
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		TerminalDiskBlockEntity bays = (TerminalDiskBlockEntity)level.getBlockEntity(at);
		for(int slot = 0; slot<TerminalDiskBlockEntity.SLOTS; slot++)
			bays.getDisks().setStackInSlot(slot, new ItemStack(ECDataCells.terminalDisks().get(2).get()));
	}

	private static double medianMillis(Runnable run)
	{
		for(int i = 0; i<3; i++)
			run.run();
		long[] times = new long[15];
		for(int i = 0; i<times.length; i++)
		{
			long start = System.nanoTime();
			run.run();
			times[i] = System.nanoTime()-start;
		}
		Arrays.sort(times);
		return times[times.length/2]/1e6;
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void networkAtTheLimitIsAllowed(GameTestHelper helper)
	{
		BlockPos corner = cables(helper, 16, 16, 16);
		StorageNetwork network = StorageNetwork.scan(helper.getLevel(), corner);
		StorageGameTests.check(helper, !network.isTooLarge(), "Network at the limit should be allowed");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void networkOverTheLimitIsRefused(GameTestHelper helper)
	{
		BlockPos corner = cables(helper, 17, 16, 16);
		StorageNetwork network = StorageNetwork.scan(helper.getLevel(), corner);
		StorageGameTests.check(helper, network.isTooLarge(), "Network over the limit should be refused");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void adapterOnATooLargeNetworkIsRefused(GameTestHelper helper)
	{
		BlockPos corner = cables(helper, 17, 16, 16);
		DataAdapterBlockEntity adapter = exporter(helper, corner.offset(17, 0, 0));
		StorageGameTests.check(helper, adapter.transfer()==AdapterStatus.CONTENT_TOO_LARGE, "Adapter on too large network should be refused");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void networksAreCachedUntilTheyChange(GameTestHelper helper)
	{
		BlockPos corner = cables(helper, 4, 1, 1);
		StorageNetwork first = StorageNetwork.of(helper.getLevel(), corner);
		StorageGameTests.check(helper, StorageNetwork.of(helper.getLevel(), corner.east(3))==first, "Networks should be cached");

		helper.getLevel().setBlockAndUpdate(corner.east(4), ECCables.block().get().defaultBlockState());
		StorageGameTests.check(helper, StorageNetwork.of(helper.getLevel(), corner)!=first, "Network should change when block is added");
		helper.succeed();
	}

	@GameTest(template = NetworkStressGameTests.BIG, timeoutTicks = 400)
	public static void manyAdaptersStayCheap(GameTestHelper helper)
	{
		BlockPos corner = cables(helper, 10, 10, 10);
		storage(helper, corner.offset(0, 10, 0));

		StorageNetwork.of(helper.getLevel(), corner).getStorage()
				.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 40000, false);
		DataAdapterBlockEntity[] adapters = new DataAdapterBlockEntity[32];
		for(int i = 0; i<32; i++)
			adapters[i] = exporter(helper, corner.offset(10, i/4, (i%4)*2));
		StorageGameTests.check(helper, adapters[0].transfer()==AdapterStatus.OK, "the first exporter didn't move anything");

		double millis = medianMillis(() -> {
			for(DataAdapterBlockEntity adapter : adapters)
				adapter.transfer();
		});
		EngineeredComputation.LOGGER.info("[stress] 32 floppy adapters on a 1000-block network, one move each: {} ms", millis);
		StorageGameTests.check(helper, millis<5, "32 adapters took "+millis+" ms");
		helper.succeed();
	}
}
