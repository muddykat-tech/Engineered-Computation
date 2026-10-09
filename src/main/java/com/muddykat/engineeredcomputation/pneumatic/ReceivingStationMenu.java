package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class ReceivingStationMenu extends AbstractContainerMenu implements NamedStationMenu
{
	public static final int TOGGLE_EJECT = 0;
	public static final int TRAY_X = 62;
	public static final int TRAY_Y = 54;
	public static final int INVENTORY_Y = 124;
	private static final int TRAY_SLOTS = ReceivingStationBlockEntity.SLOTS;

	private final BlockPos pos;
	@Nullable
	private final ReceivingStationBlockEntity receiver;
	private final String name;
	private final ContainerData data;

	public ReceivingStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, inventory, buf.readBlockPos(), null, new SimpleContainer(TRAY_SLOTS), buf.readUtf(),
				new SimpleContainerData(1));
	}

	private ReceivingStationMenu(int id, Inventory inventory, BlockPos pos, @Nullable ReceivingStationBlockEntity receiver,
			Container tray, String name, ContainerData data)
	{
		super(ECMenuTypes.RECEIVING_STATION.get(), id);
		this.pos = pos;
		this.receiver = receiver;
		this.name = name;
		this.data = data;
		for(int row = 0; row<3; row++)
			for(int col = 0; col<3; col++)
				addSlot(new Slot(tray, col+row*3, TRAY_X+1+col*18, TRAY_Y+1+row*18)
				{
					@Override
					public boolean mayPlace(ItemStack stack)
					{
						return false;
					}
				});
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(inventory, col, 8+col*18, INVENTORY_Y+58));
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, ReceivingStationBlockEntity receiver)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return receiver.isEjecting()? 1: 0;
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return 1;
			}
		};
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ReceivingStationMenu(id, inventory,
				receiver.getBlockPos(), receiver, receiver.getTray(), receiver.getName(), data),
				ReceivingStationBlockEntity.TITLE), buf -> {
			buf.writeBlockPos(receiver.getBlockPos());
			buf.writeUtf(receiver.getName());
		});
	}

	@Override
	public String getName()
	{
		return name;
	}

	public boolean isEjecting()
	{
		return data.get(0)!=0;
	}

	@Override
	public void rename(String name)
	{
		if(receiver!=null)
			receiver.setName(name);
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(id!=TOGGLE_EJECT)
			return false;
		if(receiver!=null)
			receiver.setEjecting(!receiver.isEjecting());
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(index>=TRAY_SLOTS||!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if(!moveItemStackTo(stack, TRAY_SLOTS, slots.size(), true))
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
		return player.level().getBlockEntity(pos) instanceof ReceivingStationBlockEntity
				&&player.canInteractWithBlock(pos, 4.0);
	}
}
