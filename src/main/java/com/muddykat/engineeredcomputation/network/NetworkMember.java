package com.muddykat.engineeredcomputation.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.processor.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;
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

	@Nullable
	default StorageAnalyzerBlockEntity getAnalyzer()
	{
		return null;
	}

	default int powerCost(Level level, BlockPos pos)
	{
		return 0;
	}

	/** One-block machines are charged per block; a multiblock is charged once, since all its parts share one member. */
	default boolean chargedPerBlock()
	{
		return true;
	}

	default boolean isEnergyAccepter()
	{
		return false;
	}
}
