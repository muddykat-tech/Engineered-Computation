package com.muddykat.engineeredcomputation.multiblock.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record MultiblockContext(
		Level level, BlockPos masterPos, Direction facing, MultiblockDefinition<?> definition, Runnable markDirty)
{
	public BlockPos toAbsolute(BlockPos posInMultiblock)
	{
		return origin().offset(MultiblockOrientation.toWorldOffset(posInMultiblock, facing));
	}

	public Vec3 toTemplatePoint(Vec3 worldPoint)
	{
		return MultiblockOrientation.toTemplatePoint(worldPoint.subtract(Vec3.atLowerCornerOf(origin())), facing);
	}

	private BlockPos origin()
	{
		return masterPos.subtract(MultiblockOrientation.toWorldOffset(definition.masterPos(), facing));
	}

	public void setChanged()
	{
		markDirty.run();
	}
}
