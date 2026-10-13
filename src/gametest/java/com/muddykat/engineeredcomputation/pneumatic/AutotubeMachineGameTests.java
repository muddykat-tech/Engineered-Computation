package com.muddykat.engineeredcomputation.pneumatic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.NetworkStressGameTests;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlock;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("pneumatic")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class AutotubeMachineGameTests
{
	private AutotubeMachineGameTests()
	{
	}

	@GameTest(template = NetworkStressGameTests.BIG)
	public static void aWorkstationIsntAnInventoryToAnAutotube(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(3, 1, 3);
		StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_WORKSTATION, Direction.NORTH, at);
		BlockPos part = StorageGameTests.world(helper.absolutePos(at), EraDrive.FLOPPY_WORKSTATION.geometry().master(),
				Direction.NORTH);
		check(helper, part!=null&&level.getCapability(Capabilities.ItemHandler.BLOCK, part, Direction.UP)!=null,
				"the workstation's part should have slots, for this test to mean anything");
		check(helper, !AutotubeBlock.isInventory(level, part, Direction.DOWN),
				"an Autotube above the workstation would send into it");
		helper.succeed();
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}
}
