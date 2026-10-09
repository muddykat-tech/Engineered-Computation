package com.muddykat.engineeredcomputation.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block that can be part of a storage network. Cables have no block entity, so this is asked of the block. */
public interface NetworkMemberBlock
{
	/** What joins the network here through {@code side}; with no side, whether anything here is a member at all. */
	@Nullable
	NetworkMember getNetworkMember(Level level, BlockPos pos, BlockState state, @Nullable Direction side);

	@Nullable
	static NetworkMember at(Level level, BlockPos pos, @Nullable Direction side)
	{
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof NetworkMemberBlock block? block.getNetworkMember(level, pos, state, side)
				: null;
	}
}
