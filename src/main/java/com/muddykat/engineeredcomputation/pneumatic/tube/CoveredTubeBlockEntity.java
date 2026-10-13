package com.muddykat.engineeredcomputation.pneumatic.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class CoveredTubeBlockEntity extends CoveredCableBlockEntity
{
	public CoveredTubeBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.COVERED_TUBE_ENTITY.get(), pos, state);
	}
}
