package com.muddykat.engineeredcomputation.era;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record NetworkPort(BlockPos pos, Direction side)
{
}
