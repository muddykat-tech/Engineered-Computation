package com.muddykat.engineeredcomputation.network;

import org.jetbrains.annotations.Nullable;

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
}
