package com.muddykat.engineeredcomputation.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

public final class GuideRecipes
{
	public static final ResourceLocation WRENCH = id("assembly_wrench");
	public static final ResourceLocation CABLE = id("network_cable");
	public static final ResourceLocation BRIDGE = id("storage_bridge");
	public static final ResourceLocation ENERGY_ACCEPTER = id("energy_accepter");
	public static final ResourceLocation PROCESSOR_CASING = id("processor_casing");

	private GuideRecipes()
	{
	}

	public static ResourceLocation component(ComputerEra era)
	{
		return id(era.componentName());
	}

	public static ResourceLocation casing(ComputerEra era)
	{
		return id(era.casingName());
	}

	public static Optional<ResourceLocation> terminal(ComputerEra era)
	{
		return era.hasTerminal()? Optional.of(id(era.terminalName())): Optional.empty();
	}

	public static List<ResourceLocation> terminalDisks(ComputerEra era)
	{
		return era==ComputerEra.FLOPPY? ECDataCells.terminalDisks().stream().map(disk -> disk.getId()).toList(): List.of();
	}

	public static List<ResourceLocation> adapters(ComputerEra era)
	{
		return ECAdapters.of(era).values().stream().map(block -> block.getId()).toList();
	}

	public static Optional<ResourceLocation> programFloppy(ComputerEra era)
	{
		return era==ComputerEra.FLOPPY? Optional.of(ECAdapters.PROGRAM_FLOPPY.getId()): Optional.empty();
	}

	public static Optional<ResourceLocation> patternFloppy(ComputerEra era)
	{
		return era==ComputerEra.FLOPPY? Optional.of(ECCrafting.PATTERN_FLOPPY.getId()): Optional.empty();
	}

	public static List<ResourceLocation> cells(ComputerEra era)
	{
		if(era==ComputerEra.PUNCHCARD)
			return ECCatalogue.decks().stream().map(deck -> deck.getId()).toList();
		return ECDataCells.cellsOf(era).stream().map(cell -> cell.getId()).toList();
	}

	public static List<ResourceLocation> tubes(ComputerEra era)
	{
		return era==ComputerEra.PUNCHCARD? ECPneumatics.items().stream().map(item -> item.getId())
				.filter(id -> !id.equals(ECPneumatics.ADVANCED_TUBE_STATION.getId())).toList(): List.of();
	}

	/**
	 * The Advanced Tube Station, Filter Card and Program Deck, in that order: with the tubes, in the first edition (its
	 * Vacuum Tube's recipe shows under Other Parts).
	 */
	public static List<ResourceLocation> filterStation(ComputerEra era)
	{
		return era==ComputerEra.PUNCHCARD? List.of(ECPneumatics.ADVANCED_TUBE_STATION.getId(),
				ECCatalogue.FILTER_CARD.getId(), ECCatalogue.PROGRAM_DECK.getId()): List.of();
	}

	public static Optional<ResourceLocation> catalogueCard(ComputerEra era)
	{
		return era==ComputerEra.PUNCHCARD? Optional.of(ECCatalogue.CARD.getId()): Optional.empty();
	}

	public static Optional<ResourceLocation> nextGuide(ComputerEra era)
	{
		return era.ordinal()+1<ComputerEra.values().length
				? Optional.of(id(ComputerEra.values()[era.ordinal()+1].guideName())): Optional.empty();
	}

	public static List<ResourceLocation> shown(ComputerEra era)
	{
		List<ResourceLocation> recipes = new ArrayList<>(List.of(WRENCH));
		if(era.onDataNetwork())
			recipes.addAll(List.of(CABLE, BRIDGE, ENERGY_ACCEPTER));
		recipes.addAll(List.of(component(era), casing(era)));
		terminal(era).ifPresent(recipes::add);
		recipes.addAll(terminalDisks(era));
		recipes.addAll(tubes(era));
		catalogueCard(era).ifPresent(recipes::add);
		recipes.addAll(filterStation(era));
		recipes.addAll(adapters(era));
		programFloppy(era).ifPresent(recipes::add);
		patternFloppy(era).ifPresent(recipes::add);
		recipes.addAll(cells(era));
		nextGuide(era).ifPresent(recipes::add);
		return recipes;
	}

	public static List<ResourceLocation> prerequisites(Collection<ResourceLocation> shown, RecipeManager recipes,
			HolderLookup.Provider registries)
	{
		Set<Item> made = new HashSet<>();
		for(ResourceLocation id : shown)
			recipes.byKey(id).ifPresent(holder -> made.add(holder.value().getResultItem(registries).getItem()));
		Map<Item, ResourceLocation> needed = new LinkedHashMap<>();
		Deque<ResourceLocation> queue = new ArrayDeque<>(shown);
		while(!queue.isEmpty())
			for(Item part : modParts(queue.poll(), recipes))
				if(!made.contains(part)&&!needed.containsKey(part))
					recipeFor(part, recipes, registries).ifPresent(id -> {
						needed.put(part, id);
						queue.add(id);
					});
		List<ResourceLocation> ordered = new ArrayList<>();
		Set<ResourceLocation> visited = new HashSet<>();
		for(ResourceLocation id : needed.values())
			addAfterItsParts(id, needed, recipes, visited, ordered);
		return ordered;
	}

	private static void addAfterItsParts(ResourceLocation id, Map<Item, ResourceLocation> needed, RecipeManager recipes,
			Set<ResourceLocation> visited, List<ResourceLocation> ordered)
	{
		if(!visited.add(id))
			return;
		for(Item part : modParts(id, recipes))
			if(needed.containsKey(part))
				addAfterItsParts(needed.get(part), needed, recipes, visited, ordered);
		ordered.add(id);
	}

	private static List<Item> modParts(ResourceLocation id, RecipeManager recipes)
	{
		List<Item> parts = new ArrayList<>();
		recipes.byKey(id).ifPresent(holder -> {
			for(Ingredient ingredient : holder.value().getIngredients())
			{
				Item part = modPart(ingredient);
				if(part!=null)
					parts.add(part);
			}
		});
		return parts;
	}

	private static Item modPart(Ingredient ingredient)
	{
		ItemStack[] options = ingredient.getItems();
		if(options.length==0)
			return null;
		for(ItemStack option : options)
			if(!BuiltInRegistries.ITEM.getKey(option.getItem()).getNamespace().equals(EngineeredComputation.MODID))
				return null;
		return options[0].getItem();
	}

	private static Optional<ResourceLocation> recipeFor(Item item, RecipeManager recipes, HolderLookup.Provider registries)
	{
		ResourceLocation named = BuiltInRegistries.ITEM.getKey(item);
		Optional<ResourceLocation> any = Optional.empty();
		for(RecipeHolder<?> holder : recipes.getAllRecipesFor(RecipeType.CRAFTING))
		{
			Recipe<?> recipe = holder.value();
			if(recipe.getResultItem(registries).is(item))
			{
				if(holder.id().equals(named))
					return Optional.of(holder.id());
				if(any.isEmpty())
					any = Optional.of(holder.id());
			}
		}
		return any;
	}

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}
}
