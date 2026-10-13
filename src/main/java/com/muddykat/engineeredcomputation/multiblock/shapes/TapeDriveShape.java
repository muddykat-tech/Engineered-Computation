package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class TapeDriveShape extends GenericShape
{
	public static final TapeDriveShape GETTER = new TapeDriveShape();

	private TapeDriveShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(2, 5, 2),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 3, 0, 16, 16, 16), box(1, 0, 1, 16, 3, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 3, 0, 16, 16, 16), box(1, 0, 0, 16, 3, 14))),
						Map.entry(new BlockPos(0, 1, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 1, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 2, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 2, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 3, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 3, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 4, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(0, 4, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 1, 15, 3, 16), box(0, 3, 0, 16, 16, 16))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 15, 3, 14), box(0, 3, 0, 16, 16, 16))),
						Map.entry(new BlockPos(1, 1, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 1, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 2, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 2, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 3, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 3, 1), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 4, 0), box(0, 0, 0, 16, 16, 16)),
						Map.entry(new BlockPos(1, 4, 1), box(0, 0, 0, 16, 16, 16))
				),
				List.of(),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
