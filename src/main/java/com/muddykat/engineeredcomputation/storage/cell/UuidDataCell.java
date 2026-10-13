package com.muddykat.engineeredcomputation.storage.cell;

import com.muddykat.engineeredcomputation.era.ComputerEra;

/** Holds only an id on the stack; the contents live in {@link CellStorage}. */
public class UuidDataCell extends EraDataCell
{
	public UuidDataCell(Properties properties, ComputerEra era, int totalBytes, int bytesPerType, int totalTypes)
	{
		super(properties, era, totalBytes, bytesPerType, totalTypes);
	}
}
