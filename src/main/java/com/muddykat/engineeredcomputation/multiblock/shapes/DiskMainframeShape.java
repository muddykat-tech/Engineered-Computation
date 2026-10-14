package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class DiskMainframeShape extends GenericShape
{
	public static final DiskMainframeShape GETTER = new DiskMainframeShape();

	private DiskMainframeShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(3, 4, 3),
				new BlockPos(1, 0, 1),
				new BlockPos(1, 0, 2),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 16, 16))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 16, 4, 8), box(0, 2, 12, 16, 16, 16), box(0, 4, 0, 16, 16, 9), box(1, 0, 13, 16, 2, 16))),
						Map.entry(new BlockPos(0, 0, 2), shape(box(0, 2, 0, 16, 16, 15), box(1, 0, 0, 16, 2, 14))),
						Map.entry(new BlockPos(0, 1, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 1, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 12, 16, 2, 16), box(1, 2, 11, 16, 16, 12))),
						Map.entry(new BlockPos(0, 1, 2), box(0, 0, 0, 16, 2, 15)),
						Map.entry(new BlockPos(0, 2, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 2, 1), shape(box(0, 0, 0, 16, 16, 9), box(1, 0, 11, 16, 4, 12))),
						Map.entry(new BlockPos(0, 3, 0), box(0, 0, 0, 16, 12, 16)),
						Map.entry(new BlockPos(0, 3, 1), box(0, 0, 0, 16, 12, 9)),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 16, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 16, 4, 8), box(0, 0, 13, 5, 2, 16), box(0, 2, 12, 6, 16, 16), box(0, 4, 0, 16, 16, 9), box(9, 13, 12, 16, 16, 16), box(12, 0, 15, 16, 2, 16))),
						Map.entry(new BlockPos(1, 0, 2), shape(box(0, 0, 0, 5, 2, 14), box(0, 2, 0, 6, 16, 15), box(9, 13, 0, 16, 16, 14), box(12, 0, 0, 16, 2, 11))),
						Map.entry(new BlockPos(1, 1, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 1, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 12, 6, 2, 16), box(0, 2, 11, 5, 16, 12), box(9, 0, 12, 16, 2, 16), box(11, 2, 12, 16, 16, 16))),
						Map.entry(new BlockPos(1, 1, 2), shape(box(0, 0, 0, 6, 2, 15), box(9, 0, 0, 16, 2, 14), box(11, 2, 0, 16, 16, 7))),
						Map.entry(new BlockPos(1, 2, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 2, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 11, 5, 4, 12), box(11, 0, 12, 16, 2, 16))),
						Map.entry(new BlockPos(1, 2, 2), box(11, 0, 0, 16, 2, 7)),
						Map.entry(new BlockPos(1, 3, 0), box(0, 0, 0, 16, 12, 16)),
						Map.entry(new BlockPos(1, 3, 1), box(0, 0, 0, 16, 12, 9)),
						Map.entry(new BlockPos(2, 0, 0), shape(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 16, 16))),
						Map.entry(new BlockPos(2, 0, 1), shape(box(0, 0, 0, 16, 4, 8), box(0, 0, 15, 10, 2, 16), box(0, 4, 0, 16, 16, 9), box(0, 13, 12, 13, 16, 16))),
						Map.entry(new BlockPos(2, 0, 2), shape(box(0, 0, 0, 10, 2, 11), box(0, 2, 2, 6, 13, 8), box(0, 13, 0, 13, 16, 14))),
						Map.entry(new BlockPos(2, 1, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(2, 1, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 12, 13, 2, 16), box(0, 2, 12, 11, 16, 16))),
						Map.entry(new BlockPos(2, 1, 2), shape(box(0, 0, 0, 13, 2, 14), box(0, 2, 0, 11, 16, 7))),
						Map.entry(new BlockPos(2, 2, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(2, 2, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 12, 11, 2, 16))),
						Map.entry(new BlockPos(2, 2, 2), box(0, 0, 0, 11, 2, 7)),
						Map.entry(new BlockPos(2, 3, 0), box(0, 0, 0, 16, 12, 16)),
						Map.entry(new BlockPos(2, 3, 1), box(0, 0, 0, 16, 12, 9))
				),
				List.of(zone(25, 13, 28, 45, 18, 46), zone(27, 18, 28, 43, 34, 39)),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(1, 0, 0), Direction.NORTH))
		));
	}
}
