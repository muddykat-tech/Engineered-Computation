package com.muddykat.engineeredcomputation.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.crafting.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.storage.StorageHost;

public interface NetworkMember
{
	@Nullable
	default StorageHost getStorage()
	{
		return null;
	}

	@Nullable
	default ComputerEra getEra()
	{
		return null;
	}

	default boolean isBridge()
	{
		return false;
	}

	@Nullable
	default CraftingComputerState getCraftingComputer()
	{
		return null;
	}

	@Nullable
	default DataProcessorState getDataProcessor()
	{
		return null;
	}

	/** RF per tick this machine costs while power is enabled (see ECConfig), for the block at {@code pos}. */
	default int powerCost(Level level, BlockPos pos)
	{
		return 0;
	}

	/**
	 * Whether the cost is charged for every block with this member (a machine of one block), or once for the member
	 * itself: a multiblock's state, shared by all of its parts.
	 */
	default boolean chargedPerBlock()
	{
		return true;
	}

	/** Whether this is an Energy Accepter, which feeds the network RF. */
	default boolean isEnergyAccepter()
	{
		return false;
	}
}
