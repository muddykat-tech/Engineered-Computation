package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class FloppyBankShape extends GenericShape
{
	public static final FloppyBankShape GETTER = new FloppyBankShape();

	private FloppyBankShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(1, 4, 2),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 2, 16, 2, 16), box(0, 2, 2, 16, 16, 16), box(5, 5, 0, 11, 11, 2))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 16, 2, 13), box(0, 2, 0, 16, 16, 14))),
						Map.entry(new BlockPos(0, 1, 0), box(0, 0, 2, 16, 16, 16)),
						Map.entry(new BlockPos(0, 1, 1), box(0, 0, 0, 16, 16, 14)),
						Map.entry(new BlockPos(0, 2, 0), box(0, 0, 2, 16, 16, 16)),
						Map.entry(new BlockPos(0, 2, 1), box(0, 0, 0, 16, 16, 14)),
						Map.entry(new BlockPos(0, 3, 0), box(0, 0, 2, 16, 16, 16)),
						Map.entry(new BlockPos(0, 3, 1), box(0, 0, 0, 16, 16, 14))
				),
				List.of(),
				List.of(),
				List.of(zone(1, 20, 26, 15, 27, 30), zone(1, 29, 26, 15, 36, 30), zone(1, 38, 26, 15, 45, 30), zone(1, 47, 26, 15, 54, 30)),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
