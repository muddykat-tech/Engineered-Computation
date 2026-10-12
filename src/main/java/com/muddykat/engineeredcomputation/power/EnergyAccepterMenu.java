package com.muddykat.engineeredcomputation.power;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class EnergyAccepterMenu extends AbstractContainerMenu
{
	private static final int STORED = 0;
	private static final int CAPACITY = 1;
	private static final int COST = 2;
	private static final int NETWORK_STORED = 3;
	private static final int NETWORK_CAPACITY = 4;
	private static final int FLAGS = 5;
	private static final int VALUES = 6;
	private static final int ENABLED = 1;
	private static final int POWERED = 2;

	private final BlockPos pos;
	private final ContainerData data;

	public EnergyAccepterMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, buf.readBlockPos(), new SimpleContainerData(2*VALUES));
	}

	EnergyAccepterMenu(int id, Inventory inventory, EnergyAccepterBlockEntity accepter)
	{
		this(id, accepter.getBlockPos(), split(accepter));
	}

	private EnergyAccepterMenu(int id, BlockPos pos, ContainerData data)
	{
		super(ECMenuTypes.ENERGY_ACCEPTER.get(), id);
		this.pos = pos;
		this.data = data;
		addDataSlots(data);
	}

	/** The accepter's values, each as its low and high 16 bits. */
	private static ContainerData split(EnergyAccepterBlockEntity accepter)
	{
		return new ContainerData()
		{
			@Override
			public int get(int index)
			{
				int value = value(accepter, index/2);
				return index%2==0? value&0xFFFF: value>>>16;
			}

			@Override
			public void set(int index, int value)
			{
			}

			@Override
			public int getCount()
			{
				return 2*VALUES;
			}
		};
	}

	private static int value(EnergyAccepterBlockEntity accepter, int value)
	{
		if(value==STORED)
			return accepter.getEnergy();
		if(value==CAPACITY)
			return EnergyAccepterBlockEntity.capacity();
		StorageNetwork network = StorageNetwork.of(accepter.getLevel(), accepter.getBlockPos());
		return switch(value)
		{
			case COST -> clamp(network.getPowerCost(accepter.getLevel()));
			case NETWORK_STORED -> clamp(EnergyAccepterBlockEntity.stored(
					EnergyAccepterBlockEntity.accepters(accepter.getLevel(), network)));
			case NETWORK_CAPACITY -> clamp((long)network.getAccepters().size()*EnergyAccepterBlockEntity.capacity());
			default -> (ECConfig.powerEnabled()? ENABLED: 0)|(network.isPowered(accepter.getLevel())? POWERED: 0);
		};
	}

	private static int clamp(long value)
	{
		return (int)Math.min(Integer.MAX_VALUE, value);
	}

	private int get(int value)
	{
		return data.get(2*value)&0xFFFF|(data.get(2*value+1)&0xFFFF)<<16;
	}

	public int getStored()
	{
		return get(STORED);
	}

	public int getCapacity()
	{
		return get(CAPACITY);
	}

	public int getCost()
	{
		return get(COST);
	}

	public int getNetworkStored()
	{
		return get(NETWORK_STORED);
	}

	public int getNetworkCapacity()
	{
		return get(NETWORK_CAPACITY);
	}

	public boolean isEnabled()
	{
		return (get(FLAGS)&ENABLED)!=0;
	}

	public boolean isPowered()
	{
		return (get(FLAGS)&POWERED)!=0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getBlockEntity(pos) instanceof EnergyAccepterBlockEntity
				&&player.canReach(pos, 4.0);
	}
}
