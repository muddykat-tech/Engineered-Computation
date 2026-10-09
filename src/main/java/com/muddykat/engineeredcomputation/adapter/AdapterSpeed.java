package com.muddykat.engineeredcomputation.adapter;

import java.util.Optional;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public record AdapterSpeed(int items, int ticks)
{
	public static final int SLOWEST_TICKS = 200;

	public static Optional<AdapterSpeed> of(ComputerEra era)
	{
		return switch(era)
		{
			case PUNCHCARD -> Optional.empty();
			case DRUM -> Optional.of(new AdapterSpeed(1, 10));
			case TAPE -> Optional.of(new AdapterSpeed(8, 5));
			case DISK -> Optional.of(new AdapterSpeed(32, 1));
			case FLOPPY -> Optional.of(new AdapterSpeed(64, 1));
		};
	}

	public int clampItems(int items)
	{
		return Math.max(1, Math.min(this.items, items));
	}

	public int clampTicks(int ticks)
	{
		return Math.max(this.ticks, Math.min(SLOWEST_TICKS, ticks));
	}
}
