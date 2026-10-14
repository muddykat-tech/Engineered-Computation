package com.muddykat.engineeredcomputation.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

/**
 * What an era's guide holds, in order, apart from how it's drawn: the in-game guide lays it out on its pages and the
 * Patchouli handbook is exported from it, so the two can't drift apart.
 */
public final class GuideOutline
{
	public static final String KEY = "guide.engineered_computation.";
	/** Patchouli text functions that show a placeholder's current value; registered by the Patchouli compat. */
	public static final String ERA_FUNCTION = "ec_era";
	public static final String MEMORY_FUNCTION = "ec_memory";
	public static final String PART_FUNCTION = "ec_part";
	public static final String GUIDE_FUNCTION = "ec_guide";
	public static final String CONFIG_FUNCTION = "ec_config";
	public static final String CODE_FUNCTION = "ec_code";

	private final ComputerEra era;
	private final List<Step> steps = new ArrayList<>();

	private GuideOutline(ComputerEra era)
	{
		this.era = era;
	}

	/** {@code recipes} may be null where no level is loaded; the "other parts" chapter is then left out. */
	public static List<Step> of(ComputerEra era, @Nullable RecipeManager recipes,
			@Nullable HolderLookup.Provider registries)
	{
		GuideOutline outline = new GuideOutline(era);
		outline.basics();
		outline.eraChapter();
		outline.advancedStation();
		if(recipes!=null&&registries!=null)
			outline.otherParts(recipes, registries);
		if(era.ordinal()+1<ComputerEra.values().length)
		{
			ComputerEra next = ComputerEra.values()[era.ordinal()+1];
			outline.chapter(GuideTexts.NEXT);
			outline.text(GuideTexts.NEXT, Map.of(
					GuideTexts.NEXT_GUIDE, new Value(() -> ECContent.guide(next).get().getDescription(),
							GUIDE_FUNCTION+":"+next.getSerializedName()),
					GuideTexts.NEXT_ERA, eraName(next)));
			GuideRecipes.nextGuide(era).ifPresent(outline::recipe);
		}
		else
		{
			outline.chapter(GuideTexts.PALM);
			outline.text(GuideTexts.PALM);
		}
		outline.chapter(GuideTexts.CODES);
		outline.steps.add(new Codes());
		return List.copyOf(outline.steps);
	}

	private void basics()
	{
		chapter(GuideTexts.INTRO);
		text(GuideTexts.INTRO, Map.of(GuideTexts.ERA, eraName(era)));

		chapter(GuideTexts.WRENCH);
		text(GuideTexts.WRENCH);
		recipe(GuideRecipes.WRENCH);

		chapter(GuideTexts.COMPONENTS);
		text(GuideTexts.COMPONENTS);

		if(!era.onDataNetwork())
			return;

		chapter(GuideTexts.NETWORK);
		text(GuideTexts.NETWORK);
		text(GuideTexts.TERMINAL);

		chapter(GuideTexts.CABLES);
		text(GuideTexts.CABLES);
		recipe(GuideRecipes.CABLE);

		chapter(GuideTexts.BRIDGE);
		text(GuideTexts.BRIDGE);
		recipe(GuideRecipes.BRIDGE);

		chapter(GuideTexts.ENERGY_ACCEPTER);
		text(GuideTexts.ENERGY_ACCEPTER);
		recipe(GuideRecipes.ENERGY_ACCEPTER);

		chapter(GuideTexts.CELLS);
		text(GuideTexts.CELLS);
		section(GuideTexts.PLACEMENT);
		text(GuideTexts.PLACEMENT);
	}

	private void eraChapter()
	{
		chapter(GuideTexts.era(era));
		text(GuideTexts.era(era), Map.of(
				GuideTexts.MEMORY, new Value(era::memoryDisplayName, MEMORY_FUNCTION+":"+era.getSerializedName()),
				GuideTexts.PART, new Value(() -> ECContent.component(era).get().getDescription(),
						PART_FUNCTION+":"+era.getSerializedName())));
		recipe(GuideRecipes.component(era));
		recipe(GuideRecipes.casing(era));

		for(EraDrive drive : era.drives())
		{
			Component name = ECMultiblocks.ERA_DRIVES.get(drive).partBlock().getName();
			steps.add(new Heading(drive.getSerializedName(), null, name, true));
			String slots = drive.era().onDataNetwork()? "structure.slots": "structure.decks";
			steps.add(new Structure(drive.getSerializedName(), name, ECContent.casing(era).get(), drive.geometry(),
					Component.translatable(KEY+slots, drive.slots()), Component.translatable(KEY+"structure.hint")));
			if(!drive.geometry().terminalZones().isEmpty())
				text(GuideTexts.BUILTIN_TERMINAL);
			if(!drive.geometry().storageZones().isEmpty())
				text(GuideTexts.STORAGE_BAY);
			GuideTexts.drive(drive).ifPresent(this::text);
		}

		if(!GuideRecipes.tubes(era).isEmpty())
		{
			section(GuideTexts.TUBES);
			text(GuideTexts.TUBES);
			GuideRecipes.tubes(era).forEach(this::recipe);
		}
		GuideRecipes.pressurizer(era).ifPresent(pressurizer -> {
			section(GuideTexts.PRESSURIZER);
			text(GuideTexts.PRESSURIZER, Map.of(
					GuideTexts.CARRIER_SIZE_UNPRESSURED, config(GuideTexts.CARRIER_SIZE_UNPRESSURED,
							() -> ECConfig.carrierItems(false)),
					GuideTexts.CARRIER_TICKS, config(GuideTexts.CARRIER_TICKS, () -> ECConfig.carrierTicks(false)),
					GuideTexts.CARRIER_SIZE_PRESSURED, config(GuideTexts.CARRIER_SIZE_PRESSURED,
							() -> ECConfig.carrierItems(true)),
					GuideTexts.CARRIER_PRESSURED_TICKS, config(GuideTexts.CARRIER_PRESSURED_TICKS,
							() -> ECConfig.carrierTicks(true)),
					GuideTexts.CARRIERS_PER_FUEL, config(GuideTexts.CARRIERS_PER_FUEL, ECConfig::carriersPerFuel)));
			recipe(pressurizer);
		});
		if(!GuideRecipes.autotubes(era).isEmpty())
		{
			section(GuideTexts.AUTOTUBE);
			text(GuideTexts.AUTOTUBE);
			GuideRecipes.autotubes(era).forEach(this::recipe);
		}
		GuideRecipes.catalogueCard(era).ifPresent(card -> {
			section(GuideTexts.CATALOGUE_CARDS);
			text(GuideTexts.CATALOGUE_CARDS);
			recipe(card);
		});

		if(era.hasTerminal())
		{
			section(GuideTexts.eraTerminal(era));
			text(GuideTexts.eraTerminal(era));
		}
		GuideRecipes.terminal(era).ifPresent(this::recipe);
		if(!GuideRecipes.terminalDisks(era).isEmpty())
		{
			text(GuideTexts.TERMINAL_DISKS);
			GuideRecipes.terminalDisks(era).forEach(this::recipe);
		}

		if(!GuideRecipes.adapters(era).isEmpty())
		{
			section(GuideTexts.ADAPTERS);
			text(GuideTexts.ADAPTERS);
			GuideRecipes.adapters(era).forEach(this::recipe);
		}
		GuideRecipes.programFloppy(era).ifPresent(floppy -> {
			section(GuideTexts.PROGRAM_FLOPPY);
			text(GuideTexts.PROGRAM_FLOPPY);
			recipe(floppy);
		});
		GuideRecipes.patternFloppy(era).ifPresent(floppy -> {
			section(GuideTexts.CRAFTING_COMPUTER);
			text(GuideTexts.CRAFTING_COMPUTER);
			EraMachine computer = EraMachine.CRAFTING_COMPUTER;
			steps.add(new Structure(computer.blockName(), ECMultiblocks.CRAFTING_COMPUTER.partBlock().getName(),
					ECContent.casing(computer.era()).get(), computer.geometry(), null,
					Component.translatable(KEY+"structure.hint")));
			section(GuideTexts.DATA_PROCESSOR);
			text(GuideTexts.DATA_PROCESSOR);
			steps.add(new Structure(EraMachine.DATA_PROCESSOR.blockName(),
					ECMultiblocks.DATA_PROCESSOR.partBlock().getName(), ECContent.PROCESSOR_CASING.get(),
					EraMachine.DATA_PROCESSOR.geometry(), null, Component.translatable(KEY+"structure.hint")));
			recipe(GuideRecipes.PROCESSOR_CASING);
			section(GuideTexts.PATTERN_FLOPPY);
			text(GuideTexts.PATTERN_FLOPPY);
			recipe(floppy);
			section(GuideTexts.ORDERING);
			text(GuideTexts.ORDERING);
		});

		steps.add(new Heading("memory", null, era.onDataNetwork()
				? Component.translatable(KEY+"section.memory", era.memoryDisplayName())
				: Component.translatable(KEY+"section.decks"), true));
		GuideRecipes.cells(era).forEach(this::recipe);
	}

	private void advancedStation()
	{
		List<ResourceLocation> recipes = GuideRecipes.filterStation(era);
		if(recipes.isEmpty())
			return;
		chapter(GuideTexts.ADVANCED_STATION);
		text(GuideTexts.ADVANCED_STATION);
		recipe(recipes.get(0));
		section(GuideTexts.FILTER_CARDS);
		text(GuideTexts.FILTER_CARDS);
		recipe(recipes.get(1));
		section(GuideTexts.PROGRAM_DECK);
		text(GuideTexts.PROGRAM_DECK);
		recipe(recipes.get(2));
	}

	private void otherParts(RecipeManager recipes, HolderLookup.Provider registries)
	{
		List<ResourceLocation> others = GuideRecipes.prerequisites(GuideRecipes.shown(era), recipes, registries);
		if(others.isEmpty())
			return;
		chapter(GuideTexts.OTHER_PARTS);
		text(GuideTexts.OTHER_PARTS);
		others.forEach(this::recipe);
	}

	private static Value eraName(ComputerEra era)
	{
		return new Value(era::displayName, ERA_FUNCTION+":"+era.getSerializedName());
	}

	private static Value config(String name, Supplier<Integer> value)
	{
		return new Value(() -> Component.literal(Integer.toString(value.get())), CONFIG_FUNCTION+":"+name);
	}

	private void chapter(String page)
	{
		steps.add(new Heading(page, page, null, false));
	}

	private void section(String page)
	{
		steps.add(new Heading(page, page, null, true));
	}

	private void text(String page)
	{
		text(page, Map.of());
	}

	private void text(String page, Map<String, Value> values)
	{
		steps.add(new Text(page, values));
	}

	private void recipe(ResourceLocation id)
	{
		steps.add(new Recipe(id));
	}

	public sealed interface Step permits Heading, Text, Recipe, Structure, Codes
	{
	}

	/** A chapter, or a section within one; labelled by a guide page's contents line, or by {@code label}. */
	public record Heading(String id, @Nullable String page, @Nullable Component label, boolean section) implements Step
	{
	}

	public record Text(String page, Map<String, Value> values) implements Step
	{
	}

	public record Recipe(ResourceLocation id) implements Step
	{
	}

	/** {@code slots} is null for machines, which hold no cells. */
	public record Structure(String id, Component name, Block casing, EraDriveGeometry geometry,
			@Nullable Component slots, Component hint) implements Step
	{
	}

	/** Every error code, with its meaning from the codes page. */
	public record Codes() implements Step
	{
	}

	/** A placeholder's value as it stands, and the Patchouli text function that shows it live. */
	public record Value(Supplier<Component> now, String patchouli)
	{
	}
}
