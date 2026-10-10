package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class DataAdapterMenu extends AbstractContainerMenu
{
	public static final int FILTER_SLOT = 0;
	private static final int INVENTORY_SLOTS = 36;
	public static final int FILTER_X = 17;
	public static final int FILTER_Y = 25;
	public static final int INVENTORY_Y = 88;

	public static final int ITEMS_DOWN = 0;
	public static final int ITEMS_UP = 1;
	public static final int TICKS_DOWN = 2;
	public static final int TICKS_UP = 3;
	public static final int BIG = 4;
	public static final int TOGGLE_POWER = 2*BIG;
	private static final int BIG_ITEMS = 8;
	private static final int BIG_TICKS = 10;

	private final BlockPos pos;
	private final Player player;
	private final DataAdapterBlockEntity adapter;
	private final Container filter;
	private final ContainerData data;

	public DataAdapterMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, inventory, buf.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(4));
	}

	DataAdapterMenu(int id, Inventory inventory, DataAdapterBlockEntity adapter)
	{
		this(id, inventory, adapter.getBlockPos(), adapter, adapter.filterContainer(), adapter.data());
	}

	private DataAdapterMenu(int id, Inventory inventory, BlockPos pos, DataAdapterBlockEntity adapter, Container filter,
			ContainerData data)
	{
		super(ECMenuTypes.DATA_ADAPTER.get(), id);
		this.pos = pos;
		this.player = inventory.player;
		this.adapter = adapter;
		this.filter = filter;
		this.data = data;
		addSlot(new Slot(filter, 0, FILTER_X+1, FILTER_Y+1)
		{
			@Override
			public int getMaxStackSize()
			{
				return 1;
			}
		});
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18));
		for(int col = 0; col<9; col++)
			addSlot(new Slot(inventory, col, 8+col*18, INVENTORY_Y+58));
		addDataSlots(data);
	}

	public BlockPos getPos()
	{
		return pos;
	}

	public DataAdapterBlock block()
	{
		BlockState state = player.level.getBlockState(pos);
		return state.getBlock() instanceof DataAdapterBlock block? block: null;
	}

	public AdapterStatus getStatus()
	{
		AdapterStatus[] statuses = AdapterStatus.values();
		return statuses[Math.max(0, Math.min(statuses.length-1, data.get(0)))];
	}

	public int itemsPerMove()
	{
		return data.get(1);
	}

	public int ticksBetween()
	{
		return data.get(2);
	}

	public boolean takesPrograms()
	{
		DataAdapterBlock block = block();
		return block!=null&&AdapterFilter.takesPrograms(block.getEra());
	}

	public boolean accepts(ItemStack floppy)
	{
		DataAdapterBlock block = block();
		return block!=null&&AdapterFilter.accepts(floppy, block.getEra());
	}

	public ItemStack getFilter()
	{
		return filter.getItem(0);
	}

	public boolean isEnabled()
	{
		return data.get(3)!=0;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player)
	{
		if(slotId!=FILTER_SLOT)
		{
			super.clicked(slotId, button, clickType, player);
			return;
		}
		ItemStack current = filter.getItem(0);
		ItemStack carried = getCarried();
		boolean holdsProgram = AdapterFilter.isProgram(current);
		if(clickType==ClickType.QUICK_MOVE)
		{
			if(holdsProgram&&moveItemStackTo(current.copy(), 1, 1+INVENTORY_SLOTS, true))
				filter.setItem(0, ItemStack.EMPTY);
			else if(!holdsProgram)
				filter.setItem(0, ItemStack.EMPTY);
			return;
		}
		if(clickType!=ClickType.PICKUP)
			return;
		if(carried.isEmpty())
		{
			if(holdsProgram)
				setCarried(current);
			filter.setItem(0, ItemStack.EMPTY);
		}
		else if(AdapterFilter.isProgram(carried))
		{
			if(!accepts(carried))
				return;
			filter.setItem(0, carried.split(1));
			if(holdsProgram)
			{
				if(carried.isEmpty())
					setCarried(current);
				else if(!player.getInventory().add(current))
					player.drop(current, false);
			}
		}
		else if(!holdsProgram)
			filter.setItem(0, ItemHandlerHelper.copyStackWithSize(carried, 1));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		if(index==FILTER_SLOT)
			return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		ItemStack stack = slot.getItem();
		if(stack.isEmpty()||AdapterFilter.isProgram(filter.getItem(0)))
			return ItemStack.EMPTY;
		if(AdapterFilter.isProgram(stack))
		{
			if(!accepts(stack))
				return ItemStack.EMPTY;
			filter.setItem(0, stack.split(1));
			slot.setChanged();
		}
		else
			filter.setItem(0, ItemHandlerHelper.copyStackWithSize(stack, 1));
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(adapter==null)
			return true;
		if(id==TOGGLE_POWER)
		{
			adapter.setEnabled(!adapter.isEnabled());
			return true;
		}
		boolean big = id>=BIG;
		switch(id%BIG)
		{
			case ITEMS_DOWN -> adapter.setSpeed(adapter.getItems()-(big? BIG_ITEMS: 1), adapter.getTicks());
			case ITEMS_UP -> adapter.setSpeed(adapter.getItems()+(big? BIG_ITEMS: 1), adapter.getTicks());
			case TICKS_DOWN -> adapter.setSpeed(adapter.getItems(), adapter.getTicks()-(big? BIG_TICKS: 1));
			case TICKS_UP -> adapter.setSpeed(adapter.getItems(), adapter.getTicks()+(big? BIG_TICKS: 1));
			default ->
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level.getBlockState(pos).getBlock() instanceof DataAdapterBlock
				&&player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))<Mth.square(player.getReachDistance()+4.0);
	}
}
