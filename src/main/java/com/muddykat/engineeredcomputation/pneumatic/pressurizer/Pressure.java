package com.muddykat.engineeredcomputation.pneumatic.pressurizer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;

/** Autotubes don't use pressure, so their carriers are never slowed by it. */
public final class Pressure
{
	private Pressure()
	{
	}

	public static boolean high(Level level, TubeNetwork network)
	{
		if(!ECConfig.pressurizerRequired())
			return true;
		for(BlockPos end : network.getEnds())
			if(level.getBlockEntity(end) instanceof PressurizerBlockEntity pressurizer&&pressurizer.hasPressure())
				return true;
		return false;
	}

	public static boolean draw(Level level, TubeNetwork network)
	{
		if(!ECConfig.pressurizerRequired())
			return true;
		for(BlockPos end : network.getEnds())
			if(level.getBlockEntity(end) instanceof PressurizerBlockEntity pressurizer&&pressurizer.draw())
				return true;
		return false;
	}

	public static int carrierItems(boolean high, int maxStackSize)
	{
		return Math.min(maxStackSize, ECConfig.carrierItems(high));
	}

	public static int carrierTicks(boolean high)
	{
		return ECConfig.carrierTicks(high);
	}
}
