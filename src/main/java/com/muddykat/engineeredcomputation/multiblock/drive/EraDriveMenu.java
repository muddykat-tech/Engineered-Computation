package com.muddykat.engineeredcomputation.multiblock.drive;

import java.util.function.IntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;

public class EraDriveMenu extends AbstractContainerMenu
{
	public static final int INVENTORY_Y = 63;
	public static final int CELLS_Y = 20;
	public static final int PRIORITY_UP = 0;
	public static final int PRIORITY_DOWN = 1;
	public static final int PRIORITY_UP_TEN = 2;
	public static final int PRIORITY_DOWN_TEN = 3;
	public static final int PRIORITY_ACTIONS = 4;

	private static final double EXTRA_REACH = 4.0;
	private static final Component COMPACT = Component.translatable("gui.engineered_computation.terminal_disks");

	private final int cellSlots;
	private final IntFunction<ItemStack> ghosts;
	private final IntFunction<Component> accepts;
	private final BlockPos pos;
	private final boolean terminalDisks;

	private EraDriveMenu(int id, Inventory playerInventory, IItemHandler cells, int cellSlots,
			IntFunction<ItemStack> ghosts, IntFunction<Component> accepts, BlockPos pos, boolean terminalDisks)
	{
		super(ECMenuTypes.ERA_DRIVE.get(), id);
		this.cellSlots = cellSlots;
		this.ghosts = ghosts;
		this.accepts = accepts;
		this.pos = pos;
		this.terminalDisks = terminalDisks;

		for(int i = 0; i<cellSlots; i++)
			addSlot(new SlotItemHandler(cells, i, 89-9*cellSlots+i*18, CELLS_Y));

		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(playerInventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(playerInventory, col, 8+col*18, INVENTORY_Y+58));
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		int slot = id/PRIORITY_ACTIONS;
		if(slot<0||slot>=cellSlots||!slots.get(slot).hasItem())
			return false;
		int change = switch(id%PRIORITY_ACTIONS)
		{
			case PRIORITY_UP -> 1;
			case PRIORITY_DOWN -> -1;
			case PRIORITY_UP_TEN -> 10;
			default -> -10;
		};
		ItemStack cell = slots.get(slot).getItem().copy();
		CellInventory.setPriority(cell, Math.clamp(CellInventory.priority(cell)+change, EraDataCell.MIN_PRIORITY,
				EraDataCell.MAX_PRIORITY));
		slots.get(slot).set(cell);
		return true;
	}

	public static EraDriveMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf)
	{
		BlockPos pos = buf.readBlockPos();
		if(buf.readBoolean())
			return forTerminalDisks(id, playerInventory, TerminalDiskBlockEntity.createDiskSlots(() -> {
			}, (cell, inserted) -> {
			}), pos);
		EraDrive drive = buf.readEnum(EraDrive.class);
		return forDrive(id, playerInventory, drive, EraDriveState.createCellSlots(drive, () -> {
		}, (cell, inserted) -> {
		}), pos);
	}

	public static EraDriveMenu forDrive(int id, Inventory playerInventory, EraDrive drive, IItemHandler cellSlots,
			BlockPos masterPos)
	{
		return new EraDriveMenu(id, playerInventory, cellSlots, drive.slots(),
				slot -> drive.isCompactSlot(slot)? compactGhost(): new ItemStack(firstTier(drive.era())),
				slot -> drive.isCompactSlot(slot)? COMPACT: drive.era().memoryDisplayName(), masterPos, false);
	}

	private static Item firstTier(ComputerEra era)
	{
		return era==ComputerEra.PUNCHCARD? ECCatalogue.decks().get(0).get(): ECDataCells.cellsOf(era).get(0).get();
	}

	private static ItemStack compactGhost()
	{
		return new ItemStack(ECDataCells.terminalDisks().get(0).get());
	}

	public static EraDriveMenu forTerminalDisks(int id, Inventory playerInventory, IItemHandler disks, BlockPos pos)
	{
		return new EraDriveMenu(id, playerInventory, disks, TerminalDiskBlockEntity.SLOTS, slot -> compactGhost(),
				slot -> COMPACT, pos, true);
	}

	public static void openDrive(ServerPlayer player, EraDrive drive, IItemHandler cellSlots, BlockPos masterPos,
			Component title)
	{
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> forDrive(id, inventory, drive, cellSlots, masterPos), title), buf -> {
			buf.writeBlockPos(masterPos);
			buf.writeBoolean(false);
			buf.writeEnum(drive);
		});
	}

	public static void openTerminalDisks(ServerPlayer player, BlockPos pos, Component title, IItemHandler disks)
	{
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> forTerminalDisks(id, inventory, disks, pos), title), buf -> {
			buf.writeBlockPos(pos);
			buf.writeBoolean(true);
		});
	}

	public int getCellSlotCount()
	{
		return cellSlots;
	}

	public Component getAcceptedCells(int slot)
	{
		return accepts.apply(slot);
	}

	public ItemStack getGhost(int slot)
	{
		return ghosts.apply(slot);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		boolean moved = index<cellSlots
				? moveItemStackTo(stack, cellSlots, slots.size(), true)
				: moveItemStackTo(stack, 0, cellSlots, false);
		if(!moved)
			return ItemStack.EMPTY;

		if(stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player)
	{
		if(terminalDisks)
			return player.level().getBlockEntity(pos) instanceof TerminalDiskBlockEntity
					&&player.canInteractWithBlock(pos, 4.0);
		return isDriveInReach(player, pos);
	}

	static boolean isDriveInReach(Player player, BlockPos masterPos)
	{
		return player.level().getBlockEntity(masterPos) instanceof MultiblockPartBlockEntity part
				&&part.isMaster()
				&&player.canInteractWithBlock(masterPos, EXTRA_REACH);
	}
}
