package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class PunchcardVaultShape extends GenericShape
{
	public static final PunchcardVaultShape GETTER = new PunchcardVaultShape();

	private PunchcardVaultShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(3, 3, 2),
				new BlockPos(1, 0, 0),
				new BlockPos(1, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 0, 16, 1, 16), box(0, 1, 0, 16, 13, 16), box(0, 13, 0, 16, 14, 16), box(1, 14, 1, 16, 16, 12), box(2, 14, 13, 16, 16, 16))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 16, 1, 14), box(0, 1, 0, 16, 13, 14), box(0, 13, 0, 16, 14, 15), box(2, 14, 0, 16, 16, 9), box(15, 14, 9, 16, 16, 14))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(1, 0, 1, 16, 16, 12), box(2, 0, 13, 16, 3, 16), box(3, 3, 15, 11, 6, 16), box(4, 8, 12, 16, 16, 13))),
						Map.entry(new BlockPos(0, 1, 1), shape(box(2, 0, 0, 16, 3, 9), box(3, 3, 0, 11, 6, 7), box(4, 4, 0, 10, 8, 6), box(5, 8, 1, 9, 9, 5), box(12, 3, 0, 16, 6, 6), box(13, 6, 0, 16, 11, 4))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 13, 0, 16, 15, 13), box(1, 0, 1, 16, 13, 12), box(2, 6, 12, 16, 13, 13), box(4, 0, 12, 16, 5, 13))),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 0, 16, 1, 16), box(0, 1, 0, 16, 13, 16), box(0, 13, 0, 16, 14, 16), box(0, 14, 1, 15, 16, 12), box(0, 14, 13, 14, 16, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 16, 1, 14), box(0, 1, 0, 16, 13, 14), box(0, 13, 0, 16, 14, 15), box(0, 14, 0, 14, 16, 9), box(0, 14, 9, 13, 16, 14))),
						Map.entry(new BlockPos(1, 1, 0), shape(box(0, 0, 1, 15, 16, 12), box(0, 0, 13, 14, 3, 16), box(0, 8, 12, 12, 16, 13), box(5, 3, 15, 13, 5, 16))),
						Map.entry(new BlockPos(1, 1, 1), shape(box(0, 0, 0, 14, 3, 9), box(0, 3, 0, 4, 6, 6), box(0, 6, 0, 3, 11, 4), box(5, 3, 0, 13, 5, 7), box(6, 4, 0, 12, 6, 6))),
						Map.entry(new BlockPos(1, 2, 0), shape(box(0, 0, 1, 15, 13, 12), box(0, 0, 12, 12, 5, 13), box(0, 6, 12, 14, 13, 13), box(0, 13, 0, 16, 15, 13))),
						Map.entry(new BlockPos(2, 0, 0), shape(box(0, 0, 1, 2, 10, 3), box(0, 2, 3, 2, 3, 16), box(0, 10, 1, 16, 11, 16), box(0, 11, 1, 2, 16, 9), box(0, 11, 9, 2, 16, 15), box(0, 11, 15, 2, 16, 16), box(2, 2, 15, 14, 3, 16), box(2, 11, 1, 14, 16, 3), box(2, 11, 3, 14, 14, 16), box(2, 11, 3, 14, 16, 15), box(14, 0, 1, 16, 10, 3), box(14, 2, 3, 16, 3, 16), box(14, 11, 1, 16, 16, 9), box(14, 11, 9, 16, 16, 15), box(14, 11, 15, 16, 16, 16))),
						Map.entry(new BlockPos(2, 0, 1), shape(box(0, 0, 11, 2, 10, 13), box(0, 2, 0, 2, 3, 11), box(0, 10, 0, 16, 11, 13), box(0, 11, 0, 2, 16, 5), box(0, 11, 5, 2, 15, 13), box(2, 2, 0, 14, 3, 1), box(2, 11, 0, 14, 14, 5), box(2, 11, 11, 14, 14, 13), box(14, 0, 11, 16, 10, 13), box(14, 2, 0, 16, 3, 11), box(14, 11, 0, 16, 16, 5), box(14, 11, 5, 16, 15, 13))),
						Map.entry(new BlockPos(2, 1, 0), shape(box(0, 0, 1, 2, 8, 9), box(0, 0, 9, 2, 5, 15), box(0, 0, 15, 2, 2, 16), box(2, 0, 1, 14, 10, 3), box(2, 0, 3, 14, 1, 15), box(2, 0, 3, 14, 4, 9), box(14, 0, 1, 16, 8, 9), box(14, 0, 9, 16, 5, 15), box(14, 0, 15, 16, 2, 16))),
						Map.entry(new BlockPos(2, 1, 1), shape(box(0, 0, 0, 2, 2, 5), box(14, 0, 0, 16, 2, 5)))
				),
				List.of(),
				List.of(),
				List.of(zone(34, 11, 21, 46, 18, 27), zone(34, 14, 15, 46, 21, 21), zone(34, 17, 9, 46, 24, 15), zone(34, 20, 3, 46, 27, 9)),
				List.of(new NetworkPort(new BlockPos(1, 0, 0), Direction.NORTH))
		));
	}
}
