package com.muddykat.engineeredcomputation.multiblock.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MultiblockOrientation
{
	private MultiblockOrientation()
	{
	}

	public static Rotation rotationFor(Direction facing)
	{
		return switch(facing)
		{
			case EAST -> Rotation.CLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_180;
			case WEST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public static BlockPos toWorldOffset(BlockPos templatePos, Direction facing)
	{
		return StructureTemplate.calculateRelativePosition(
				new StructurePlaceSettings().setRotation(rotationFor(facing)), templatePos);
	}

	public static Vec3 toTemplatePoint(Vec3 worldOffset, Direction facing)
	{
		double x = worldOffset.x;
		double y = worldOffset.y;
		double z = worldOffset.z;
		return switch(facing)
		{
			case EAST -> new Vec3(z, y, 1-x);
			case SOUTH -> new Vec3(1-x, y, 1-z);
			case WEST -> new Vec3(1-z, y, x);
			default -> worldOffset;
		};
	}

	public static Direction toTemplateSide(Direction worldSide, Direction facing)
	{
		Rotation back = switch(facing)
		{
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_180;
			case WEST -> Rotation.CLOCKWISE_90;
			default -> Rotation.NONE;
		};
		return back.rotate(worldSide);
	}

	public static VoxelShape rotateShape(VoxelShape north, Direction facing)
	{
		if(facing==Direction.NORTH)
			return north;
		VoxelShape result = Shapes.empty();
		for(AABB box : north.toAabbs())
		{
			AABB rotated = switch(facing)
			{
				case EAST -> new AABB(1-box.maxZ, box.minY, box.minX, 1-box.minZ, box.maxY, box.maxX);
				case SOUTH -> new AABB(1-box.maxX, box.minY, 1-box.maxZ, 1-box.minX, box.maxY, 1-box.minZ);
				case WEST -> new AABB(box.minZ, box.minY, 1-box.maxX, box.maxZ, box.maxY, 1-box.minX);
				default -> box;
			};
			result = Shapes.or(result, Shapes.create(rotated));
		}
		return result;
	}
}
