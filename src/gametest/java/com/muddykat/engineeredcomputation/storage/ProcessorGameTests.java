package com.muddykat.engineeredcomputation.storage;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterStatus;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;

@TestCategory({"network", "storage"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class ProcessorGameTests
{
	private ProcessorGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void onlyDrumDiskAndFloppyNeedAProcessor(GameTestHelper helper)
	{
		StorageGameTests.check(helper, !ComputerEra.PUNCHCARD.requiresProcessor(),
				"PUNCHCARD should not require processor");
		StorageGameTests.check(helper, !ComputerEra.TAPE.requiresProcessor(), "TAPE should not require processor");
		StorageGameTests.check(helper, ComputerEra.DRUM.requiresProcessor(), "DRUM should require processor");
		StorageGameTests.check(helper, ComputerEra.DISK.requiresProcessor(), "DISK should require processor");
		StorageGameTests.check(helper, ComputerEra.FLOPPY.requiresProcessor(), "FLOPPY should require processor");

		StorageGameTests.check(helper, EraDrive.DRUM_COMPUTER.isProcessor(), "DRUM_COMPUTER should be a processor");
		StorageGameTests.check(helper, EraDrive.DISK_MAINFRAME.isProcessor(), "DISK_MAINFRAME should be a processor");
		StorageGameTests.check(helper, EraDrive.FLOPPY_WORKSTATION.isProcessor(),
				"FLOPPY_WORKSTATION should be a processor");
		StorageGameTests.check(helper, !EraDrive.PUNCHCARD_VAULT.isProcessor(),
				"PUNCHCARD_VAULT should not be a processor");
		StorageGameTests.check(helper, !EraDrive.DRUM_BANK.isProcessor(), "DRUM_BANK should not be a processor");
		StorageGameTests.check(helper, !EraDrive.TAPE_DRIVE.isProcessor(), "TAPE_DRIVE should not be a processor");
		StorageGameTests.check(helper, !EraDrive.DISK_BANK.isProcessor(), "DISK_BANK should not be a processor");
		StorageGameTests.check(helper, !EraDrive.FLOPPY_BANK.isProcessor(), "FLOPPY_BANK should not be a processor");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void drumBankNeedsADrumComputer(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState bank = StorageGameTests.formDriveAt(helper, EraDrive.DRUM_BANK, Direction.NORTH,
				new BlockPos(3, 1, 3));
		bank.getCellSlots().insertItem(0, cell(ComputerEra.DRUM, 0), false);
		bank.getCellSlots().insertItem(1, cell(ComputerEra.DRUM, 1), false);
		place(helper, 4, 1, 2, cable());
		BlockPos terminal = place(helper, 4, 1, 1, StorageGameTests.terminal(ComputerEra.DRUM));
		StorageNetwork network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getHosts().size()==1, "Should have 1 host");
		StorageGameTests.check(helper, network.getStorage().getCells().isEmpty(), "Should have no cells");
		StorageGameTests.check(helper, network.getErasWithoutProcessor().equals(Set.of(ComputerEra.DRUM)),
				"Should have DRUM without processor");

		place(helper, 3, 1, 2, cable());
		place(helper, 2, 1, 2, cable());
		place(helper, 1, 1, 2, cable());
		place(helper, 0, 1, 2, cable());
		place(helper, 0, 1, 3, cable());
		place(helper, 0, 1, 4, cable());
		EraDriveState computer = StorageGameTests.formDriveAt(helper, EraDrive.DRUM_COMPUTER, Direction.NORTH,
				new BlockPos(0, 1, 5));
		computer.getCellSlots().insertItem(0, cell(ComputerEra.DRUM, 2), false);
		network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getStorage().getCells().size()==3, "Should have 3 cells");
		StorageGameTests.check(helper, network.getErasWithoutProcessor().isEmpty(),
				"Should have no eras without processor");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void diskMainframeDoesNotWorkFloppyBanks(GameTestHelper helper)
	{
		EraDriveState mainframe = StorageGameTests.formDriveAt(helper, EraDrive.DISK_MAINFRAME, Direction.NORTH,
				new BlockPos(0, 1, 5));
		mainframe.getCellSlots().insertItem(0, cell(ComputerEra.DISK, 0), false);
		EraDriveState floppyBank = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_BANK, Direction.NORTH,
				new BlockPos(3, 1, 3));
		floppyBank.getCellSlots().insertItem(0, cell(ComputerEra.FLOPPY, 0), false);
		BlockPos start = place(helper, 3, 1, 2, cable());
		place(helper, 2, 1, 2, bridge());
		place(helper, 1, 1, 2, cable());
		place(helper, 1, 1, 3, cable());
		place(helper, 1, 1, 4, cable());
		StorageNetwork network = StorageNetwork.scan(helper.getLevel(), start);
		StorageGameTests.check(helper, network.getEras().equals(Set.of(ComputerEra.DISK, ComputerEra.FLOPPY)),
				"Should have DISK and FLOPPY eras");
		StorageGameTests.check(helper, network.getConflictingEras().isEmpty(), "Should have no conflicting eras");
		StorageGameTests.check(helper, network.getErasWithoutProcessor().equals(Set.of(ComputerEra.FLOPPY)),
				"Should have FLOPPY without processor");
		StorageGameTests.check(helper, network.getStorage().getCells().size()==1, "Should have 1 cell");
		StorageGameTests.check(helper, network.getStorage(ComputerEra.FLOPPY).getCells().isEmpty(),
				"FLOPPY storage should be empty");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyTerminalWorksOnlyItsOwnDisks(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos terminal = place(helper, 3, 1, 2, StorageGameTests.terminal(ComputerEra.FLOPPY));
		((TerminalDiskBlockEntity)level.getBlockEntity(terminal)).getDisks().insertItem(0,
				new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);
		StorageNetwork network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getStorage().getCells().size()==1, "Should have 1 cell");
		StorageGameTests.check(helper, network.getErasWithoutProcessor().isEmpty(),
				"Should have no eras without processor");

		EraDriveState floppyBank = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_BANK, Direction.NORTH,
				new BlockPos(3, 1, 3));
		floppyBank.getCellSlots().insertItem(0, cell(ComputerEra.FLOPPY, 0), false);
		network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getHosts().size()==2, "Should have 2 hosts");
		StorageGameTests.check(helper, network.getStorage().getCells().size()==1, "Should still have 1 cell");
		StorageGameTests.check(helper, network.getErasWithoutProcessor().equals(Set.of(ComputerEra.FLOPPY)),
				"Should have FLOPPY without processor");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void adapterWithoutAProcessorShows424(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState bank = StorageGameTests.formDriveAt(helper, EraDrive.DRUM_BANK, Direction.NORTH,
				new BlockPos(3, 1, 3));
		bank.getCellSlots().insertItem(0, cell(ComputerEra.DRUM, 3), false);
		place(helper, 4, 1, 2, cable());
		BlockPos adapterPos = place(helper, 4, 2, 2,
				ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.IMPORTER).get().defaultBlockState()
						.setValue(DataAdapterBlock.FACING, Direction.UP));
		BlockPos chest = place(helper, 4, 3, 2, Blocks.CHEST.defaultBlockState());
		((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
		DataAdapterBlockEntity adapter = (DataAdapterBlockEntity)level.getBlockEntity(adapterPos);
		StorageGameTests.check(helper, adapter.transfer()==AdapterStatus.NO_PROCESSOR,
				"Should return NO_PROCESSOR status");
		StorageGameTests.check(helper, ((ChestBlockEntity)level.getBlockEntity(chest)).getItem(0).getCount()==16,
				"Chest should still have 16 cobblestone");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aDiskPackProcessorWorksADiskBank(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState bank = StorageGameTests.formDriveAt(helper, EraDrive.DISK_BANK, Direction.NORTH,
				new BlockPos(5, 1, 5));
		bank.getCellSlots().insertItem(0, cell(ComputerEra.DISK, 0), false);
		BlockPos bankPort = outsidePort(helper, EraDrive.DISK_BANK.geometry(), new BlockPos(5, 1, 5));
		level.setBlockAndUpdate(bankPort, cable());
		StorageNetwork alone = StorageNetwork.scan(level, bankPort);
		StorageGameTests.check(helper, alone.getErasWithoutProcessor().equals(Set.of(ComputerEra.DISK)),
				"a Disk Bank on its own should want a processor");

		EraDriveState processor = StorageGameTests.formDriveAt(helper, EraDrive.DISK_PROCESSOR, Direction.NORTH,
				new BlockPos(1, 1, 3));
		StorageGameTests.check(helper, processor.getCellSlots().insertItem(0, cell(ComputerEra.DISK, 1), false)
				.isEmpty(), "the Disk Pack Processor should take a disk pack");
		cableBetween(helper, bankPort, outsidePort(helper, EraDrive.DISK_PROCESSOR.geometry(), new BlockPos(1, 1, 3)));
		StorageNetwork network = StorageNetwork.scan(level, bankPort);
		StorageGameTests.check(helper, network.getErasWithoutProcessor().isEmpty(),
				"the Disk Pack Processor should work the Disk Bank, wanting: "+network.getErasWithoutProcessor());
		StorageGameTests.check(helper, network.getStorage(ComputerEra.DISK).getCells().size()==2,
				"the Disk Bank's disk and the processor's own should be usable");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theStandaloneProcessorsAreTheirErasProcessors(GameTestHelper helper)
	{
		StorageGameTests.check(helper, EraDrive.DISK_PROCESSOR.isProcessor()
				&&EraDrive.DISK_PROCESSOR.era()==ComputerEra.DISK&&EraDrive.DISK_PROCESSOR.slots()==1
				&&EraDrive.DISK_PROCESSOR.compactSlots()==0, "the Disk Pack Processor should be an era 4 processor "
				+"with one disk pack slot");
		StorageGameTests.check(helper, EraDrive.FLOPPY_PROCESSOR.isProcessor()
				&&EraDrive.FLOPPY_PROCESSOR.era()==ComputerEra.FLOPPY&&EraDrive.FLOPPY_PROCESSOR.slots()==3
				&&EraDrive.FLOPPY_PROCESSOR.compactSlots()==2, "the Floppy Disk Processor should be an era 5 "
				+"processor with a floppy slot and two 3.5\" slots");
		StorageGameTests.check(helper, !EraDrive.COMPACT_FLOPPY_BAY.isProcessor()
				&&EraDrive.COMPACT_FLOPPY_BAY.slots()==4&&EraDrive.COMPACT_FLOPPY_BAY.compactSlots()==4,
				"the Compact Floppy Bay should hold four 3.5\" floppies and process nothing");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void compactSlotsTakeOnlyThreeAndAHalfInchFloppies(GameTestHelper helper)
	{
		ItemStack floppy = cell(ComputerEra.FLOPPY, 0);
		ItemStack small = new ItemStack(ECDataCells.terminalDisks().get(0).get());
		EraDriveState processor = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_PROCESSOR, Direction.NORTH,
				new BlockPos(1, 1, 3));
		var slots = processor.getCellSlots();
		StorageGameTests.check(helper, slots.isItemValid(0, floppy)&&!slots.isItemValid(0, small),
				"the processor's top bay should take floppies only");
		for(int slot = 1; slot<3; slot++)
			StorageGameTests.check(helper, slots.isItemValid(slot, small)&&!slots.isItemValid(slot, floppy),
					"the processor's bay "+slot+" should take 3.5\" floppies only");
		EraDriveState bay = StorageGameTests.formDriveAt(helper, EraDrive.COMPACT_FLOPPY_BAY, Direction.NORTH,
				new BlockPos(5, 1, 5));
		for(int slot = 0; slot<4; slot++)
			StorageGameTests.check(helper, bay.getCellSlots().isItemValid(slot, small)
					&&!bay.getCellSlots().isItemValid(slot, floppy), "the Compact Floppy Bay's bay "+slot
					+" should take 3.5\" floppies only");
		StorageGameTests.check(helper, !EraDriveState.fitsBay(EraDrive.FLOPPY_BANK, 0, small),
				"a Floppy Bank shouldn't take 3.5\" floppies");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aFloppyDiskProcessorWorksACompactFloppyBay(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState bay = StorageGameTests.formDriveAt(helper, EraDrive.COMPACT_FLOPPY_BAY, Direction.NORTH,
				new BlockPos(5, 1, 5));
		bay.getCellSlots().insertItem(2, new ItemStack(ECDataCells.terminalDisks().get(1).get()), false);
		BlockPos bayPort = outsidePort(helper, EraDrive.COMPACT_FLOPPY_BAY.geometry(), new BlockPos(5, 1, 5));
		level.setBlockAndUpdate(bayPort, cable());
		StorageGameTests.check(helper, StorageNetwork.scan(level, bayPort).getErasWithoutProcessor()
				.equals(Set.of(ComputerEra.FLOPPY)), "a Compact Floppy Bay on its own should want a processor");

		EraDriveState processor = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_PROCESSOR, Direction.NORTH,
				new BlockPos(1, 1, 3));
		processor.getCellSlots().insertItem(0, cell(ComputerEra.FLOPPY, 0), false);
		processor.getCellSlots().insertItem(1, new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);
		cableBetween(helper, bayPort, outsidePort(helper, EraDrive.FLOPPY_PROCESSOR.geometry(),
				new BlockPos(1, 1, 3)));
		StorageNetwork network = StorageNetwork.scan(level, bayPort);
		StorageGameTests.check(helper, network.getErasWithoutProcessor().isEmpty(),
				"the Floppy Disk Processor should work the Compact Floppy Bay, wanting: "
						+network.getErasWithoutProcessor());
		StorageGameTests.check(helper, network.getStorage(ComputerEra.FLOPPY).getCells().size()==3,
				"the processor's two disks and the bay's should be usable, got "
						+network.getStorage(ComputerEra.FLOPPY).getCells().size());
		StorageGameTests.check(helper, network.getStorage().insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64,
				false)==64, "the floppies should store items");
		helper.succeed();
	}

	private static void cableBetween(GameTestHelper helper, BlockPos from, BlockPos to)
	{
		ServerLevel level = helper.getLevel();
		int top = helper.absolutePos(new BlockPos(0, 6, 0)).getY();
		for(BlockPos port : List.of(from, to))
			for(int y = port.getY(); y<=top; y++)
				level.setBlockAndUpdate(new BlockPos(port.getX(), y, port.getZ()), cable());
		for(int x = Math.min(from.getX(), to.getX()); x<=Math.max(from.getX(), to.getX()); x++)
			level.setBlockAndUpdate(new BlockPos(x, top, from.getZ()), cable());
		for(int z = Math.min(from.getZ(), to.getZ()); z<=Math.max(from.getZ(), to.getZ()); z++)
			level.setBlockAndUpdate(new BlockPos(to.getX(), top, z), cable());
	}

	private static BlockPos outsidePort(GameTestHelper helper, EraDriveGeometry geometry, BlockPos at)
	{
		NetworkPort port = geometry.ports().get(0);
		return StorageGameTests.world(helper.absolutePos(at), port.pos(), Direction.NORTH)
				.relative(MultiblockOrientation.rotationFor(Direction.NORTH).rotate(port.side()));
	}

	private static BlockPos at(GameTestHelper helper, int x, int y, int z)
	{
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static BlockPos place(GameTestHelper helper, int x, int y, int z, BlockState state)
	{
		BlockPos absolute = at(helper, x, y, z);
		helper.getLevel().setBlockAndUpdate(absolute, state);
		return absolute;
	}

	private static BlockState cable()
	{
		return ECCables.block().get().defaultBlockState();
	}

	private static BlockState bridge()
	{
		return ECBridges.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
	}

	private static ItemStack cell(ComputerEra era, int tier)
	{
		return new ItemStack(ECDataCells.cellsOf(era).get(tier).get());
	}
}
