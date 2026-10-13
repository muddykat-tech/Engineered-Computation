package com.muddykat.engineeredcomputation.era;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.shapes.TerminalShape;

public enum ComputerEra implements StringRepresentable
{
	PUNCHCARD("era1", "punchcard", "relay", true),
	DRUM("era2", "magnetic_drum", "vacuum_tube", true),
	TAPE("era3", "magnetic_tape", "transistor", true),
	DISK("era4", "disk_pack", "integrated_circuit", true),
	FLOPPY("era5", "floppy_disk", "microprocessor", true);

	public static final Codec<ComputerEra> CODEC = StringRepresentable.fromEnum(ComputerEra::values);

	private final String id;
	private final String memoryName;
	private final String componentName;
	private final boolean terminal;
	private Map<Direction, VoxelShape> terminalShapes;

	ComputerEra(String id, String memoryName, String componentName, boolean terminal)
	{
		this.id = id;
		this.memoryName = memoryName;
		this.componentName = componentName;
		this.terminal = terminal;
	}

	public boolean requiresProcessor()
	{
		return this==DRUM||this==DISK||this==FLOPPY;
	}

	public boolean onDataNetwork()
	{
		return this!=PUNCHCARD;
	}

	public boolean hasTerminal()
	{
		return terminal;
	}

	@Override
	public String getSerializedName()
	{
		return id;
	}

	public String memoryName()
	{
		return memoryName;
	}

	public String terminalName()
	{
		return id+"_terminal";
	}

	public String guideName()
	{
		return this==FLOPPY? "palm_pilot": "guide_edition_"+(ordinal()+1);
	}

	public String casingName()
	{
		return id+"_casing";
	}

	public String componentName()
	{
		return componentName;
	}

	public Component displayName()
	{
		return Component.translatable("era.engineered_computation."+id);
	}

	public Component memoryDisplayName()
	{
		return Component.translatable("era.engineered_computation."+id+".memory");
	}

	public List<EraDrive> drives()
	{
		return EraDrive.of(this);
	}

	public VoxelShape terminalShape(Direction facing)
	{
		if(terminalShapes==null)
		{
			VoxelShape north = TerminalShape.north(this);
			Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
			for(Direction direction : Direction.Plane.HORIZONTAL)
				shapes.put(direction, MultiblockOrientation.rotateShape(north, direction));
			terminalShapes = shapes;
		}
		return terminalShapes.get(facing);
	}
}
