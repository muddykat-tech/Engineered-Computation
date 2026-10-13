package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class DataProcessorShape extends GenericShape
{
	public static final DataProcessorShape GETTER = new DataProcessorShape();

	private DataProcessorShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(1, 3, 1),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 0),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 1, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 15), box(1, 0, 1, 15, 1, 15), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 15))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 0, 0, 16, 14, 16), box(0, 14, 0, 16, 16, 16)))
				),
				List.of(),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
