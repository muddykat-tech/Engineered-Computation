package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class DrumComputerShape extends GenericShape
{
	public static final DrumComputerShape GETTER = new DrumComputerShape();

	private DrumComputerShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(2, 3, 2),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(1, 1, 0, 2, 15, 16), box(2, 0, 1, 16, 1, 16), box(2, 1, 0, 16, 15, 16), box(2, 15, 1, 16, 16, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(1, 1, 0, 2, 15, 14), box(2, 0, 0, 16, 1, 14), box(2, 1, 0, 16, 15, 15), box(2, 15, 0, 16, 16, 14))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(1, 0, 0, 2, 16, 16), box(2, 0, 0, 10, 16, 16), box(10, 0, 0, 16, 16, 16))),
						Map.entry(new BlockPos(0, 1, 1), shape(box(1, 0, 0, 2, 16, 14), box(2, 0, 0, 10, 16, 15), box(10, 0, 0, 16, 16, 15), box(10, 0, 15, 11, 16, 16), box(11, 0, 15, 16, 5, 16))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 13, 0, 16, 15, 16), box(1, 0, 0, 2, 13, 16), box(2, 0, 0, 10, 13, 16), box(10, 0, 0, 16, 13, 16))),
						Map.entry(new BlockPos(0, 2, 1), shape(box(0, 13, 0, 16, 15, 16), box(1, 0, 0, 2, 13, 14), box(2, 0, 0, 10, 13, 15), box(10, 0, 0, 16, 13, 15), box(10, 0, 15, 11, 13, 16), box(11, 2, 15, 16, 4, 16), box(11, 11, 15, 16, 13, 16), box(13, 5, 15, 16, 10, 16))),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 1, 14, 1, 16), box(0, 1, 0, 14, 15, 16), box(0, 15, 1, 14, 16, 16), box(14, 1, 0, 15, 15, 16))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 14, 1, 14), box(0, 1, 0, 14, 15, 15), box(0, 15, 0, 14, 16, 14), box(14, 1, 0, 15, 15, 14))),
						Map.entry(new BlockPos(1, 1, 0), shape(box(0, 0, 0, 6, 16, 16), box(6, 0, 0, 14, 16, 16), box(14, 0, 0, 15, 16, 16))),
						Map.entry(new BlockPos(1, 1, 1), shape(box(0, 0, 0, 6, 16, 15), box(0, 0, 15, 5, 5, 16), box(5, 0, 15, 6, 16, 16), box(6, 0, 0, 14, 16, 15), box(14, 0, 0, 15, 16, 14))),
						Map.entry(new BlockPos(1, 2, 0), shape(box(0, 0, 0, 6, 13, 16), box(0, 13, 0, 16, 15, 16), box(6, 0, 0, 14, 13, 16), box(14, 0, 0, 15, 13, 16))),
						Map.entry(new BlockPos(1, 2, 1), shape(box(0, 0, 0, 6, 13, 15), box(0, 2, 15, 5, 4, 16), box(0, 5, 15, 3, 10, 16), box(0, 11, 15, 5, 13, 16), box(0, 13, 0, 16, 15, 16), box(5, 0, 15, 6, 13, 16), box(6, 0, 0, 14, 13, 15), box(14, 0, 0, 15, 13, 14)))
				),
				List.of(),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
