package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class DiskBankShape extends GenericShape
{
	public static final DiskBankShape GETTER = new DiskBankShape();

	private DiskBankShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(2, 2, 2),
				new BlockPos(0, 0, 0),
				new BlockPos(0, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(3, 0, 9, 16, 16, 16), box(5, 5, 0, 11, 11, 9))),
						Map.entry(new BlockPos(0, 0, 1), box(3, 0, 0, 16, 16, 12)),
						Map.entry(new BlockPos(0, 1, 0), box(3, 0, 9, 16, 2, 16)),
						Map.entry(new BlockPos(0, 1, 1), box(3, 0, 0, 16, 2, 12)),
						Map.entry(new BlockPos(1, 0, 0), box(0, 0, 9, 9, 16, 16)),
						Map.entry(new BlockPos(1, 0, 1), box(0, 0, 0, 9, 16, 12)),
						Map.entry(new BlockPos(1, 1, 0), box(0, 0, 9, 9, 2, 16)),
						Map.entry(new BlockPos(1, 1, 1), box(0, 0, 0, 9, 2, 12))
				),
				List.of(),
				List.of(),
				List.of(),
				List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
		));
	}
}
