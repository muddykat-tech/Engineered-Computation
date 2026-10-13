package com.muddykat.engineeredcomputation.era;

import java.util.function.Supplier;

import com.muddykat.engineeredcomputation.multiblock.shapes.CraftingComputerShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DataProcessorShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.GenericShape;

public enum EraMachine
{
	CRAFTING_COMPUTER(ComputerEra.FLOPPY, "era5_crafting_computer", () -> CraftingComputerShape.GETTER),
	DATA_PROCESSOR(ComputerEra.FLOPPY, "era5_data_processor", () -> DataProcessorShape.GETTER);

	private final ComputerEra era;
	private final String name;
	private final Supplier<GenericShape> shape;

	EraMachine(ComputerEra era, String name, Supplier<GenericShape> shape)
	{
		this.era = era;
		this.name = name;
		this.shape = shape;
	}

	public ComputerEra era()
	{
		return era;
	}

	public String blockName()
	{
		return name;
	}

	public GenericShape shape()
	{
		return shape.get();
	}

	public EraDriveGeometry geometry()
	{
		return shape().geometry();
	}
}
