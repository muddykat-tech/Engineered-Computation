package com.muddykat.engineeredcomputation.era;

public enum EraMachine
{
	CRAFTING_COMPUTER(ComputerEra.FLOPPY, "era5_crafting_computer"),
	DATA_PROCESSOR(ComputerEra.FLOPPY, "era5_data_processor");

	private final ComputerEra era;
	private final String name;
	private EraDriveGeometry geometry;

	EraMachine(ComputerEra era, String name)
	{
		this.era = era;
		this.name = name;
	}

	public ComputerEra era()
	{
		return era;
	}

	public String blockName()
	{
		return name;
	}

	public EraDriveGeometry geometry()
	{
		if(geometry==null)
			geometry = EraModelGeometry.machine(this);
		return geometry;
	}
}
