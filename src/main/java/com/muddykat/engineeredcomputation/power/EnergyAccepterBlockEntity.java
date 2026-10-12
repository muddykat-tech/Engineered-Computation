package com.muddykat.engineeredcomputation.power;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECPower;

public class EnergyAccepterBlockEntity extends BlockEntity implements MenuProvider
{
	private int energy;
	/** The last game tick this accepter paid for its network. */
	private long paidAt = Long.MIN_VALUE;

	private final IEnergyStorage storage = new IEnergyStorage()
	{
		@Override
		public int receiveEnergy(int toReceive, boolean simulate)
		{
			int taken = Math.max(0, Math.min(toReceive, capacity()-energy));
			if(!simulate&&taken>0)
			{
				energy += taken;
				setChanged();
			}
			return taken;
		}

		@Override
		public int extractEnergy(int toExtract, boolean simulate)
		{
			return 0;
		}

		@Override
		public int getEnergyStored()
		{
			return energy;
		}

		@Override
		public int getMaxEnergyStored()
		{
			return capacity();
		}

		@Override
		public boolean canExtract()
		{
			return false;
		}

		@Override
		public boolean canReceive()
		{
			return true;
		}
	};
	private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> storage);

	public EnergyAccepterBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPower.BLOCK_ENTITY.get(), pos, state);
	}

	public static int capacity()
	{
		return ECConfig.accepterCapacity();
	}

	public IEnergyStorage getEnergyStorage()
	{
		return storage;
	}

	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side)
	{
		if(capability==CapabilityEnergy.ENERGY)
			return energyCapability.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps()
	{
		super.invalidateCaps();
		energyCapability.invalidate();
	}

	@Override
	public void reviveCaps()
	{
		super.reviveCaps();
		energyCapability = LazyOptional.of(() -> storage);
	}

	public int getEnergy()
	{
		return energy;
	}

	public void setEnergy(int energy)
	{
		this.energy = Math.max(0, Math.min(energy, capacity()));
		setChanged();
	}

	/** Whether it paid for its network this tick or the last (machines check before or after it ticks). */
	public boolean paidRecently(long gameTime)
	{
		return paidAt>=gameTime-1;
	}

	void tickServer()
	{
		if(level==null)
			return;
		if(energy>capacity())
			setEnergy(capacity());
		StorageNetwork network = StorageNetwork.of(level, worldPosition);
		if(!ECConfig.powerEnabled()||!worldPosition.equals(network.getPayingAccepter()))
			return;
		long cost = network.getPowerCost(level);
		List<EnergyAccepterBlockEntity> accepters = accepters(level, network);
		if(stored(accepters)<cost)
			return;
		long left = cost;
		for(EnergyAccepterBlockEntity accepter : accepters)
			left -= accepter.draw(left);
		paidAt = level.getGameTime();
	}

	private int draw(long amount)
	{
		int taken = (int)Math.min(amount, energy);
		if(taken>0)
		{
			energy -= taken;
			setChanged();
		}
		return taken;
	}

	/** A network's Energy Accepters (loaded ones). */
	public static List<EnergyAccepterBlockEntity> accepters(Level level, StorageNetwork network)
	{
		List<EnergyAccepterBlockEntity> accepters = new ArrayList<>();
		for(BlockPos pos : network.getAccepters())
			if(level.getBlockEntity(pos) instanceof EnergyAccepterBlockEntity accepter)
				accepters.add(accepter);
		return accepters;
	}

	public static long stored(List<EnergyAccepterBlockEntity> accepters)
	{
		return accepters.stream().mapToLong(EnergyAccepterBlockEntity::getEnergy).sum();
	}

	@Override
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		tag.putInt("energy", energy);
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		energy = Math.max(0, tag.getInt("energy"));
	}

	@Override
	public Component getDisplayName()
	{
		return getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
	{
		return new EnergyAccepterMenu(id, inventory, this);
	}
}
