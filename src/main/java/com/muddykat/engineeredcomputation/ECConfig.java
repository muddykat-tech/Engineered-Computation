package com.muddykat.engineeredcomputation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterSpeed;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;

public final class ECConfig
{
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue TAPE_ARCHIVING = BUILDER
			.comment("Whether tape drives move a network's biggest stocks onto their reels, and small ones back off them.",
					"Off, tape still takes items that are already on it, and new ones once other storage is full.")
			.translation(EngineeredComputation.MODID+".configuration.tapeArchiving")
			.define("tapeArchiving", true);

	private static final String KEY = EngineeredComputation.MODID+".configuration.";

	public static final ModConfigSpec.BooleanValue POWER = BUILDER
			.comment("Networks running on RF, fed in through Energy Accepters.")
			.translation(KEY+"section.power").push("power")
			.comment("Whether networks need RF, fed through Energy Accepters, to run.",
					"A network that can't pay its machines' costs stops until it can.")
			.translation(KEY+"powerEnabled")
			.define("enabled", false);

	public static final ModConfigSpec.IntValue ACCEPTER_CAPACITY = BUILDER
			.comment("How much RF each Energy Accepter holds. A network draws from all of its accepters.")
			.translation(KEY+"accepterCapacity")
			.defineInRange("accepterCapacity", 100_000, 1, Integer.MAX_VALUE);

	/** Each machine's cost in RF per tick, by config key (see {@link #costKey}). */
	private static final Map<String, ModConfigSpec.IntValue> COSTS = defineCosts();

	public static final ModConfigSpec SPEC = BUILDER.pop().build();

	private static Map<String, ModConfigSpec.IntValue> defineCosts()
	{
		Map<String, ModConfigSpec.IntValue> costs = new HashMap<>();
		BUILDER.comment("Each machine's cost in RF per tick, while power is enabled.").translation(KEY+"section.costs")
				.push("costs");
		for(EraDrive drive : EraDrive.values())
			if(drive.era().onDataNetwork())
				defineCost(costs, costKey(drive), defaultCost(drive));
		for(ComputerEra era : ComputerEra.values())
		{
			if(!era.onDataNetwork())
				continue;
			if(era.hasTerminal())
				defineCost(costs, terminalKey(era), 2*era.ordinal());
			if(AdapterSpeed.of(era).isPresent())
				for(AdapterKind kind : AdapterKind.values())
					defineCost(costs, adapterKey(era, kind), 4*era.ordinal());
		}
		defineCost(costs, "storageBridge", 8);
		defineCost(costs, "craftingComputer", 32);
		defineCost(costs, "dataProcessor", 24);
		BUILDER.pop();
		return Map.copyOf(costs);
	}

	private static void defineCost(Map<String, ModConfigSpec.IntValue> costs, String key, int cost)
	{
		costs.put(key, BUILDER.translation(KEY+"cost."+key).defineInRange(key, cost, 0, Integer.MAX_VALUE));
	}

	private static int defaultCost(EraDrive drive)
	{
		return switch(drive)
		{
			case DRUM_COMPUTER -> 16;
			case DRUM_BANK -> 8;
			case TAPE_DRIVE -> 24;
			case DISK_MAINFRAME -> 32;
			case DISK_BANK -> 16;
			case FLOPPY_WORKSTATION -> 40;
			case FLOPPY_BANK -> 20;
			case PUNCHCARD_VAULT -> 0;
		};
	}

	/** A config key from an enum name: DRUM_COMPUTER becomes drumComputer. */
	private static String camel(String name)
	{
		StringBuilder key = new StringBuilder();
		for(String word : name.toLowerCase(Locale.ROOT).split("_"))
			key.append(key.isEmpty()? word: Character.toUpperCase(word.charAt(0))+word.substring(1));
		return key.toString();
	}

	public static String costKey(EraDrive drive)
	{
		return camel(drive.name());
	}

	public static String terminalKey(ComputerEra era)
	{
		return camel(era.name())+"Terminal";
	}

	public static String adapterKey(ComputerEra era, AdapterKind kind)
	{
		return camel(era.name()+"_"+kind.name());
	}

	private ECConfig()
	{
	}

	public static boolean tapeArchiving()
	{
		return SPEC.isLoaded() ? TAPE_ARCHIVING.getAsBoolean() : TAPE_ARCHIVING.getDefault();
	}

	public static boolean powerEnabled()
	{
		return SPEC.isLoaded()? POWER.getAsBoolean(): POWER.getDefault();
	}

	public static int accepterCapacity()
	{
		return SPEC.isLoaded()? ACCEPTER_CAPACITY.getAsInt(): ACCEPTER_CAPACITY.getDefault();
	}

	/** A machine's cost in RF per tick by its config key, or 0 for a key with no cost. */
	public static int cost(String key)
	{
		ModConfigSpec.IntValue cost = COSTS.get(key);
		if(cost==null)
			return 0;
		return SPEC.isLoaded()? cost.getAsInt(): cost.getDefault();
	}
}
