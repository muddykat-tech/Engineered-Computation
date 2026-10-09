package com.muddykat.engineeredcomputation.multiblock.core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;

public interface MultiblockState
{
	void save(CompoundTag tag);

	void load(CompoundTag tag);

	default void writeSyncData(CompoundTag tag)
	{
	}

	default void tick(MultiblockContext ctx)
	{
	}

	default InteractionResult use(MultiblockContext ctx, Player player, BlockPos posInMultiblock, Vec3 hit)
	{
		return InteractionResult.PASS;
	}

	default void onDisassemble(MultiblockContext ctx)
	{
	}

	@Nullable
	default IItemHandler getItemHandler(BlockPos posInMultiblock, @Nullable Direction side)
	{
		return null;
	}
}
