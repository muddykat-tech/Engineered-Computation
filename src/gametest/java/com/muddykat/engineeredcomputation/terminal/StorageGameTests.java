package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.gametest.MultiblockGameTests;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveMenu;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECRegistrationHolder;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.storage.CellContents;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.ECDataCells;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class StorageGameTests
{
	static final String TEMPLATE = "gametest/empty_8x8x8";
	static final BlockPos ORIGIN = new BlockPos(3, 1, 3);

	private StorageGameTests()
	{
	}

	@GameTest(template = TEMPLATE)
	public static void cellCapacityMatchesAe2(GameTestHelper helper)
	{
		ItemStack drumCell = new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get());
		int[] saves = {0};
		CellInventory cell = CellInventory.of(drumCell, () -> saves[0]++);
		ItemKey dirt = ItemKey.of(new ItemStack(Items.DIRT));
		ItemKey stone = ItemKey.of(new ItemStack(Items.STONE));
		check(helper, cell.insert(dirt, 5000, true)==1984, "simulated insert should fit 1984");
		check(helper, saves[0]==0&&cell.getAmount(dirt)==0, "simulation changed the cell");
		check(helper, cell.insert(dirt, 5000, false)==1984, "insert should fit 1984");
		check(helper, saves[0]==1, "insert didn't ask for a save");
		check(helper, cell.getUsedBytes()==256, "full cell should use all 256 bytes, uses "+cell.getUsedBytes());
		check(helper, cell.insert(stone, 1, false)==0, "a full cell took a new type");

		check(helper, cell.extract(dirt, 984, false)==984&&cell.getAmount(dirt)==1000, "extract went wrong");
		check(helper, cell.insert(stone, 10_000, true)==920, "new type room should be 920");
		check(helper, cell.extract(dirt, 5000, false)==1000&&cell.getContents().types()==0, "cell didn't empty");
		check(helper, !drumCell.has(ECDataComponents.CELL_CONTENTS.get()),
				"an empty cell kept its contents component");

		CellInventory small = CellInventory.of(new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get()), () -> {
		});
		for(var item : List.of(Items.DIRT, Items.STONE, Items.SAND, Items.GRAVEL))
			check(helper, small.insert(ItemKey.of(new ItemStack(item)), 1, false)==1, "one of the first four types refused");
		check(helper, small.insert(ItemKey.of(new ItemStack(Items.CLAY_BALL)), 1, false)==0, "a fifth type accepted");

		check(helper, cell.insert(ItemKey.of(new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(1).get())), 1, false)==0,
				"a cell was stored inside a cell");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void unstackablesStackInCells(GameTestHelper helper)
	{
		ItemStack drumCell = new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get());
		CellInventory cell = CellInventory.of(drumCell, () -> {
		});
		ItemKey sword = ItemKey.of(new ItemStack(Items.DIAMOND_SWORD));
		ItemStack damaged = new ItemStack(Items.DIAMOND_SWORD);
		damaged.set(DataComponents.DAMAGE, 10);
		ItemKey damagedSword = ItemKey.of(damaged);

		check(helper, cell.insert(sword, 500, false)==500, "500 identical swords didn't fit");
		check(helper, !sword.equals(damagedSword), "a damaged sword has the same key as a new one");
		check(helper, cell.insert(damagedSword, 3, false)==3&&cell.getContents().types()==2, "damaged swords not a separate type");
		check(helper, ItemKey.of(new ItemStack(Items.DIAMOND_SWORD)).equals(sword), "equal stacks gave different keys");

		ItemStack copy = drumCell.copy();
		check(helper, CellInventory.of(copy, () -> {
		}).getAmount(sword)==500, "copied cell lost its contents");
		Tag encoded = CellContents.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
				cell.getContents()).getOrThrow();
		CellContents decoded = CellContents.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
				encoded).getOrThrow();
		check(helper, decoded.equals(cell.getContents()), "contents changed through save/load");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void floppyDriveHasTwoSyncedSlots(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = formDrive(helper, ComputerEra.FLOPPY, Direction.NORTH);
		check(helper, drive.getCellSlots().getSlots()==2, "floppy drive should have 2 slots, has "+drive.getCellSlots().getSlots());
		EraDriveMenu menu = EraDriveMenu.forDrive(3, FakePlayerFactory.getMinecraft(level).getInventory(),
				EraDrive.FLOPPY_WORKSTATION, drive.getCellSlots(), BlockPos.ZERO);
		check(helper, menu.slots.size()==2+36, "floppy drive menu should show 2 cell slots");

		drive.getCellSlots().insertItem(1, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()), false);
		EraDriveGeometry geometry = EraDrive.FLOPPY_WORKSTATION.geometry();
		MultiblockPartBlockEntity master = (MultiblockPartBlockEntity)level.getBlockEntity(
				world(helper.absolutePos(ORIGIN), geometry.master(), Direction.NORTH));
		int filled = master.getUpdateTag(level.registryAccess()).getCompound("sync").getInt(EraDriveState.FILLED_SLOTS);
		check(helper, filled==0b10, "sync data should flag slot 1 only, got "+Integer.toBinaryString(filled));

		ItemStackHandler old = new ItemStackHandler(4);
		ItemStack first = new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(2).get());
		old.setStackInSlot(0, first);
		old.setStackInSlot(3, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(3).get()));
		CompoundTag saved = new CompoundTag();
		saved.put("cells", old.serializeNBT(level.registryAccess()));
		EraDriveState loaded = new EraDriveState(EraDrive.FLOPPY_WORKSTATION, MultiblockGameTests.NO_HOST);
		loaded.load(saved, level.registryAccess());
		check(helper, loaded.getCellSlots().getSlots()==2&&ItemStack.isSameItem(loaded.getCellSlots().getStackInSlot(0), first),
				"loading a 4-slot save didn't keep 2 slots with the first cell");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void terminalsConnectByContact(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get()), false);

		BlockPos first = atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		BlockPos second = first.relative(Direction.NORTH);
		BlockPos otherEra = first.relative(Direction.EAST);
		level.setBlockAndUpdate(first, terminal(ComputerEra.DRUM));
		level.setBlockAndUpdate(second, terminal(ComputerEra.DRUM));
		level.setBlockAndUpdate(otherEra, terminal(ComputerEra.TAPE));

		check(helper, StorageNetwork.scan(level, first).getStorage(ComputerEra.DRUM).getCells().size()==1,
				"terminal at the drive's port doesn't see its cell");
		check(helper, StorageNetwork.scan(level, second).getHosts().size()==1,
				"terminal chained through another terminal doesn't reach the drive");
		check(helper, StorageNetwork.scan(level, otherEra).getHosts().size()==1
						&&StorageNetwork.scan(level, otherEra).getStorage(ComputerEra.TAPE).getCells().isEmpty(),
				"another era's terminal should reach the drive but see none of its cells");

		level.removeBlock(first, false);
		check(helper, StorageNetwork.scan(level, second).getHosts().isEmpty(), "network survived its link being removed");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void terminalClicksAndSync(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState drive = formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos terminalPos = atPort(helper, ComputerEra.DRUM, Direction.NORTH);
		level.setBlockAndUpdate(terminalPos, terminal(ComputerEra.DRUM));

		FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ec_terminal_test"));
		CapturingConnection connection = new CapturingConnection(player);
		player.getInventory().clearContent();
		TerminalMenu menu = new TerminalMenu(7, player.getInventory(), terminalPos);
		menu.broadcastChanges();
		NetworkStorage storage = StorageNetwork.scan(level, terminalPos).getStorage(ComputerEra.DRUM);
		ItemKey cobble = ItemKey.of(new ItemStack(Items.COBBLESTONE));
		ItemKey sword = ItemKey.of(new ItemStack(Items.DIAMOND_SWORD));

		menu.setCarried(new ItemStack(Items.COBBLESTONE, 64));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		check(helper, menu.getCarried().isEmpty()&&storage.getAmount(cobble)==64, "insert-all failed");

		for(int i = 0; i<3; i++)
		{
			menu.setCarried(new ItemStack(Items.DIAMOND_SWORD));
			menu.handleClick(null, TerminalAction.INSERT_ALL);
		}
		check(helper, storage.getAmount(sword)==3, "three swords should be stored as one entry of 3");

		menu.handleClick(sword, TerminalAction.TAKE_STACK);
		check(helper, menu.getCarried().is(Items.DIAMOND_SWORD)&&menu.getCarried().getCount()==1
				&&storage.getAmount(sword)==2, "taking a 'stack' of swords should take exactly one");
		menu.handleClick(null, TerminalAction.INSERT_ONE);
		check(helper, menu.getCarried().isEmpty()&&storage.getAmount(sword)==3, "insert-one failed");

		menu.handleClick(cobble, TerminalAction.TAKE_HALF);
		check(helper, menu.getCarried().getCount()==32&&storage.getAmount(cobble)==32, "take-half failed");
		menu.handleClick(cobble, TerminalAction.TAKE_HALF);
		check(helper, menu.getCarried().getCount()==32, "take-half with a full hand changed the hand");
		menu.setCarried(ItemStack.EMPTY);

		storage.insert(cobble, 168, false);
		menu.handleClick(cobble, TerminalAction.TAKE_HALF);
		check(helper, menu.getCarried().getCount()==32&&storage.getAmount(cobble)==168,
				"take-half of 200 should take half a stack, got "+menu.getCarried().getCount());
		menu.setCarried(ItemStack.EMPTY);
		storage.extract(cobble, 136, false);

		menu.handleClick(cobble, TerminalAction.QUICK_MOVE);
		check(helper, player.getInventory().countItem(Items.COBBLESTONE)==32&&storage.getAmount(cobble)==0,
				"quick-move to inventory failed");
		int slot = -1;
		for(int i = 0; i<menu.slots.size(); i++)
			if(menu.slots.get(i).getItem().is(Items.COBBLESTONE))
				slot = i;
		menu.quickMoveStack(player, slot);
		menu.broadcastChanges();
		check(helper, player.getInventory().countItem(Items.COBBLESTONE)==0&&storage.getAmount(cobble)==32,
				"shift-clicking from the inventory didn't store");

		check(helper, !connection.payloads.isEmpty()&&connection.payloads.get(0).reset(), "first update didn't reset");
		TerminalMenu clientCopy = new TerminalMenu(7, player.getInventory(), terminalPos);
		connection.payloads.forEach(clientCopy::applyUpdate);
		Map<ItemKey, Long> expected = new HashMap<>();
		storage.collect(expected);
		check(helper, clientCopy.getClientContents().equals(expected),
				"client copy "+clientCopy.getClientContents()+" doesn't match storage "+expected);
		check(helper, clientCopy.getClientStats().equals(new TerminalStats(21, 2048, 2, 10, 1, 0)),
				"capacity not synced: "+clientCopy.getClientStats());
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void templatePointsRoundTrip(GameTestHelper helper)
	{
		List<Vec3> points = List.of(new Vec3(0.1, 0.2, 0.3), new Vec3(2.4375, 1.625, 2.9), new Vec3(1.9, 3.5, 0.05));
		for(Direction facing : Direction.Plane.HORIZONTAL)
			for(Vec3 point : points)
			{
				Vec3 back = MultiblockOrientation.toTemplatePoint(worldOffset(point, facing), facing);
				check(helper, back.distanceTo(point)<1e-6, facing+": "+point+" came back as "+back);
			}
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void diskPackConsoleOpensTerminal(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Direction facing = Direction.EAST;
		formDrive(helper, ComputerEra.DISK, facing);
		EraDriveGeometry geometry = EraDrive.DISK_MAINFRAME.geometry();
		check(helper, geometry.terminalZones().size()==2, "expected a screen and a keyboard zone");
		AABB monitor = geometry.terminalZones().stream().max(Comparator.comparingDouble(zone -> zone.maxY)).orElseThrow();
		AABB desk = geometry.terminalZones().stream().min(Comparator.comparingDouble(zone -> zone.maxY)).orElseThrow();
		MenuRecordingPlayer player = new MenuRecordingPlayer(level);

		AbstractContainerMenu screen = click(helper, player,
				new Vec3(monitor.getCenter().x, monitor.getCenter().y, monitor.maxZ), facing);
		check(helper, screen instanceof TerminalMenu,
				"clicking the screen opened "+screen);
		check(helper, screen.stillValid(player), "a terminal on the console should stay open");
		AbstractContainerMenu keyboard = click(helper, player,
				new Vec3(desk.getCenter().x, desk.maxY, (monitor.maxZ+desk.maxZ)/2), facing);
		check(helper, keyboard instanceof TerminalMenu, "clicking the keyboard opened "+keyboard);
		AbstractContainerMenu diskUnit = click(helper, player, new Vec3(11/16.0, 8/16.0, 47/16.0), facing);
		check(helper, diskUnit instanceof EraDriveMenu, "clicking the disk unit opened "+diskUnit);

		level.destroyBlock(world(helper.absolutePos(ORIGIN), geometry.master(), facing), false);
		check(helper, !screen.stillValid(player), "the console's terminal should close once the drive is broken");
		helper.succeed();
	}

	@GameTest(template = TEMPLATE)
	public static void everyItemHasARecipe(GameTestHelper helper)
	{
		List<String> missing = new ArrayList<>();
		for(var item : ECRegistrationHolder.ITEMS.getEntries())
			if(helper.getLevel().getRecipeManager().byKey(item.getId()).isEmpty())
				missing.add(item.getId().getPath());
		check(helper, missing.isEmpty(), "no recipe loaded for "+missing);
		helper.succeed();
	}

	@Nullable
	static AbstractContainerMenu click(
			GameTestHelper helper, MenuRecordingPlayer player, Vec3 templatePoint, Direction facing)
	{
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(ORIGIN);
		BlockPos block = world(origin, BlockPos.containing(templatePoint), facing);
		Vec3 hit = Vec3.atLowerCornerOf(origin).add(worldOffset(templatePoint, facing));
		player.setPos(hit);
		player.opened = null;
		level.getBlockState(block).useWithoutItem(level, player, new BlockHitResult(hit, facing.getOpposite(), block, false));
		return player.opened==null? null: player.opened.createMenu(1, player.getInventory(), player);
	}

	static Vec3 worldOffset(Vec3 templatePoint, Direction facing)
	{
		BlockPos block = BlockPos.containing(templatePoint);
		Vec3 inBlock = templatePoint.subtract(Vec3.atLowerCornerOf(block));
		Vec3 turned = MultiblockOrientation.rotateShape(Shapes.create(new AABB(inBlock, inBlock).inflate(1e-4)), facing)
				.bounds().getCenter();
		return Vec3.atLowerCornerOf(MultiblockOrientation.toWorldOffset(block, facing)).add(turned);
	}

	static EraDriveState formDrive(GameTestHelper helper, ComputerEra era, Direction facing)
	{
		return formDrive(helper, era.drives().get(0), facing);
	}

	static EraDriveState formDrive(GameTestHelper helper, EraDrive drive, Direction facing)
	{
		return formDriveAt(helper, drive, facing, ORIGIN);
	}

	static EraDriveState formDriveAt(GameTestHelper helper, EraDrive drive, Direction facing, BlockPos at)
	{
		ComputerEra era = drive.era();
		ServerLevel level = helper.getLevel();
		EraDriveGeometry geometry = drive.geometry();
		BlockPos origin = helper.absolutePos(at);
		Block casing = ECContent.casing(era).get();
		for(BlockPos pos : geometry.shapes().keySet())
			level.setBlockAndUpdate(world(origin, pos, facing), casing.defaultBlockState());
		BlockPos trigger = world(origin, geometry.trigger(), facing);
		if(!ECMultiblocks.ERA_DRIVES.get(drive).tryForm(level, trigger, facing.getOpposite()))
			helper.fail("drive didn't form");
		if(level.getBlockEntity(world(origin, geometry.master(), facing)) instanceof MultiblockPartBlockEntity master
				&&master.getState() instanceof EraDriveState state)
			return state;
		helper.fail("formed drive has no state");
		throw new IllegalStateException();
	}

	static BlockPos atPort(GameTestHelper helper, ComputerEra era, Direction facing)
	{
		NetworkPort port = era.drives().get(0).geometry().ports().get(0);
		return world(helper.absolutePos(ORIGIN), port.pos(), facing)
				.relative(MultiblockOrientation.rotationFor(facing).rotate(port.side()));
	}

	static BlockPos world(BlockPos origin, BlockPos templatePos, Direction facing)
	{
		return origin.offset(MultiblockOrientation.toWorldOffset(templatePos, facing));
	}

	static BlockState terminal(ComputerEra era)
	{
		return ECTerminals.block(era).get().defaultBlockState();
	}

	static void check(GameTestHelper helper, boolean condition, String failure)
	{
		if(!condition)
			helper.fail(failure);
	}

	static final class MenuRecordingPlayer extends FakePlayer
	{
		@Nullable
		MenuProvider opened;

		MenuRecordingPlayer(ServerLevel level)
		{
			super(level, new GameProfile(UUID.randomUUID(), "ec_menu_test"));
		}

		@Override
		public OptionalInt openMenu(
				@Nullable MenuProvider provider, @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter)
		{
			opened = provider;
			return OptionalInt.of(1);
		}
	}

	private static final class CapturingConnection extends ServerGamePacketListenerImpl
	{
		final List<TerminalContentsPayload> payloads = new ArrayList<>();

		CapturingConnection(ServerPlayer player)
		{
			super(player.server, new Connection(PacketFlow.CLIENTBOUND), player,
					CommonListenerCookie.createInitial(player.getGameProfile(), false));
		}

		@Override
		public void send(Packet<?> packet)
		{
			if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof TerminalContentsPayload update)
				payloads.add(update);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketSendListener listener)
		{
			send(packet);
		}
	}
}
