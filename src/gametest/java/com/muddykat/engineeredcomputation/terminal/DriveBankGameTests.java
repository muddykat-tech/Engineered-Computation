package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockDefinition;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

@TestCategory({"storage", "multiblock"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class DriveBankGameTests
{
	private DriveBankGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void workstationBuildFormsTheWorkstation(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDrive workstation = EraDrive.FLOPPY_WORKSTATION;
		EraDriveGeometry geometry = workstation.geometry();
		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		for(BlockPos pos : geometry.shapes().keySet())
			level.setBlockAndUpdate(
					StorageGameTests.world(origin, pos, Direction.NORTH),
					ECContent.casing(ComputerEra.FLOPPY).get().defaultBlockState());
		BlockPos trigger = StorageGameTests.world(origin, geometry.trigger(), Direction.NORTH);
		boolean formed = false;
		for(MultiblockDefinition<EraDriveState> definition : ECMultiblocks.all())
			if(definition.tryForm(level, trigger, Direction.SOUTH))
			{
				formed = true;
				break;
			}
		StorageGameTests.check(helper, formed, "workstation didn't form");
		MultiblockPartBlockEntity master = (MultiblockPartBlockEntity)level.getBlockEntity(
				StorageGameTests.world(origin, geometry.master(), Direction.NORTH));
		StorageGameTests.check(helper, master.getState() instanceof EraDriveState state
						&&state.getDrive()==workstation,
				"formed workstation has wrong drive type");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyBankHasFourSyncedSlots(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, EraDrive.FLOPPY_BANK, Direction.NORTH);
		StorageGameTests.check(helper, drive.getCellSlots().getSlots()==4, "floppy bank should have 4 slots");
		for(int i = 0; i<4; i++)
			drive.getCellSlots().insertItem(i, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(i).get()),
					false);
		CompoundTag tag = new CompoundTag();
		drive.writeSyncData(tag);
		StorageGameTests.check(helper, tag.getInt(EraDriveState.FILLED_SLOTS)==0b1111,
				"all 4 slots should be flagged as filled");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void diskBankHoldsOnePack(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, EraDrive.DISK_BANK, Direction.NORTH);
		StorageGameTests.check(helper, drive.getCellSlots().getSlots()==1, "disk bank should have 1 slot");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyBankConnectsAtItsPort(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, EraDrive.FLOPPY_BANK, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);
		BlockPos portPos = StorageGameTests.world(helper.absolutePos(StorageGameTests.ORIGIN),
				drive.getDrive().geometry().ports().get(0).pos(), Direction.NORTH)
				.relative(drive.getDrive().geometry().ports().get(0).side());
		helper.getLevel().setBlockAndUpdate(portPos, StorageGameTests.terminal(ComputerEra.FLOPPY));
		StorageGameTests.check(helper, StorageNetwork.scan(helper.getLevel(), portPos).getHosts().size()==1,
				"floppy bank's port should connect to the network");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void diskBankConnectsAtItsPort(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, EraDrive.DISK_BANK, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()), false);
		BlockPos portPos = StorageGameTests.world(helper.absolutePos(StorageGameTests.ORIGIN),
				drive.getDrive().geometry().ports().get(0).pos(), Direction.NORTH)
				.relative(drive.getDrive().geometry().ports().get(0).side());
		helper.getLevel().setBlockAndUpdate(portPos, StorageGameTests.terminal(ComputerEra.DISK));
		StorageGameTests.check(helper, StorageNetwork.scan(helper.getLevel(), portPos).getHosts().size()==1,
				"disk bank's port should connect to the network");
		helper.succeed();
	}
}
