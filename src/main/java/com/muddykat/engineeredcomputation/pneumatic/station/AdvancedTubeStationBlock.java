package com.muddykat.engineeredcomputation.pneumatic.station;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.DirectionalBlock;

public class AdvancedTubeStationBlock extends TubeStationBlock
{
	public static final MapCodec<AdvancedTubeStationBlock> CODEC = simpleCodec(AdvancedTubeStationBlock::new);

	public AdvancedTubeStationBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec()
	{
		return CODEC;
	}

	@Override
	public boolean filters()
	{
		return true;
	}
}
