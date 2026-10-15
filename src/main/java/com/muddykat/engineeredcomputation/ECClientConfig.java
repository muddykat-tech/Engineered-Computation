package com.muddykat.engineeredcomputation;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ECClientConfig
{
	public enum TerminalSort
	{
		NAME,
		COUNT
	}

	/** Where terminals list what Storage Analyzers report. */
	public enum RemotePlacement
	{
		MIXED,
		TOP,
		BOTTOM,
		HIDDEN
	}

	public enum CraftableFilter
	{
		BOTH,
		ONLY,
		HIDDEN
	}

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	private static final String KEY = EngineeredComputation.MODID+".configuration.";

	public static final ModConfigSpec.EnumValue<TerminalSort> TERMINAL_SORT = BUILDER
			.comment("How terminals sort their items. The buttons beside a terminal's search field change these.")
			.translation(KEY+"section.terminals").push("terminals")
			.comment("What terminals sort their items by.")
			.translation(KEY+"terminalSort")
			.defineEnum("sortBy", TerminalSort.NAME);

	public static final ModConfigSpec.BooleanValue TERMINAL_DESCENDING = BUILDER
			.comment("Whether terminals sort their items in descending order.")
			.translation(KEY+"terminalDescending")
			.define("descending", false);

	public static final ModConfigSpec.EnumValue<CraftableFilter> TERMINAL_CRAFTABLES = BUILDER
			.comment("Whether autocrafting terminals show stored and craftable items, only craftable ones, or hide the items",
					"that can only be crafted.")
			.translation(KEY+"terminalCraftables")
			.defineEnum("craftables", CraftableFilter.BOTH);

	public static final ModConfigSpec.EnumValue<RemotePlacement> TERMINAL_REMOTE = BUILDER
			.comment("Where terminals list what Storage Analyzers report: sorted in with what the network holds, at the",
					"top, at the bottom, or not at all.")
			.translation(KEY+"terminalRemote")
			.defineEnum("analyzed", RemotePlacement.MIXED);

	public static final ModConfigSpec SPEC = BUILDER.pop().build();

	private ECClientConfig()
	{
	}

	public static TerminalSort terminalSort()
	{
		return SPEC.isLoaded()? TERMINAL_SORT.get(): TERMINAL_SORT.getDefault();
	}

	public static boolean terminalDescending()
	{
		return SPEC.isLoaded()? TERMINAL_DESCENDING.get(): TERMINAL_DESCENDING.getDefault();
	}

	public static CraftableFilter terminalCraftables()
	{
		return SPEC.isLoaded()? TERMINAL_CRAFTABLES.get(): TERMINAL_CRAFTABLES.getDefault();
	}

	public static void setTerminalCraftables(CraftableFilter filter)
	{
		if(!SPEC.isLoaded())
			return;
		TERMINAL_CRAFTABLES.set(filter);
		SPEC.save();
	}

	public static RemotePlacement terminalRemote()
	{
		return SPEC.isLoaded()? TERMINAL_REMOTE.get(): TERMINAL_REMOTE.getDefault();
	}

	public static void setTerminalRemote(RemotePlacement placement)
	{
		if(!SPEC.isLoaded())
			return;
		TERMINAL_REMOTE.set(placement);
		SPEC.save();
	}

	public static void setTerminalSort(TerminalSort sort, boolean descending)
	{
		if(!SPEC.isLoaded())
			return;
		TERMINAL_SORT.set(sort);
		TERMINAL_DESCENDING.set(descending);
		SPEC.save();
	}
}
