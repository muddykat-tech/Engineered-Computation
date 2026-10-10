package com.muddykat.engineeredcomputation.era;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EraDrive implements StringRepresentable
{
	PUNCHCARD_VAULT(ComputerEra.PUNCHCARD, "era1_drive", 4, false),
	DRUM_COMPUTER(ComputerEra.DRUM, "era2_drive", 1, true),
	DRUM_BANK(ComputerEra.DRUM, "era2_drum_bank", 2, false),
	TAPE_DRIVE(ComputerEra.TAPE, "era3_drive", 1, false),
	DISK_MAINFRAME(ComputerEra.DISK, "era4_drive", 1, true),
	DISK_BANK(ComputerEra.DISK, "era4_disk_bank", 1, false),
	FLOPPY_WORKSTATION(ComputerEra.FLOPPY, "era5_drive", 2, true),
	FLOPPY_BANK(ComputerEra.FLOPPY, "era5_floppy_bank", 4, false);

	public static final Codec<EraDrive> CODEC = StringRepresentable.fromEnum(EraDrive::values, EraDrive::byName);

	@Nullable
	public static EraDrive byName(String name)
	{
		for(EraDrive value : values())
			if(value.getSerializedName().equals(name))
				return value;
		return null;
	}

	private final ComputerEra era;
	private final String name;
	private final int slots;
	private final boolean processor;
	private EraDriveGeometry geometry;

	EraDrive(ComputerEra era, String name, int slots, boolean processor)
	{
		this.era = era;
		this.name = name;
		this.slots = slots;
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

	public EraDriveGeometry geometry()
	{
		if(geometry==null)
			geometry = EraModelGeometry.drive(this);
		return geometry;
	}

	public static List<EraDrive> of(ComputerEra era)
	{
		return Arrays.stream(values()).filter(drive -> drive.era==era).toList();
	}
}
