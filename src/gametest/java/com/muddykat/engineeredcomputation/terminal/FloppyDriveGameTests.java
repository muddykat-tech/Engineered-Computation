package com.muddykat.engineeredcomputation.terminal;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.gametest.MultiblockGameTests;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveMenu;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.EraDataCell;

@TestCategory({"storage", "multiblock"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class FloppyDriveGameTests
{
	private FloppyDriveGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyScreenOpensTerminal(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);
		EraDriveGeometry geometry = EraDrive.FLOPPY_WORKSTATION.geometry();

		AABB monitor = geometry.terminalZones().stream()
				.max(Comparator.comparingDouble(zone -> zone.maxY))
				.orElseThrow();

		AbstractContainerMenu menu = StorageGameTests.click(helper, new StorageGameTests.MenuRecordingPlayer(level),
				new Vec3(monitor.getCenter().x, monitor.getCenter().y, monitor.maxZ), facing);

		StorageGameTests.check(helper, menu instanceof TerminalMenu,
				"clicking the screen didn't open a floppy terminal");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyDrawersOpenStorageBay(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);
		EraDriveGeometry geometry = EraDrive.FLOPPY_WORKSTATION.geometry();

		AABB drawers = geometry.storageZones().get(0);
		AbstractContainerMenu menu = StorageGameTests.click(helper, new StorageGameTests.MenuRecordingPlayer(level),
				new Vec3(drawers.getCenter().x, drawers.getCenter().y, drawers.maxZ), facing);

		StorageGameTests.check(helper, menu instanceof DispenserMenu&&menu.slots.size()==9+36,
				"clicking the drawers didn't open a 9-slot storage bay");

		drive.getStorageBay().setItem(0, new ItemStack(Items.COBBLESTONE, 64));

		ItemStack shown = menu.getSlot(0).getItem();
		StorageGameTests.check(helper, shown.is(Items.COBBLESTONE)&&shown.getCount()==64,
				"storage bay slot 0 didn't show the cobblestone");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyTowerOpensCellSlots(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);

		AbstractContainerMenu menu = StorageGameTests.click(helper, new StorageGameTests.MenuRecordingPlayer(level),
				new Vec3(0.5, 2.5, 1.95), facing);

		StorageGameTests.check(helper, menu instanceof EraDriveMenu,
				"clicking the tower didn't open a cell slot menu");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void sneakUsingFloppyFillsThatBay(GameTestHelper helper)
	{
		Direction facing = Direction.EAST;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		Vec3 upperBay = bayFront(EraDrive.FLOPPY_WORKSTATION, 1);

		StorageGameTests.check(helper, useFloppy(helper, player, false, upperBay, facing)==InteractionResult.PASS,
				"using a floppy on a bay without sneaking was taken as inserting it");

		StorageGameTests.check(helper, useFloppy(helper, player, true, upperBay, facing).consumesAction(),
				"sneak-using a floppy on the upper bay didn't insert it");
		StorageGameTests.check(helper, player.getMainHandItem().isEmpty(), "the inserted floppy stayed in hand");
		StorageGameTests.check(helper, drive.getCellSlots().getStackInSlot(0).isEmpty()
						&&!drive.getCellSlots().getStackInSlot(1).isEmpty(),
				"the floppy went into the wrong bay");

		StorageGameTests.check(helper, useFloppy(helper, player, true, upperBay, facing)==InteractionResult.PASS
						&&!player.getMainHandItem().isEmpty(),
				"a floppy went into a bay that already held one");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void sneakClickingFilledBayTakesFloppy(GameTestHelper helper)
	{
		Direction facing = Direction.NORTH;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		ItemStack disk = new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(1).get());
		drive.getCellSlots().setStackInSlot(0, disk.copy());

		player.setShiftKeyDown(true);
		AbstractContainerMenu menu = StorageGameTests.click(helper, player, bayFront(EraDrive.FLOPPY_WORKSTATION, 0),
				facing);
		StorageGameTests.check(helper, menu==null, "sneak-clicking a filled bay opened a screen");
		StorageGameTests.check(helper, ItemStack.isSameItemSameComponents(player.getMainHandItem(), disk)
						&&drive.getCellSlots().getStackInSlot(0).isEmpty(),
				"sneak-clicking a filled bay with an empty hand didn't take its floppy");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		StorageGameTests.check(helper, StorageGameTests.click(helper, player, bayFront(EraDrive.FLOPPY_WORKSTATION, 0),
						facing) instanceof EraDriveMenu,
				"sneak-clicking an empty bay didn't open the cell slots");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void creativeInsertUsesUpTheFloppy(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.setGameMode(GameType.CREATIVE);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()));

		Vec3 point = bayFront(EraDrive.FLOPPY_WORKSTATION, 0);
		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos block = StorageGameTests.world(origin, BlockPos.containing(point), facing);
		Vec3 hit = Vec3.atLowerCornerOf(origin).add(StorageGameTests.worldOffset(point, facing));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(hit, facing.getOpposite(), block, false));

		StorageGameTests.check(helper, !drive.getCellSlots().getStackInSlot(0).isEmpty(),
				"a creative player's floppy didn't go in");
		StorageGameTests.check(helper, player.getMainHandItem().isEmpty(),
				"a creative player kept a copy of the floppy they put in");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void sneakUsingFloppyFillsBankBay(GameTestHelper helper)
	{
		Direction facing = Direction.SOUTH;
		EraDriveState bank = StorageGameTests.formDrive(helper, EraDrive.FLOPPY_BANK, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());

		StorageGameTests.check(helper, useFloppy(helper, player, true, bayFront(EraDrive.FLOPPY_BANK, 3), facing)
						.consumesAction()&&!bank.getCellSlots().getStackInSlot(3).isEmpty(),
				"sneak-using a floppy on the bank's top bay didn't put it in slot 3");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void sneakUsingDiskFillsTerminalBay(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.WEST;
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		level.setBlockAndUpdate(pos, StorageGameTests.terminal(ComputerEra.FLOPPY)
				.setValue(HorizontalDirectionalBlock.FACING, facing));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECDataCells.terminalDisks().get(0).get()));

		Vec3 hit = Vec3.atLowerCornerOf(pos).add(StorageGameTests.worldOffset(new Vec3(4/16.0, 2.5/16, 1/16.0), facing));
		InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(hit, facing, pos, false)));

		StorageGameTests.check(helper, result.consumesAction()
						&&level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays
						&&bays.getDisks().getStackInSlot(0).isEmpty()&&!bays.getDisks().getStackInSlot(1).isEmpty(),
				"sneak-using a disk on the terminal's right bay didn't put it in slot 1");

		level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(hit, facing, pos, false));
		StorageGameTests.check(helper, EraDataCell.isTerminalDisk(player.getMainHandItem())
						&&level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays
						&&bays.getDisks().getStackInSlot(1).isEmpty()&&player.opened==null,
				"sneak-clicking the terminal's filled bay with an empty hand didn't take its disk");

		helper.succeed();
	}

	private static Vec3 bayFront(EraDrive drive, int slot)
	{
		AABB bay = drive.geometry().bayZones().get(slot);
		return new Vec3(bay.getCenter().x, bay.getCenter().y, bay.maxZ);
	}

	private static InteractionResult useFloppy(GameTestHelper helper, StorageGameTests.MenuRecordingPlayer player,
			boolean sneaking, Vec3 templatePoint, Direction facing)
	{
		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		BlockPos block = StorageGameTests.world(origin, BlockPos.containing(templatePoint), facing);
		Vec3 hit = Vec3.atLowerCornerOf(origin).add(StorageGameTests.worldOffset(templatePoint, facing));
		player.setShiftKeyDown(sneaking);
		player.setItemInHand(InteractionHand.MAIN_HAND,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()));
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(hit, facing.getOpposite(), block, false)));
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void storageBayFollowsStackRules(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);

		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
				StorageGameTests.world(origin, new BlockPos(1, 0, 1), facing), null);

		StorageGameTests.check(helper, handler!=null&&handler.getSlots()==9,
				"storage bay capability not found or wrong slot count");

		ItemStack remainder = handler.insertItem(0, new ItemStack(Items.DIRT, 100), false);
		StorageGameTests.check(helper, remainder.getCount()==36,
				"dirt stack size incorrect after insertion");

		remainder = handler.insertItem(1, new ItemStack(Items.DIAMOND_SWORD, 2), false);
		StorageGameTests.check(helper, remainder.getCount()==1,
				"sword stack size incorrect after insertion");

		StorageGameTests.check(helper, drive.getStorageBay().getItem(0).is(Items.DIRT)
				&&				drive.getStorageBay().getItem(0).getCount()==64,
				"storage bay didn't hold the correct amount of dirt");

		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void storageBaySavedAndSpilled(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.FLOPPY, facing);

		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		EraDriveGeometry geometry = EraDrive.FLOPPY_WORKSTATION.geometry();

		drive.getStorageBay().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		drive.getStorageBay().setItem(4, new ItemStack(Items.DIAMOND_SWORD, 1));

		CompoundTag tag = new CompoundTag();
		drive.save(tag, level.registryAccess());
		EraDriveState reloaded = new EraDriveState(EraDrive.FLOPPY_WORKSTATION, MultiblockGameTests.NO_HOST);
		reloaded.load(tag, level.registryAccess());

		StorageGameTests.check(helper, reloaded.getStorageBay().getItem(0).is(Items.COBBLESTONE)
				&&				reloaded.getStorageBay().getItem(0).getCount()==64,
				"reloaded storage bay didn't hold cobblestone");

		StorageGameTests.check(helper, reloaded.getStorageBay().getItem(4).is(Items.DIAMOND_SWORD)
				&&				reloaded.getStorageBay().getItem(4).getCount()==1,
				"reloaded storage bay didn't hold diamond sword");

		level.destroyBlock(StorageGameTests.world(origin, geometry.master(), facing), false);

		AABB box = new AABB(origin).inflate(6);
		var entities = level.getEntitiesOfClass(ItemEntity.class, box);
		int cobbleCount = 0;
		boolean swordFound = false;

		for(ItemEntity entity : entities)
		{
			if(entity.getItem().is(Items.COBBLESTONE))
				cobbleCount += entity.getItem().getCount();
			else if(entity.getItem().is(Items.DIAMOND_SWORD))
				swordFound = true;
		}

		StorageGameTests.check(helper, cobbleCount==64&&swordFound,
				"items weren't spilled correctly when drive was destroyed");

		helper.succeed();
	}
}
