package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.NetworkPort;

public final class CraftingComputerShape extends GenericShape
{
	public static final CraftingComputerShape GETTER = new CraftingComputerShape();

	private CraftingComputerShape()
	{
		super(new EraDriveGeometry(
				new BlockPos(3, 3, 2),
				new BlockPos(2, 0, 0),
				new BlockPos(2, 0, 1),
				Map.ofEntries(
						Map.entry(new BlockPos(0, 0, 0), shape(box(1, 0, 1, 16, 3, 16), box(1, 3, 1, 15, 16, 16), box(15, 3, 1, 16, 16, 16))),
						Map.entry(new BlockPos(0, 0, 1), shape(box(1, 0, 0, 16, 3, 15), box(1, 3, 0, 15, 16, 14), box(15, 3, 0, 16, 16, 15))),
						Map.entry(new BlockPos(0, 1, 0), shape(box(1, 0, 1, 15, 16, 16), box(15, 0, 1, 16, 16, 16))),
						Map.entry(new BlockPos(0, 1, 1), shape(box(1, 0, 0, 15, 16, 14), box(15, 0, 0, 16, 16, 15))),
						Map.entry(new BlockPos(0, 2, 0), shape(box(0, 14, 0, 16, 16, 16), box(1, 0, 1, 15, 14, 16), box(15, 0, 1, 16, 14, 16))),
						Map.entry(new BlockPos(0, 2, 1), shape(box(0, 14, 0, 16, 16, 16), box(1, 0, 0, 15, 14, 14), box(15, 0, 0, 16, 14, 15))),
						Map.entry(new BlockPos(1, 0, 0), shape(box(0, 0, 1, 16, 3, 16), box(0, 3, 1, 16, 16, 16))),
						Map.entry(new BlockPos(1, 0, 1), shape(box(0, 0, 0, 16, 3, 15), box(0, 3, 0, 16, 16, 15))),
						Map.entry(new BlockPos(1, 1, 0), box(0, 0, 1, 16, 16, 16)),
						Map.entry(new BlockPos(1, 1, 1), box(0, 0, 0, 16, 16, 15)),
						Map.entry(new BlockPos(1, 2, 0), shape(box(0, 0, 1, 16, 14, 16), box(0, 14, 0, 16, 16, 16))),
						Map.entry(new BlockPos(1, 2, 1), shape(box(0, 0, 0, 16, 14, 15), box(0, 14, 0, 16, 16, 16))),
						Map.entry(new BlockPos(2, 0, 0), shape(box(0, 0, 1, 15, 3, 16), box(0, 3, 1, 1, 16, 16), box(1, 3, 1, 15, 16, 16), box(5, 5, 0, 11, 11, 1))),
						Map.entry(new BlockPos(2, 0, 1), shape(box(0, 0, 0, 15, 3, 15), box(0, 3, 0, 1, 16, 15), box(1, 3, 0, 15, 16, 14))),
						Map.entry(new BlockPos(2, 1, 0), shape(box(0, 0, 1, 1, 16, 16), box(1, 0, 1, 15, 16, 16))),
						Map.entry(new BlockPos(2, 1, 1), shape(box(0, 0, 0, 1, 16, 15), box(1, 0, 0, 15, 16, 14))),
						Map.entry(new BlockPos(2, 2, 0), shape(box(0, 0, 1, 1, 14, 16), box(0, 14, 0, 16, 16, 16), box(1, 0, 1, 15, 14, 16))),
						Map.entry(new BlockPos(2, 2, 1), shape(box(0, 0, 0, 1, 14, 15), box(0, 14, 0, 16, 16, 16), box(1, 0, 0, 15, 14, 14)))
				),
				List.of(),
				List.of(),
				List.of(zone(2, 36, 29, 14, 43, 32), zone(2, 26, 29, 14, 33, 32), zone(2, 16, 29, 14, 23, 32), zone(2, 6, 29, 14, 13, 32), zone(34, 36, 29, 46, 43, 32), zone(34, 26, 29, 46, 33, 32), zone(34, 16, 29, 46, 23, 32), zone(34, 6, 29, 46, 13, 32)),
				List.of(new NetworkPort(new BlockPos(2, 0, 0), Direction.NORTH))
		));
	}
}
