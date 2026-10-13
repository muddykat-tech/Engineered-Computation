package com.muddykat.engineeredcomputation.pneumatic.pressurizer;

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

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class PressurizerMenu extends AbstractContainerMenu
{
	public static final int FUEL_X = 44;
	public static final int FUEL_Y = 35;
	public static final int INVENTORY_Y = 84;
	private static final int DATA = 2;

	private final BlockPos pos;
	@Nullable
	private final PressurizerBlockEntity pressurizer;
	private final ContainerData data;

	public PressurizerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, inventory, buf.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(DATA));
	}

	private PressurizerMenu(int id, Inventory inventory, BlockPos pos, @Nullable PressurizerBlockEntity pressurizer,
			Container fuel, ContainerData data)
	{
		super(ECMenuTypes.PRESSURIZER.get(), id);
		this.pos = pos;
		this.pressurizer = pressurizer;
		this.data = data;
		addSlot(new Slot(fuel, 0, FUEL_X+1, FUEL_Y+1)
		{
			@Override
			public boolean mayPlace(ItemStack stack)
			{
				return PressurizerBlockEntity.isFuel(stack);
			}
		});
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(inventory, col, 8+col*18, INVENTORY_Y+58));
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, PressurizerBlockEntity pressurizer)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return index==0? pressurizer.getCharge(): ECConfig.carriersPerFuel();
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return DATA;
			}
		};
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PressurizerMenu(id, inventory,
				pressurizer.getBlockPos(), pressurizer, pressurizer.fuelContainer(), data),
				pressurizer.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(pressurizer.getBlockPos()));
	}

	public int getCharge()
	{
		return data.get(0);
	}

	public int getChargePerFuel()
	{
		return Math.max(1, data.get(1));
	}

	public boolean hasFuel()
	{
		return PressurizerBlockEntity.isFuel(slots.get(0).getItem());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved = index==0? moveItemStackTo(stack, 1, slots.size(), true)
				: PressurizerBlockEntity.isFuel(stack)&&moveItemStackTo(stack, 0, 1, false);
		if(!moved)
			return ItemStack.EMPTY;
		if(stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getBlockEntity(pos) instanceof PressurizerBlockEntity&&player.canInteractWithBlock(pos, 4.0);
	}
}
