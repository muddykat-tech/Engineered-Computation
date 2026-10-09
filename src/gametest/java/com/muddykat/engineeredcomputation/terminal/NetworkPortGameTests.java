package com.muddykat.engineeredcomputation.terminal;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

@TestCategory({"network", "multiblock"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkPortGameTests
{
	private NetworkPortGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cableConnectsAtThePort(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, Direction.EAST);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);

		BlockPos port = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos cable1 = place(helper, port.east(), cable());
		place(helper, port.east(2), cable());
		BlockPos terminal = place(helper, port.east(3), StorageGameTests.terminal(ComputerEra.FLOPPY));

		StorageGameTests.check(helper, connected(helper, cable1, Direction.WEST),
				"cable at port.east() should connect west");
		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal).getHosts().size()==1,
				"network should have exactly one host");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cablesRefusedAwayFromThePort(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, Direction.EAST);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);

		BlockPos port = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos cable1 = place(helper, port.north(), cable());
		BlockPos cable2 = place(helper, port.above(4), cable());
		BlockPos terminal = place(helper, port.north(2), StorageGameTests.terminal(ComputerEra.FLOPPY));

		StorageGameTests.check(helper, !connected(helper, cable1, Direction.SOUTH),
				"cable at port.north() should not connect south");
		StorageGameTests.check(helper, !connected(helper, cable2, Direction.DOWN),
				"cable at port.above(4) should not connect down");
		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal).getHosts().isEmpty(),
				"a terminal on a cable that misses the port shouldn't reach the drive");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void terminalsTouchOnlyAtThePort(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, Direction.EAST);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);

		BlockPos port = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos terminal1 = place(helper, port.north(), StorageGameTests.terminal(ComputerEra.FLOPPY));
		BlockPos terminal2 = place(helper, port.east(), StorageGameTests.terminal(ComputerEra.FLOPPY));

		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal1).getHosts().isEmpty(),
				"network should have no hosts when terminal touches the drive, not the port");
		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal2).getHosts().size()==1,
				"network should have exactly one host when terminal touches the port");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void drivePartsStayLinked(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, Direction.EAST);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);

		BlockPos port = helper.absolutePos(StorageGameTests.ORIGIN);
		StorageGameTests.check(helper, StorageNetwork.scan(level, port.south()).getHosts().size()==1,
				"network should have exactly one host when scanning from a drawer part");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void diskPackPortIsBottomMiddleOfTheBack(GameTestHelper helper)
	{
		StorageGameTests.formDrive(helper, ComputerEra.DISK, Direction.NORTH);
		BlockPos backMiddle = helper.absolutePos(StorageGameTests.ORIGIN.offset(1, 0, 0));
		BlockPos atPort = place(helper, backMiddle.north(), cable());
		BlockPos besidePort = place(helper, backMiddle.west().north(), cable());
		StorageGameTests.check(helper, connected(helper, atPort, Direction.SOUTH),
				"a cable behind the bottom middle of the back should connect to the port");
		StorageGameTests.check(helper, !connected(helper, besidePort, Direction.SOUTH),
				"a cable behind the bottom left of the back shouldn't connect");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void tapePortIsBottomLeftOfTheBack(GameTestHelper helper)
	{
		StorageGameTests.formDrive(helper, ComputerEra.TAPE, Direction.NORTH);
		BlockPos backLeft = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos atPort = place(helper, backLeft.north(), cable());
		BlockPos besidePort = place(helper, backLeft.east().north(), cable());
		StorageGameTests.check(helper, connected(helper, atPort, Direction.SOUTH),
				"a cable behind the bottom left of the back should connect to the port");
		StorageGameTests.check(helper, !connected(helper, besidePort, Direction.SOUTH),
				"a cable behind the bottom right of the back shouldn't connect");
		helper.succeed();
	}

	private static BlockPos place(GameTestHelper helper, BlockPos absolute, BlockState state)
	{
		helper.getLevel().setBlockAndUpdate(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute));
		return absolute;
	}

	private static BlockState cable()
	{
		return ECCables.block().get().defaultBlockState();
	}

	private static boolean connected(GameTestHelper helper, BlockPos cable, Direction side)
	{
		return helper.getLevel().getBlockState(cable).getValue(NetworkCableBlock.CONNECTIONS.get(side));
	}
}
