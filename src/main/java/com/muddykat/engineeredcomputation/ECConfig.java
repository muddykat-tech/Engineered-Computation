package com.muddykat.engineeredcomputation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterSpeed;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveMode;
import com.muddykat.engineeredcomputation.storage.cell.persist.IndexedCellFile;

public final class ECConfig
{
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue TAPE_ARCHIVING = BUILDER
			.comment("Whether bulk storage drives move a network's biggest stocks onto themself while moving smaller stock into generic storage if possible",
					"Off, bulk storage drives still takes items that are already on it, and new ones once other storage is full.")
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

	private static final Map<String, ModConfigSpec.IntValue> COSTS = defineCosts();

	public static final ModConfigSpec.BooleanValue PRESSURIZER_REQUIRED = BUILDER.pop()
			.comment("Pneumatic tube networks, pressurised by Pressurizers.")
			.translation(KEY+"section.pneumatics").push("pneumatics")
			.comment("Whether a tube network needs a fuelled Pressurizer to run at high pressure.",
					"Off, every tube network runs at high pressure, with no Pressurizer.")
			.translation(KEY+"pressurizerRequired")
			.define("pressurizerRequired", true);

	public static final ModConfigSpec.IntValue LOW_PRESSURE_ITEMS = BUILDER
			.comment("How many items a carrier holds at low pressure: with no fuelled Pressurizer on its network.",
					"Autotubes aren't held back by pressure.")
			.translation(KEY+"lowPressureItems")
			.defineInRange("lowPressureItems", 8, 1, 64);

	public static final ModConfigSpec.IntValue LOW_PRESSURE_TICKS = BUILDER
			.comment("How long a station waits between carriers at low pressure, in ticks.")
			.translation(KEY+"lowPressureTicks")
			.defineInRange("lowPressureTicks", 10, 1, 1200);

	public static final ModConfigSpec.IntValue HIGH_PRESSURE_ITEMS = BUILDER
			.comment("How many items a carrier holds at high pressure (never more than a stack of them).")
			.translation(KEY+"highPressureItems")
			.defineInRange("highPressureItems", 64, 1, 64);

	public static final ModConfigSpec.IntValue HIGH_PRESSURE_TICKS = BUILDER
			.comment("How long a station waits between carriers at high pressure, in ticks.")
			.translation(KEY+"highPressureTicks")
			.defineInRange("highPressureTicks", 4, 1, 1200);

	public static final ModConfigSpec.ConfigValue<String> PRESSURIZER_FUEL = BUILDER
			.comment("The item tag a Pressurizer burns for pressure.")
			.translation(KEY+"pressurizerFuel")
			.define("pressurizerFuel", "minecraft:coals");

	public static final ModConfigSpec.IntValue CARRIERS_PER_FUEL = BUILDER
			.comment("How many high pressure carriers one fuel item gives a Pressurizer.")
			.translation(KEY+"carriersPerFuel")
			.defineInRange("carriersPerFuel", 128, 1, 100_000);

	public static final ModConfigSpec.IntValue CRAFTING_TICKS = BUILDER.pop()
			.comment("Autocrafting by Crafting Computers.")
			.translation(KEY+"section.crafting").push("crafting")
			.comment("How many ticks a Crafting Computer takes over each crafting grid recipe it crafts.",
					"Processing patterns run through Advanced Data Processors, at their machines' own pace.")
			.translation(KEY+"craftingTicks")
			.defineInRange("craftingTicks", 10, 1, 1200);

	public static final ModConfigSpec.EnumValue<CellSaveMode> CELL_SAVE_MODE = BUILDER.pop()
			.comment("How memory cell contents are saved, in the world's data/engineered_computation folder. Only change these settings if you know what you're doing.")
			.translation(KEY+"section.storage").push("storage")
			.comment("INDEXED rewrites only the cells that changed since the last save, and is normally the best choice.",
					"BULK rewrites every cell into one file whenever any of them changed since the last save. If you need to save space it has a smaller save footprint, but it writes far more to disk on each save, and more the more cells the world has.",
					"Changing this moves the saved cells over to the new mode the next time the world loads.")
			.translation(KEY+"cellSaveMode")
			.defineEnum("saveMode", CellSaveMode.INDEXED);

	public static final ModConfigSpec.ConfigValue<Integer> SECTOR_SIZE = BUILDER
			.comment("Sector size of the INDEXED file, in bytes. Smaller sectors waste less space per cell, but when storage cells fill, they need to move around on the file more often, and this can leave small gaps behind. (this is cleaned automatically on world load or via command)",
					"Larger sectors give each cell more room to grow in place, but waste far more space, for example using 1024 or more can bloat the file size by 2-3 times.",
					"I'd leave this at the default of 128 if you don't know what you're doing.",
					"Changes to this value are applied on world load, or if you run /engineeredcomputation cells compact.")
			.translation(KEY+"sectorSize")
			.defineInList("sectorSize", IndexedCellFile.DEFAULT_SECTOR, Arrays.asList(64, 128, 256, 512, 1024, 2048, 4096));

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
			case DISK_PROCESSOR -> 24;
			case FLOPPY_PROCESSOR -> 30;
			case COMPACT_FLOPPY_BAY -> 10;
			case PUNCHCARD_VAULT -> 0;
		};
	}

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

	public static boolean pressurizerRequired()
	{
		return SPEC.isLoaded()? PRESSURIZER_REQUIRED.getAsBoolean(): PRESSURIZER_REQUIRED.getDefault();
	}

	public static int carrierItems(boolean pressurised)
	{
		ModConfigSpec.IntValue items = pressurised? HIGH_PRESSURE_ITEMS: LOW_PRESSURE_ITEMS;
		return SPEC.isLoaded()? items.getAsInt(): items.getDefault();
	}

	public static int carrierTicks(boolean pressurised)
	{
		ModConfigSpec.IntValue ticks = pressurised? HIGH_PRESSURE_TICKS: LOW_PRESSURE_TICKS;
		return SPEC.isLoaded()? ticks.getAsInt(): ticks.getDefault();
	}

	public static String pressurizerFuel()
	{
		return SPEC.isLoaded()? PRESSURIZER_FUEL.get(): PRESSURIZER_FUEL.getDefault();
	}

	public static int craftingTicks()
	{
		return SPEC.isLoaded()? CRAFTING_TICKS.getAsInt(): CRAFTING_TICKS.getDefault();
	}

	public static int carriersPerFuel()
	{
		return SPEC.isLoaded()? CARRIERS_PER_FUEL.getAsInt(): CARRIERS_PER_FUEL.getDefault();
	}

	public static CellSaveMode cellSaveMode()
	{
		return SPEC.isLoaded()? CELL_SAVE_MODE.get(): CELL_SAVE_MODE.getDefault();
	}

	public static int sectorSize()
	{
		return SPEC.isLoaded()? SECTOR_SIZE.get(): SECTOR_SIZE.getDefault();
	}

	public static int cost(String key)
	{
		ModConfigSpec.IntValue cost = COSTS.get(key);
		if(cost==null)
			return 0;
		return SPEC.isLoaded()? cost.getAsInt(): cost.getDefault();
	}
}
