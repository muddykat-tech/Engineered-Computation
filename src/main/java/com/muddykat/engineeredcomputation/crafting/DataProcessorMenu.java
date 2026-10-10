package com.muddykat.engineeredcomputation.crafting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class DataProcessorMenu extends AbstractContainerMenu
{
	public static final int SLOTS_X = 62;
	public static final int SLOTS_Y = 18;
	public static final int STATUS_Y = 76;
	public static final int INVENTORY_Y = 104;
	private static final double EXTRA_REACH = 4.0;

	private final BlockPos pos;
	private final ContainerData data;

	public DataProcessorMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, inventory, DataProcessorState.createPatternSlots(() -> {}), buf.readBlockPos(),
				new SimpleContainerData(1));
	}

	DataProcessorMenu(int id, Inventory inventory, DataProcessorState processor, BlockPos pos)
	{
		this(id, inventory, processor.getPatternSlots(), pos, new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return processor.status().ordinal();
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
		});
	}

	private DataProcessorMenu(int id, Inventory inventory, IItemHandler patterns, BlockPos pos, ContainerData data)
	{
		super(ECMenuTypes.DATA_PROCESSOR.get(), id);
		this.pos = pos;
		this.data = data;
		for(int i = 0; i<DataProcessorState.PATTERN_SLOTS; i++)
			addSlot(new SlotItemHandler(patterns, i, SLOTS_X+i%3*18, SLOTS_Y+i/3*18));
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(inventory, col, 8+col*18, INVENTORY_Y+58));
		addDataSlots(data);
	}

	@Nullable
	public StatusCode status()
	{
		int index = data.get(0);
		return index>=0&&index<StatusCode.values().length? StatusCode.values()[index]: null;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int patterns = DataProcessorState.PATTERN_SLOTS;
		boolean moved = index<patterns? moveItemStackTo(stack, patterns, slots.size(), true):
				moveItemStackTo(stack, 0, patterns, false);
		if(!moved)
			return ItemStack.EMPTY;
		if(stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))
				<Mth.square(player.getReachDistance()+EXTRA_REACH)&&player.level.getBlockEntity(pos)!=null;
	}
}
