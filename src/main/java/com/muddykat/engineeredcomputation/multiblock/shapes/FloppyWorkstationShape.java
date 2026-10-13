package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class FloppyWorkstationShape extends GenericShape
{
	public static final FloppyWorkstationShape GETTER = new FloppyWorkstationShape();

	private FloppyWorkstationShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(3, 4, 2),
				new BlockPos(1, 0, 0),
				new BlockPos(1, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 2, 14, 2, 16), box(0, 2, 2, 15, 16, 16), box(5, 5, 0, 11, 11, 2))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 14, 2, 12), box(0, 2, 0, 15, 16, 13))),
						Map.entry(new BlockPos(0, 1, 0), box(0, 0, 2, 15, 16, 16)),
						Map.entry(new BlockPos(0, 1, 1), box(0, 0, 0, 15, 16, 13)),
						Map.entry(new BlockPos(0, 2, 0), box(0, 0, 2, 15, 16, 16)),
						Map.entry(new BlockPos(0, 2, 1), box(0, 0, 0, 15, 16, 13)),
						Map.entry(new BlockPos(0, 3, 0), box(0, 0, 2, 15, 16, 16)),
						Map.entry(new BlockPos(0, 3, 1), box(0, 0, 0, 15, 16, 13)),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 13, 1, 16, 15, 16), box(1, 0, 3, 12, 13, 16), box(3, 15, 3, 16, 16, 16), box(12, 4, 4, 16, 13, 5), box(12, 4, 5, 16, 13, 6))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 13, 0, 16, 15, 15), box(1, 0, 0, 12, 13, 13), box(3, 15, 0, 16, 16, 13))),
						Map.entry(new BlockPos(1, 1, 0), box(3, 0, 3, 16, 16, 16)),
						Map.entry(new BlockPos(1, 1, 1), box(3, 0, 0, 16, 16, 13)),
						Map.entry(new BlockPos(1, 2, 0), box(3, 0, 3, 16, 12, 16)),
						Map.entry(new BlockPos(1, 2, 1), box(3, 0, 0, 16, 12, 13)),
						Map.entry(new BlockPos(2, 0, 0), shape(box(0, 4, 4, 12, 13, 5), box(0, 4, 5, 12, 13, 6), box(0, 13, 1, 16, 15, 16), box(0, 15, 3, 13, 16, 16), box(12, 0, 3, 15, 13, 16))),
						Map.entry(new BlockPos(2, 0, 1), shape(box(0, 13, 0, 16, 15, 15), box(0, 15, 0, 13, 16, 13), box(12, 0, 0, 15, 13, 13))),
						Map.entry(new BlockPos(2, 1, 0), box(0, 0, 3, 13, 16, 16)),
						Map.entry(new BlockPos(2, 1, 1), box(0, 0, 0, 13, 16, 13)),
						Map.entry(new BlockPos(2, 2, 0), box(0, 0, 3, 13, 12, 16)),
						Map.entry(new BlockPos(2, 2, 1), box(0, 0, 0, 13, 12, 13))
				),
				List.of(zone(16, 13, 1, 48, 15, 31), zone(19, 15, 3, 45, 44, 29), zone(20.5, 22.0308, 17.9908, 43.5, 43.0385, 20.8062)),
				List.of(zone(17, 0, 3, 28, 13, 29)),
				List.of(zone(1, 38, 25, 14, 45, 29), zone(1, 47, 25, 14, 54, 29)),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
