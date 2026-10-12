package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

/**
 * A Tube Station's screen: its name and whether it takes new items, and for an Advanced Tube Station its Filter Card
 * (or Program Deck) slot, allow / block list and the player's inventory to bring a card from.
 */
public class TubeStationMenu extends AbstractContainerMenu implements NamedStationMenu
{
	public static final int TOGGLE_NEW_ITEMS = 0;
	public static final int TOGGLE_FILTER_MODE = 1;
	public static final int FILTER_X = 150;
	public static final int FILTER_Y = 56;
	public static final int INVENTORY_Y = 104;
	private static final int DATA = 3;

	private final BlockPos pos;
	@Nullable
	private final TubeStationBlockEntity station;
	private final String name;
	private final Component inventoryName;
	private final boolean filters;
	private final Container filterCard;
	private final ContainerData data;

	public TubeStationMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, inventory, buf.readBlockPos(), null, buf.readUtf(), buf.readComponent(), buf.readBoolean(),
				new SimpleContainer(1), new SimpleContainerData(DATA));
	}

	private TubeStationMenu(int id, Inventory inventory, BlockPos pos, @Nullable TubeStationBlockEntity station,
			String name, Component inventoryName, boolean filters, Container filterCard, ContainerData data)
	{
		super(ECMenuTypes.TUBE_STATION.get(), id);
		this.pos = pos;
		this.station = station;
		this.name = name;
		this.inventoryName = inventoryName;
		this.filters = filters;
		this.filterCard = filterCard;
		this.data = data;
		if(filters)
		{
			addSlot(new Slot(filterCard, 0, FILTER_X+1, FILTER_Y+1)
			{
				@Override
				public boolean mayPlace(ItemStack stack)
				{
					return StationFilter.isCard(stack);
				}

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
		}
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, TubeStationBlockEntity station)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return switch(index)
				{
					case 0 -> station.takesNewItems()? 1: 0;
					case 1 -> station.getPendingRequests();
					default -> station.isBlocking()? 1: 0;
				};
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
		Component inventoryName = station.inventoryName();
		boolean filters = station.filters();
		NetworkHooks.openGui(player, new SimpleMenuProvider((id, inventory, p) -> new TubeStationMenu(id, inventory,
				station.getBlockPos(), station, station.getName(), inventoryName, filters, station.filterContainer(),
				data), station.getBlockState().getBlock().getName()), buf -> {
			buf.writeBlockPos(station.getBlockPos());
			buf.writeUtf(station.getName());
			buf.writeComponent(inventoryName);
			buf.writeBoolean(filters);
		});
	}

	@Override
	public String getName()
	{
		return name;
	}

	public Component getInventoryName()
	{
		return inventoryName;
	}

	/** Whether this is an Advanced Tube Station's screen, with a card slot. */
	public boolean filters()
	{
		return filters;
	}

	public ItemStack getFilterCard()
	{
		return filterCard.getItem(0);
	}

	public boolean takesNewItems()
	{
		return data.get(0)!=0;
	}

	public int getPendingRequests()
	{
		return data.get(1);
	}

	public boolean isBlocking()
	{
		return data.get(2)!=0;
	}

	@Override
	public void rename(String name)
	{
		if(station!=null)
			station.setName(name);
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(id==TOGGLE_NEW_ITEMS)
		{
			if(station!=null)
				station.setTakesNewItems(!station.takesNewItems());
			return true;
		}
		if(id==TOGGLE_FILTER_MODE&&filters)
		{
			if(station!=null)
				station.setBlocking(!station.isBlocking());
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		if(!filters)
			return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if(!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved = index==0? moveItemStackTo(stack, 1, slots.size(), true)
				: StationFilter.isCard(stack)&&moveItemStackTo(stack, 0, 1, false);
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
		return player.level.getBlockEntity(pos) instanceof TubeStationBlockEntity
				&&player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))<Mth.square(player.getReachDistance()+4.0);
	}
}
