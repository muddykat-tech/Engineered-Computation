package com.muddykat.engineeredcomputation.multiblock.shapes;

import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.era.EraDriveGeometry;

public abstract class GenericShape implements Function<BlockPos, VoxelShape>
{
	private final EraDriveGeometry geometry;

	protected GenericShape(EraDriveGeometry geometry)
	{
		this.geometry = geometry;
	}

	public EraDriveGeometry geometry()
	{
		return geometry;
	}

	@Override
	public VoxelShape apply(BlockPos posInMultiblock)
	{
		return geometry.shapeAt(posInMultiblock);
	}

	protected static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2)
	{
		return Block.box(x1, y1, z1, x2, y2, z2);
	}

	protected static VoxelShape shape(VoxelShape first, VoxelShape... rest)
	{
		return Shapes.or(first, rest);
	}

	protected static AABB zone(double x1, double y1, double z1, double x2, double y2, double z2)
	{
		return new AABB(x1/16, y1/16, z1/16, x2/16, y2/16, z2/16);
	}
}
