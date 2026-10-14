package com.muddykat.engineeredcomputation.client.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Only a client level's model data manager supplies block entities' model data; Ponder's level doesn't. */
public final class BlockEntityModelData
{
	private BlockEntityModelData()
	{
	}

	public static ModelData orFromBlockEntity(BlockAndTintGetter level, BlockPos pos, ModelData data,
			ModelProperty<?> needed)
	{
		if(data.has(needed))
			return data;
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity==null? data: blockEntity.getModelData();
	}
}
