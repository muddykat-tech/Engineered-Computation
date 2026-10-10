package com.muddykat.engineeredcomputation.pneumatic;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.DirectionalBlock;

/**
 * A Tube Station with a slot for a Filter Card or Program Deck, whose filter (an allow or a block list) decides every
 * deposit made with it - see StationFilter. Without one it's a plain Tube Station.
 */
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
