package com.muddykat.engineeredcomputation.era;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class EraModelGeometry
{
	private EraModelGeometry()
	{
	}

	static EraDriveGeometry drive(EraDrive drive)
	{
		return switch(drive)
		{
			case PUNCHCARD_VAULT -> new EraDriveGeometry(
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
			);
			case DRUM_COMPUTER -> new EraDriveGeometry(
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
			);
			case DRUM_BANK -> new EraDriveGeometry(
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
			);
			case TAPE_DRIVE -> new EraDriveGeometry(
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
			);
			case DISK_MAINFRAME -> new EraDriveGeometry(
					new BlockPos(3, 4, 3),
					new BlockPos(1, 0, 1),
					new BlockPos(1, 0, 2),
					Map.ofEntries(
							Map.entry(new BlockPos(0, 0, 0), shape(box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 16, 16))),
							Map.entry(new BlockPos(0, 0, 1), shape(box(0, 0, 0, 16, 4, 8), box(0, 2, 12, 16, 16, 16), box(0, 4, 0, 16, 16, 9), box(1, 0, 13, 16, 2, 16))),
							Map.entry(new BlockPos(0, 0, 2), shape(box(0, 2, 0, 16, 16, 15), box(1, 0, 0, 16, 2, 14))),
							Map.entry(new BlockPos(0, 1, 0), box(0, 0, 0, 16, 16, 16)),
							Map.entry(new BlockPos(0, 1, 1), shape(box(0, 0, 0, 16, 16, 9), box(0, 0, 12, 16, 2, 16), box(1, 2, 11, 16, 16, 12))),
							Map.entry(new BlockPos(0, 1, 2), shape(box(0, 0, 0, 16, 2, 15), box(7, 4, 1, 15, 6, 10), box(7, 6, 1, 15, 8, 10), box(8, 2, 3, 14, 4, 8), box(8, 8, 3, 14, 10, 8))),
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
			);
			case DISK_BANK -> new EraDriveGeometry(
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
			);
			case FLOPPY_WORKSTATION -> new EraDriveGeometry(
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
			);
			case FLOPPY_BANK -> new EraDriveGeometry(
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
			);
		};
	}

	static EraDriveGeometry machine(EraMachine machine)
	{
		return switch(machine)
		{
			case CRAFTING_COMPUTER -> new EraDriveGeometry(
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
			);
			case DATA_PROCESSOR -> new EraDriveGeometry(
					new BlockPos(1, 3, 1),
					new BlockPos(0, 0, 0),
					new BlockPos(0, 0, 0),
					Map.ofEntries(
							Map.entry(new BlockPos(0, 0, 0), shape(box(0, 1, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 15), box(1, 0, 1, 15, 1, 15), box(5, 5, 0, 11, 11, 1))),
							Map.entry(new BlockPos(0, 1, 0), shape(box(0, 0, 0, 16, 15, 16), box(0, 15, 0, 16, 16, 15))),
							Map.entry(new BlockPos(0, 2, 0), shape(box(0, 0, 0, 16, 14, 16), box(0, 14, 0, 16, 16, 16)))
					),
					List.of(),
					List.of(),
					List.of(),
					List.of(new NetworkPort(new BlockPos(0, 0, 0), Direction.NORTH))
			);
		};
	}

	static VoxelShape terminalNorth(ComputerEra era)
	{
		return switch(era)
		{
			case PUNCHCARD -> box(0, 0, 1, 16, 16, 15);
			case DRUM -> box(1, 0, 2, 15, 15, 15);
			case TAPE -> box(0, 0, 0, 16, 16, 15);
			case DISK -> box(1, 0, 0, 15, 12, 16);
			case FLOPPY -> box(0, 0, 1, 16, 16, 15);
		};
	}

	private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2)
	{
		return Block.box(x1, y1, z1, x2, y2, z2);
	}

	private static VoxelShape shape(VoxelShape first, VoxelShape... rest)
	{
		return Shapes.or(first, rest);
	}

	private static AABB zone(double x1, double y1, double z1, double x2, double y2, double z2)
	{
		return new AABB(x1/16, y1/16, z1/16, x2/16, y2/16, z2/16);
	}
}
