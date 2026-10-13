package com.muddykat.engineeredcomputation.era;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import com.muddykat.engineeredcomputation.multiblock.shapes.CompactFloppyBayShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DiskBankShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DiskMainframeShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DiskProcessorShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DrumBankShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.DrumComputerShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.FloppyBankShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.FloppyProcessorShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.FloppyWorkstationShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.GenericShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.PunchcardVaultShape;
import com.muddykat.engineeredcomputation.multiblock.shapes.TapeDriveShape;

public enum EraDrive implements StringRepresentable
{
	PUNCHCARD_VAULT(ComputerEra.PUNCHCARD, "era1_drive", 4, false, () -> PunchcardVaultShape.GETTER),
	DRUM_COMPUTER(ComputerEra.DRUM, "era2_drive", 1, true, () -> DrumComputerShape.GETTER),
	DRUM_BANK(ComputerEra.DRUM, "era2_drum_bank", 2, false, () -> DrumBankShape.GETTER),
	TAPE_DRIVE(ComputerEra.TAPE, "era3_drive", 1, false, () -> TapeDriveShape.GETTER),
	DISK_MAINFRAME(ComputerEra.DISK, "era4_drive", 1, true, () -> DiskMainframeShape.GETTER),
	DISK_BANK(ComputerEra.DISK, "era4_disk_bank", 1, false, () -> DiskBankShape.GETTER),
	FLOPPY_WORKSTATION(ComputerEra.FLOPPY, "era5_drive", 2, true, () -> FloppyWorkstationShape.GETTER),
	FLOPPY_BANK(ComputerEra.FLOPPY, "era5_floppy_bank", 4, false, () -> FloppyBankShape.GETTER),
	DISK_PROCESSOR(ComputerEra.DISK, "era4_disk_processor", 1, true, () -> DiskProcessorShape.GETTER),
	FLOPPY_PROCESSOR(ComputerEra.FLOPPY, "era5_floppy_processor", 3, 2, true, () -> FloppyProcessorShape.GETTER),
	COMPACT_FLOPPY_BAY(ComputerEra.FLOPPY, "era5_compact_floppy_bay", 4, 4, false, () -> CompactFloppyBayShape.GETTER);

	public static final Codec<EraDrive> CODEC = StringRepresentable.fromEnum(EraDrive::values);

	private final ComputerEra era;
	private final String name;
	private final int slots;
	private final int compactSlots;
	private final boolean processor;
	private final Supplier<GenericShape> shape;

	EraDrive(ComputerEra era, String name, int slots, boolean processor, Supplier<GenericShape> shape)
	{
		this(era, name, slots, 0, processor, shape);
	}

	EraDrive(ComputerEra era, String name, int slots, int compactSlots, boolean processor,
			Supplier<GenericShape> shape)
	{
		this.shape = shape;
		this.era = era;
		this.name = name;
		this.slots = slots;
		this.compactSlots = compactSlots;
		this.processor = processor;
	}

	public ComputerEra era()
	{
		return era;
	}

	public String blockName()
	{
		return name;
	}

	@Override
	public String getSerializedName()
	{
		return name;
	}

	public boolean isProcessor()
	{
		return processor;
	}

	public int slots()
	{
		return slots;
	}

	public int compactSlots()
	{
		return compactSlots;
	}

	public boolean isCompactSlot(int slot)
	{
		return slot>=slots-compactSlots;
	}

	public GenericShape shape()
	{
		return shape.get();
	}

	public EraDriveGeometry geometry()
	{
		return shape().geometry();
	}

	public static List<EraDrive> of(ComputerEra era)
	{
		return Arrays.stream(values()).filter(drive -> drive.era==era).toList();
	}
}
