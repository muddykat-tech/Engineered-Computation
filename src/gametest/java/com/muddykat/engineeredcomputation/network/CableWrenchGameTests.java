package com.muddykat.engineeredcomputation.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;
import com.muddykat.engineeredcomputation.connector.ConnectorPorts;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("network")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CableWrenchGameTests
{
	private CableWrenchGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theWrenchCutsAndReopensACable(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		BlockPos first = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		BlockPos second = first.relative(Direction.NORTH);
		level.setBlockAndUpdate(first, ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(second, ECCables.block().get().defaultBlockState());
		check(helper, StorageNetwork.scan(level, second).getHosts().size()==1, "the far cable should reach the drive");
		check(helper, ConnectorPorts.wrench(level, first, new Vec3(0.5, 0.5, 2/16.0)), "wrenching the arm did nothing");
		check(helper, ConnectorBlockEntity.isCut(level, first, Direction.NORTH)
				&&!level.getBlockState(second).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.SOUTH)),
				"the cables still join across the cut");
		check(helper, StorageNetwork.scan(level, second).getHosts().isEmpty(), "the far cable reaches the drive past a cut");
		check(helper, ConnectorPorts.wrench(level, first, new Vec3(0.5, 0.5, 5.5/16)), "wrenching the cap did nothing");
		check(helper, StorageNetwork.scan(level, second).getHosts().size()==1, "reopening didn't join the network again");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aCableCutGoesWithItsNeighbour(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos first = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos second = first.relative(Direction.NORTH);
		level.setBlockAndUpdate(first, ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(second, ECCables.block().get().defaultBlockState());
		check(helper, ConnectorPorts.wrench(level, first, new Vec3(0.5, 0.5, 2/16.0)), "wrenching the arm did nothing");
		level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
		check(helper, !ConnectorBlockEntity.isCut(level, first, Direction.NORTH),
				"the cut stayed with nothing left to cut off");
		level.setBlockAndUpdate(second, ECCables.block().get().defaultBlockState());
		check(helper, level.getBlockState(first).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.NORTH)),
				"a cable put back where the cut one was should join");
		helper.succeed();
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}
}
