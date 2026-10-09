package com.muddykat.engineeredcomputation.terminal;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveMenu;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

@TestCategory({"network", "storage"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class EraMixingGameTests
{
	private EraMixingGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anyTerminalWorksWithAnyEra(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get()), false);
		BlockPos terminalPos = StorageGameTests.atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(terminalPos, StorageGameTests.terminal(ComputerEra.TAPE));

		Player player = FakePlayerFactory.getMinecraft(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminalPos);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().isEmpty(),
				"insert-all failed: carried stack not empty");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void tapeAndDiskClashWithoutABridge(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState tapeDrive = StorageGameTests.formDrive(helper, ComputerEra.TAPE, Direction.NORTH);
		tapeDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).get(0).get()),
				false);
		EraDriveState diskDrive = StorageGameTests.formDriveAt(helper, EraDrive.DISK_MAINFRAME, Direction.NORTH,
				new BlockPos(0, 1, 5));
		diskDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()),
				false);

		BlockPos cable1 = place(helper, 3, 1, 2, cable());
		BlockPos cable2 = place(helper, 2, 1, 2, cable());
		BlockPos cable3 = place(helper, 1, 1, 2, cable());
		BlockPos cable4 = place(helper, 0, 1, 2, cable());
		BlockPos cable5 = place(helper, 0, 1, 3, cable());
		BlockPos cable6 = place(helper, 0, 1, 4, cable());
		place(helper, 1, 1, 4, cable());
		BlockPos terminal = place(helper, 2, 1, 1, StorageGameTests.terminal(ComputerEra.DISK));

		Set<ComputerEra> conflicting = StorageNetwork.scan(level, terminal).getConflictingEras();
		StorageGameTests.check(helper, conflicting.equals(Set.of(ComputerEra.TAPE, ComputerEra.DISK)),
				"conflicting eras should be TAPE and DISK, got "+conflicting);

		Player player = FakePlayerFactory.getMinecraft(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminal);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, !menu.getCarried().isEmpty()&&menu.getCarried().getCount()==64,
				"terminal should not accept items when locked");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void storageBridgeJoinsTapeAndDisk(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState tapeDrive = StorageGameTests.formDrive(helper, ComputerEra.TAPE, Direction.NORTH);
		tapeDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).get(0).get()),
				false);
		EraDriveState diskDrive = StorageGameTests.formDriveAt(helper, EraDrive.DISK_MAINFRAME, Direction.NORTH,
				new BlockPos(0, 1, 5));
		diskDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()),
				false);

		BlockPos cable1 = place(helper, 3, 1, 2, cable());
		BlockPos cable2 = place(helper, 2, 1, 2, cable());
		BlockPos cable3 = place(helper, 1, 1, 2, cable());
		BlockPos cable4 = place(helper, 0, 1, 2, cable());
		BlockPos cable5 = place(helper, 0, 1, 3, cable());
		BlockPos cable6 = place(helper, 0, 1, 4, cable());
		place(helper, 1, 1, 4, cable());
		BlockPos bridge = place(helper, 1, 1, 2,
				ECBridges.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
						Direction.NORTH));
		BlockPos terminal = place(helper, 2, 1, 1, StorageGameTests.terminal(ComputerEra.DISK));

		Set<ComputerEra> conflicting = StorageNetwork.scan(level, terminal).getConflictingEras();
		StorageGameTests.check(helper, conflicting.isEmpty(),
				"conflicting eras should be empty, got "+conflicting);
		int cellCount = StorageNetwork.scan(level, terminal).getStorage().getCells().size();
		StorageGameTests.check(helper, cellCount==2,
				"should have 2 cells, got "+cellCount);

		Player player = FakePlayerFactory.getMinecraft(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminal);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().isEmpty(),
				"terminal should accept items after bridge");

		BlockPos floppyTerminal = place(helper, 3, 1, 1, StorageGameTests.terminal(ComputerEra.FLOPPY));
		((TerminalDiskBlockEntity)level.getBlockEntity(floppyTerminal)).getDisks().insertItem(0,
				new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);
		conflicting = StorageNetwork.scan(level, terminal).getConflictingEras();
		StorageGameTests.check(helper, conflicting.equals(Set.of(ComputerEra.TAPE, ComputerEra.FLOPPY)),
				"conflicting eras should be TAPE and FLOPPY, got "+conflicting);

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 400)
	public static void tapeDriveArchivesBigStocks(GameTestHelper helper)
	{
		ArchiveRig rig = archiveRig(helper);
		helper.succeedWhen(() -> {
			long onTape = rig.tape().getCells().get(0).getAmount(rig.stock());
			long total = StorageNetwork.scan(helper.getLevel(), rig.terminal()).getStorage().getAmount(rig.stock());
			StorageGameTests.check(helper, total==2000, "archiving shouldn't change the total, got "+total);
			StorageGameTests.check(helper, onTape==2000, "the reel should hold all 2000, got "+onTape);
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, batch = "tapeArchivingOff")
	public static void tapeArchivingCanBeTurnedOff(GameTestHelper helper)
	{
		ECConfig.TAPE_ARCHIVING.set(false);
		ArchiveRig rig = archiveRig(helper);
		helper.runAfterDelay(6*EraDriveState.ARCHIVE_TICKS, () -> {
			try
			{
				long onTape = rig.tape().getCells().get(0).getAmount(rig.stock());
				StorageGameTests.check(helper, onTape==0, "with archiving off the reel should stay empty, got "+onTape);
			}
			finally
			{
				ECConfig.TAPE_ARCHIVING.set(true);
			}
			helper.succeed();
		});
	}

	private static ArchiveRig archiveRig(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState tapeDrive = StorageGameTests.formDrive(helper, ComputerEra.TAPE, Direction.NORTH);
		tapeDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).get(0).get()),
				false);
		EraDriveState diskDrive = StorageGameTests.formDriveAt(helper, EraDrive.DISK_MAINFRAME, Direction.NORTH,
				new BlockPos(0, 1, 5));
		diskDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()),
				false);
		place(helper, 3, 1, 2, cable());
		place(helper, 2, 1, 2, cable());
		place(helper, 0, 1, 2, cable());
		place(helper, 0, 1, 3, cable());
		place(helper, 0, 1, 4, cable());
		place(helper, 1, 1, 4, cable());
		place(helper, 1, 1, 2, ECBridges.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
				Direction.NORTH));
		BlockPos terminal = place(helper, 2, 1, 1, StorageGameTests.terminal(ComputerEra.DISK));

		ItemKey cobblestone = ItemKey.of(new ItemStack(Items.COBBLESTONE));
		NetworkStorage storage = StorageNetwork.scan(level, terminal).getStorage();
		StorageGameTests.check(helper, storage.insert(cobblestone, 2000, false)==2000,
				"the network should take 2000 cobblestone");
		StorageGameTests.check(helper, diskDrive.getCells().get(0).getAmount(cobblestone)==2000,
				"a new type should land on the disk pack first, got "+diskDrive.getCells().get(0).getAmount(cobblestone));
		return new ArchiveRig(tapeDrive, terminal, cobblestone);
	}

	private record ArchiveRig(EraDriveState tape, BlockPos terminal, ItemKey stock)
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyWorkstationWorksADiskBankAcrossABridge(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState diskDrive = StorageGameTests.formDriveAt(helper, EraDrive.DISK_BANK, Direction.NORTH,
				new BlockPos(0, 1, 5));
		diskDrive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()),
				false);
		place(helper, 0, 1, 4, cable());
		place(helper, 0, 1, 3, cable());
		place(helper, 0, 1, 2, cable());
		place(helper, 1, 1, 2, ECBridges.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
				Direction.NORTH));
		place(helper, 2, 1, 2, cable());
		BlockPos terminal = place(helper, 3, 1, 2, StorageGameTests.terminal(ComputerEra.FLOPPY));
		((TerminalDiskBlockEntity)level.getBlockEntity(terminal)).getDisks().insertItem(0,
				new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);

		StorageNetwork network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getEras().equals(Set.of(ComputerEra.DISK, ComputerEra.FLOPPY)),
				"the network should hold disk pack and floppy storage, got "+network.getEras());
		StorageGameTests.check(helper, network.getConflictingEras().isEmpty(),
				"the bridge should keep the eras apart, got a clash of "+network.getConflictingEras());
		StorageGameTests.check(helper, network.getStorage().getCells().size()==1
						&&network.getErasWithoutProcessor().equals(Set.of(ComputerEra.DISK)),
				"with no processor, only the terminal's own disk should work, got "+network.getStorage().getCells().size()
						+" cells and "+network.getErasWithoutProcessor()+" without a processor");

		StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_WORKSTATION, Direction.NORTH, new BlockPos(4, 1, 3));
		place(helper, 4, 1, 2, cable());
		network = StorageNetwork.scan(level, terminal);
		StorageGameTests.check(helper, network.getConflictingEras().isEmpty()
						&&network.getErasWithoutProcessor().isEmpty()&&network.getStorage().getCells().size()==2,
				"the workstation should work the disk bank across the bridge, got "
						+network.getStorage().getCells().size()+" cells and "+network.getErasWithoutProcessor()
						+" without a processor");

		Player player = FakePlayerFactory.getMinecraft(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), terminal);
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().isEmpty(), "the terminal should take items through the bridge");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void bridgeConnectsOnlyOnItsSides(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos bridgePos = place(helper, 3, 1, 3,
				ECBridges.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
						Direction.NORTH));
		BlockPos eastCable = place(helper, 4, 1, 3, cable());
		BlockPos northCable = place(helper, 3, 1, 2, cable());

		BlockState bridge = level.getBlockState(bridgePos);
		BlockState east = level.getBlockState(eastCable);
		BlockState north = level.getBlockState(northCable);

		StorageGameTests.check(helper, east.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.WEST)),
				"east cable should connect to the bridge on its west side");
		StorageGameTests.check(helper, !north.getValue(NetworkCableBlock.CONNECTIONS.get(Direction.SOUTH)),
				"north cable should not connect to the bridge on its south side");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyTerminalDisks(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos terminalPos = place(helper, 3, 1, 3, StorageGameTests.terminal(ComputerEra.FLOPPY));

		TerminalDiskBlockEntity terminal = (TerminalDiskBlockEntity)level.getBlockEntity(terminalPos);
		ItemStack floppy = new ItemStack(ECDataCells.terminalDisks().get(0).get());
		ItemStack cell = new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get());

		ItemStack result1 = terminal.getDisks().insertItem(0, floppy, false);
		StorageGameTests.check(helper, result1.isEmpty(),
				"inserting a 3.5-inch floppy into an empty slot should return empty stack");
		ItemStack result2 = terminal.getDisks().insertItem(1, cell, false);
		StorageGameTests.check(helper, !result2.isEmpty(),
				"inserting a big floppy cell into an empty slot should return non-empty stack");

		ItemStack result3 = terminal.getDisks().insertItem(1, floppy, false);
		StorageGameTests.check(helper, result3.isEmpty(), "the second 3.5-inch floppy should go in the other bay");

		int cellCount = StorageNetwork.scan(level, terminalPos).getStorage(ComputerEra.FLOPPY).getCells().size();
		StorageGameTests.check(helper, cellCount==2,
				"should have 2 cells, got "+cellCount);

		EraDriveState drive = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_WORKSTATION, Direction.NORTH,
				new BlockPos(3, 1, 5));
		ItemStack result4 = drive.getCellSlots().insertItem(0, floppy, false);
		StorageGameTests.check(helper, !result4.isEmpty(),
				"inserting a 3.5-inch floppy into a floppy drive should return non-empty stack");

		level.destroyBlock(terminalPos, false);
		int count = 0;
		for(ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(terminalPos).inflate(2)))
			if(ECDataCells.terminalDisks().stream().anyMatch(d -> entity.getItem().is(d.get())))
				count++;
		StorageGameTests.check(helper, count==2,
				"should have 2 floppy items after breaking the terminal, got "+count);

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyTerminalBaysOpenTheBays(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos terminalPos = place(helper, 3, 1, 3, StorageGameTests.terminal(ComputerEra.FLOPPY));

		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);

		Vec3 hitBays = Vec3.atLowerCornerOf(terminalPos).add(0.5, 0.15, 0.0625);
		player.setPos(hitBays);
		level.getBlockState(terminalPos).use(level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(hitBays, Direction.NORTH, terminalPos, false));
		AbstractContainerMenu menuBays = player.openedMenu();
		player.forgetMenu();

		Vec3 hitScreen = Vec3.atLowerCornerOf(terminalPos).add(0.5, 0.6, 0.0625);
		player.setPos(hitScreen);
		level.getBlockState(terminalPos).use(level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(hitScreen, Direction.NORTH, terminalPos, false));
		AbstractContainerMenu menuScreen = player.openedMenu();

		StorageGameTests.check(helper, menuBays instanceof EraDriveMenu,
				"clicking the bays should open an EraDriveMenu, got "+menuBays);
		StorageGameTests.check(helper, ((EraDriveMenu)menuBays).getCellSlotCount()==2,
				"the bays menu should have 2 slots, got "+((EraDriveMenu)menuBays).getCellSlotCount());
		StorageGameTests.check(helper, menuScreen instanceof TerminalMenu,
				"clicking the screen should open a TerminalMenu, got "+menuScreen);

		helper.succeed();
	}

	private static BlockPos place(GameTestHelper helper, int x, int y, int z, BlockState state)
	{
		BlockPos absolute = helper.absolutePos(new BlockPos(x, y, z));
		helper.getLevel().setBlockAndUpdate(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(),
				absolute));
		return absolute;
	}

	private static BlockState cable()
	{
		return ECCables.block().get().defaultBlockState();
	}
}
