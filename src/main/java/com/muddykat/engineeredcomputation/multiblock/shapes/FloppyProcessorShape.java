package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class FloppyProcessorShape extends GenericShape
{
	public static final FloppyProcessorShape GETTER = new FloppyProcessorShape();

	private FloppyProcessorShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(1, 3, 2),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 2, 2, 16, 16, 16), box(1, 0, 3, 15, 2, 16), box(5, 5, 0, 11, 11, 2))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 2, 0, 16, 16, 10), box(0, 2, 10, 16, 16, 14), box(1, 0, 0, 15, 2, 13))),
						Map.entry(new BlockPos(0, 1, 0), box(0, 0, 2, 16, 16, 16)),
						Map.entry(new BlockPos(0, 1, 1), shape(box(0, 0, 0, 16, 16, 10), box(0, 0, 10, 16, 11, 14), box(0, 11, 10, 1, 16, 14), box(15, 11, 10, 16, 16, 14))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 0, 2, 16, 14, 16), box(0, 14, 2, 16, 16, 16))),
						Map.entry(new BlockPos(0, 2, 1), shape(box(0, 0, 0, 16, 14, 10), box(0, 0, 10, 1, 2, 14), box(0, 2, 10, 16, 14, 14), box(0, 14, 0, 16, 16, 14), box(15, 0, 10, 16, 2, 14)))
				),
				List.of(),
				List.of(),
				List.of(zone(1, 27, 26, 15, 34, 30), zone(1, 20, 30, 7, 24, 32), zone(9, 20, 30, 15, 24, 32)),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
