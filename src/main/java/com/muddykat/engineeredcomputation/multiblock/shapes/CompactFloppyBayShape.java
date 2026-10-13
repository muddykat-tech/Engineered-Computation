package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class CompactFloppyBayShape extends GenericShape
{
	public static final CompactFloppyBayShape GETTER = new CompactFloppyBayShape();

	private CompactFloppyBayShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(1, 2, 1),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 0),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 1, 0, 16, 16, 14), box(1, 0, 1, 15, 1, 13), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(0, 0, 0, 16, 14, 14), box(0, 14, 0, 16, 16, 14)))
				),
				List.of(),
				List.of(),
				List.of(zone(1, 23, 14, 7, 27, 16), zone(9, 23, 14, 15, 27, 16), zone(1, 17, 14, 7, 21, 16), zone(9, 17, 14, 15, 21, 16)),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
