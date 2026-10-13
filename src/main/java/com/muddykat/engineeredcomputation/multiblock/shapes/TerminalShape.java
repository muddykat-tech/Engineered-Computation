package com.muddykat.engineeredcomputation.multiblock.shapes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class TerminalShape
{
	private TerminalShape()
	{
	}

	public static VoxelShape north(ComputerEra era)
	{
		return switch(era)
		{
			case PUNCHCARD -> Block.box(0, 0, 1, 16, 16, 15);
			case DRUM -> Block.box(1, 0, 2, 15, 15, 15);
			case TAPE -> Block.box(0, 0, 0, 16, 16, 15);
			case DISK -> Block.box(1, 0, 0, 15, 12, 16);
			case FLOPPY -> Block.box(0, 0, 1, 16, 16, 15);
		};
	}
}
