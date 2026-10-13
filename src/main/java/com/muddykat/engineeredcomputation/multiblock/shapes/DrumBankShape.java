package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class DrumBankShape extends GenericShape
{
	public static final DrumBankShape GETTER = new DrumBankShape();

	private DrumBankShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(4, 3, 2),
				new BlockPos(1, 0, 0),
				new BlockPos(1, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 0, 16, 3, 16), box(0, 3, 0, 12, 16, 16), box(12, 3, 0, 16, 16, 16))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 16, 3, 15), box(0, 3, 0, 12, 16, 15), box(12, 3, 0, 16, 16, 14))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(0, 0, 0, 12, 16, 16), box(12, 0, 0, 16, 2, 16), box(12, 2, 0, 16, 16, 16))),
						Map.entry(new BlockPos(0, 1, 1), shape(box(0, 0, 0, 12, 16, 15), box(12, 0, 0, 16, 2, 14), box(12, 2, 0, 16, 16, 14), box(15, 1, 14, 16, 2, 15), box(15, 2, 14, 16, 16, 15))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 0, 0, 12, 16, 16), box(12, 0, 0, 16, 5, 16), box(12, 5, 0, 16, 16, 16))),
						Map.entry(new BlockPos(0, 2, 1), shape(box(0, 0, 0, 12, 16, 15), box(12, 0, 0, 16, 5, 14), box(12, 5, 0, 16, 16, 14), box(15, 0, 14, 16, 5, 15), box(15, 5, 14, 16, 6, 15))),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 0, 16, 3, 16), box(0, 3, 0, 16, 16, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 16, 3, 15), box(0, 3, 0, 16, 16, 14))),
						Map.entry(new BlockPos(1, 1, 0), shape(box(0, 0, 0, 16, 2, 16), box(0, 2, 0, 16, 16, 1), box(0, 2, 13, 16, 16, 14), box(2, 6, 14, 16, 8, 15), box(2, 15, 14, 16, 16, 15))),
						Map.entry(new BlockPos(1, 1, 1), shape(box(0, 0, 0, 16, 2, 14), box(0, 1, 14, 16, 2, 15))),
						Map.entry(new BlockPos(1, 2, 0), shape(box(0, 0, 0, 16, 5, 1), box(0, 0, 13, 16, 5, 14), box(0, 5, 0, 16, 16, 16), box(2, 0, 14, 16, 1, 15))),
						Map.entry(new BlockPos(1, 2, 1), shape(box(0, 5, 0, 16, 16, 14), box(0, 5, 14, 16, 6, 15))),
						Map.entry(new BlockPos(2, 0, 0), shape(box(0, 0, 0, 16, 3, 16), box(0, 3, 0, 16, 16, 16))),
						Map.entry(new BlockPos(2, 0, 1), shape(box(0, 0, 0, 16, 3, 15), box(0, 3, 0, 16, 16, 14))),
						Map.entry(new BlockPos(2, 1, 0), shape(box(0, 0, 0, 16, 2, 16), box(0, 2, 0, 16, 16, 1), box(0, 2, 13, 16, 16, 14), box(0, 6, 14, 14, 8, 15), box(0, 15, 14, 14, 16, 15))),
						Map.entry(new BlockPos(2, 1, 1), shape(box(0, 0, 0, 16, 2, 14), box(0, 1, 14, 16, 2, 15))),
						Map.entry(new BlockPos(2, 2, 0), shape(box(0, 0, 0, 16, 5, 1), box(0, 0, 13, 16, 5, 14), box(0, 0, 14, 14, 1, 15), box(0, 5, 0, 16, 16, 16))),
						Map.entry(new BlockPos(2, 2, 1), shape(box(0, 5, 0, 16, 16, 14), box(0, 5, 14, 16, 6, 15))),
						Map.entry(new BlockPos(3, 0, 0), shape(box(0, 0, 0, 16, 3, 16), box(0, 3, 0, 4, 16, 16), box(4, 3, 0, 16, 16, 16))),
						Map.entry(new BlockPos(3, 0, 1), shape(box(0, 0, 0, 16, 3, 15), box(0, 3, 0, 4, 16, 14), box(4, 3, 0, 16, 16, 15))),
						Map.entry(new BlockPos(3, 1, 0), shape(box(0, 0, 0, 4, 2, 16), box(0, 2, 0, 4, 16, 16), box(4, 0, 0, 16, 16, 16))),
						Map.entry(new BlockPos(3, 1, 1), shape(box(0, 0, 0, 4, 2, 14), box(0, 1, 14, 1, 2, 15), box(0, 2, 0, 4, 16, 14), box(0, 2, 14, 1, 16, 15), box(4, 0, 0, 16, 16, 15))),
						Map.entry(new BlockPos(3, 2, 0), shape(box(0, 0, 0, 4, 5, 16), box(0, 5, 0, 4, 16, 16), box(4, 0, 0, 16, 16, 16))),
						Map.entry(new BlockPos(3, 2, 1), shape(box(0, 0, 0, 4, 5, 14), box(0, 0, 14, 1, 5, 15), box(0, 5, 0, 4, 16, 14), box(0, 5, 14, 1, 6, 15), box(4, 0, 0, 16, 16, 15)))
				),
				List.of(),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(1, 0, 0), Direction.NORTH))
		));
	}
}
