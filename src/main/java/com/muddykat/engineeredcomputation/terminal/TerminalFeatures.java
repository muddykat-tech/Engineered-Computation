package com.muddykat.engineeredcomputation.terminal;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public record TerminalFeatures(boolean search, boolean crafting, boolean orders, boolean requests)
{
	public static final TerminalFeatures GRID = new TerminalFeatures(false, false, false, false);
	public static final TerminalFeatures SEARCH = new TerminalFeatures(true, false, false, false);
	public static final TerminalFeatures CRAFTING = new TerminalFeatures(true, true, false, false);
	public static final TerminalFeatures ORDERING = new TerminalFeatures(true, true, true, false);
	public static final TerminalFeatures REQUESTS = new TerminalFeatures(false, false, false, true);

	public static TerminalFeatures of(ComputerEra era)
	{
		return switch(era)
		{
			case PUNCHCARD -> REQUESTS;
			case DRUM -> GRID;
			case TAPE -> SEARCH;
			case DISK -> CRAFTING;
			case FLOPPY -> ORDERING;
		};
	}
}
