package com.muddykat.engineeredcomputation.storage;

import java.util.List;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;

public interface StorageHost
{
	ComputerEra getEra();

	default boolean isProcessor()
	{
		return false;
	}

	default boolean needsProcessor()
	{
		return getEra().requiresProcessor()&&!isProcessor();
	}

	List<CellInventory> getCells();
}
