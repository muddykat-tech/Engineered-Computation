package com.muddykat.engineeredcomputation.pneumatic.autotube;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

public class ObservingAutotubeBlock extends AutotubeBlock
{
	public static final MapCodec<ObservingAutotubeBlock> CODEC = simpleCodec(ObservingAutotubeBlock::new);

	public ObservingAutotubeBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	public boolean observes()
	{
		return true;
	}
}
