package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class TubeStationMenu extends AbstractContainerMenu implements NamedStationMenu
{
	public static final int TOGGLE_NEW_ITEMS = 0;

	private final BlockPos pos;
	@Nullable
	private final TubeStationBlockEntity station;
	private final String name;
	private final Component inventoryName;
	private final ContainerData data;

	public TubeStationMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, buf.readBlockPos(), null, buf.readUtf(), buf.readComponent(),
				new SimpleContainerData(2));
	}

	private TubeStationMenu(int id, BlockPos pos, @Nullable TubeStationBlockEntity station, String name,
			Component inventoryName, ContainerData data)
	{
		super(ECMenuTypes.TUBE_STATION.get(), id);
		this.pos = pos;
		this.station = station;
		this.name = name;
		this.inventoryName = inventoryName;
		this.data = data;
		addDataSlots(data);
	}

	public static void open(ServerPlayer player, TubeStationBlockEntity station)
	{
		ContainerData data = new ContainerData()
		{
			@Override
			public int get(int index)
			{
				return index==0? station.takesNewItems()? 1: 0: station.getPendingRequests();
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return 2;
			}
		};
		Component inventoryName = station.inventoryName();
		NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new TubeStationMenu(id,
				station.getBlockPos(),
				station, station.getName(), inventoryName, data), station.getBlockState().getBlock().getName()), buf -> {
			buf.writeBlockPos(station.getBlockPos());
			buf.writeUtf(station.getName());
			buf.writeComponent(inventoryName);
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

	public boolean takesNewItems()
	{
		return data.get(0)!=0;
	}

	public int getPendingRequests()
	{
		return data.get(1);
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
		if(id!=TOGGLE_NEW_ITEMS)
			return false;
		if(station!=null)
			station.setTakesNewItems(!station.takesNewItems());
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getBlockEntity(pos) instanceof TubeStationBlockEntity
				&&player.canReach(pos, 4.0);
	}
}
