package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block pneumatic tubes connect to: a tube that carries carriers on, or an end where they stop. */
public interface TubeMemberBlock
{
	/** How carriers meet this block through {@code side}; with no side, whether it's part of tube networks at all. */
	@Nullable
	TubeMember getTubeMember(Level level, BlockPos pos, BlockState state, @Nullable Direction side);

	@Nullable
	static TubeMember at(Level level, BlockPos pos, @Nullable Direction side)
	{
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof TubeMemberBlock block? block.getTubeMember(level, pos, state, side): null;
	}
}
