package com.muddykat.engineeredcomputation.network;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.terminal.TerminalAction;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;
import com.muddykat.engineeredcomputation.terminal.UniscopeMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;

@TestCategory("network")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CableGameTests
{
	private CableGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cablesCarryTheNetwork(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		place(helper, 3, 1, 2, cable());
		place(helper, 3, 1, 1, cable());
		BlockPos cable3 = place(helper, 2, 1, 1, cable());
		place(helper, 1, 1, 1, cable());
		BlockPos terminal = place(helper, 0, 1, 1, StorageGameTests.terminal(ComputerEra.DRUM));

		Set<ComputerEra> eras = StorageNetwork.scan(level, terminal).getEras();
		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal).getHosts().size()==1,
				"network should have exactly one host");
		StorageGameTests.check(helper, eras.equals(Set.of(ComputerEra.DRUM)),
				"network should only contain DRUM era, got "+eras);

		level.setBlockAndUpdate(cable3, Blocks.AIR.defaultBlockState());
		StorageGameTests.check(helper, StorageNetwork.scan(level, terminal).getHosts().isEmpty(),
				"network should have no hosts after removing a cable");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cableConnectionsFollowNeighbours(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos cable1 = place(helper, 2, 1, 2, cable());
		BlockPos terminal = place(helper, 3, 1, 2, StorageGameTests.terminal(ComputerEra.DRUM));

		BlockState state = level.getBlockState(cable1);
		StorageGameTests.check(helper, state.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.EAST)),
				"east connection should be true");
		for(Direction direction : Direction.values())
			if(direction!=Direction.EAST)
				StorageGameTests.check(helper, !state.getValue(NetworkCableBlock.CONNECTIONS.get(direction)),
						direction+" connection should be false");

		BlockPos cable2 = place(helper, 2, 2, 2, cable());
		state = level.getBlockState(cable1);
		StorageGameTests.check(helper, state.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP)),
				"lower cable's up connection should be true");
		state = level.getBlockState(cable2);
		StorageGameTests.check(helper, state.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.DOWN)),
				"upper cable's down connection should be true");

		level.setBlockAndUpdate(terminal, Blocks.AIR.defaultBlockState());
		state = level.getBlockState(cable1);
		StorageGameTests.check(helper, !state.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.EAST)),
				"east connection should be false after removing terminal");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void mixedErasLockTheTerminal(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get()), false);

		place(helper, 3, 1, 2, cable());
		place(helper, 3, 1, 1, cable());
		place(helper, 2, 1, 1, cable());
		place(helper, 1, 1, 1, cable());
		BlockPos drumTerminal = place(helper, 0, 1, 1, StorageGameTests.terminal(ComputerEra.DRUM));
		BlockPos floppyTerminal = place(helper, 0, 1, 2, StorageGameTests.terminal(ComputerEra.FLOPPY));
		((TerminalDiskBlockEntity)level.getBlockEntity(floppyTerminal)).getDisks().insertItem(0,
				new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);

		Set<ComputerEra> conflicting = StorageNetwork.scan(level, drumTerminal).getConflictingEras();
		StorageGameTests.check(helper, conflicting.equals(Set.of(ComputerEra.DRUM, ComputerEra.FLOPPY)),
				"drum and floppy storage should clash, got "+conflicting);

		Player player = FakePlayerFactory.getMinecraft(level);
		TerminalMenu menu = new UniscopeMenu(1, player.getInventory(), drumTerminal);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, !menu.getCarried().isEmpty()&&menu.getCarried().getCount()==64,
				"terminal should not accept items when locked");

		level.setBlockAndUpdate(floppyTerminal, Blocks.AIR.defaultBlockState());
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().isEmpty(),
				"terminal should accept items after removing the floppy terminal");
		helper.succeed();
	}

	private static BlockPos place(GameTestHelper helper, int x, int y, int z, BlockState state)
	{
		BlockPos absolute = helper.absolutePos(new BlockPos(x, y, z));
		helper.getLevel().setBlockAndUpdate(absolute, state);
		return absolute;
	}

	private static BlockState cable()
	{
		return ECCables.block().get().defaultBlockState();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void cablesReachForAdaptersBacks(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockState exporter = ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.EXPORTER).get().defaultBlockState();
		BlockPos adapter = helper.absolutePos(new BlockPos(2, 2, 2));
		level.setBlockAndUpdate(adapter, exporter.setValue(DataAdapterBlock.FACING, Direction.UP));
		BlockPos cable = adapter.below();
		level.setBlockAndUpdate(cable, Block.updateFromNeighbourShapes(ECCables.block().get().defaultBlockState(), level,
				cable));
		StorageGameTests.check(helper, level.getBlockState(cable).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP)),
				"a cable placed after an adapter didn't reach for its back");
		BlockPos second = helper.absolutePos(new BlockPos(5, 1, 2));
		level.setBlockAndUpdate(second, ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(second.above(), exporter.setValue(DataAdapterBlock.FACING, Direction.UP));
		StorageGameTests.check(helper, level.getBlockState(second).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP)),
				"a cable didn't reach for an adapter placed with its back on it");
		BlockPos third = helper.absolutePos(new BlockPos(5, 1, 5));
		level.setBlockAndUpdate(third, ECCables.block().get().defaultBlockState());
		level.setBlockAndUpdate(third.above(), exporter.setValue(DataAdapterBlock.FACING, Direction.DOWN));
		StorageGameTests.check(helper, !level.getBlockState(third).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.UP)),
				"a cable reached for an adapter's front");
		helper.succeed();
	}
}
