package com.muddykat.engineeredcomputation.pneumatic;

/**
 * A Tube Station with a slot for a Filter Card or Program Deck, whose filter (an allow or a block list) decides every
 * deposit made with it - see StationFilter. Without one it's a plain Tube Station.
 */
public class AdvancedTubeStationBlock extends TubeStationBlock
{
	public AdvancedTubeStationBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean filters()
	{
		return true;
	}
}
