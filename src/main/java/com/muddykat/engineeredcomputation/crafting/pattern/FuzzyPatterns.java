package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.storage.ItemKey;

/**
 * What fuzzy direct patterns stretch to. Fuzzy inputs take anything the pattern's recipe accepts in their place;
 * fuzzy outputs add the same recipe for related items, such as birch planks from birch logs for an oak log pattern,
 * where related means sharing an item tag and a real recipe has to exist.
 */
public final class FuzzyPatterns
{
	/** Tags bigger than this say little about what's alike, so they aren't used to find related items. */
	private static final int MAX_TAG_SIZE = 64;
	private static final int MAX_COMBINATIONS = 2048;

	private static final Map<RecipeManager, Map<Pattern, Map<ItemKey, List<ItemKey>>>> ACCEPTED = new WeakHashMap<>();
	private static final Map<RecipeManager, Map<Pattern, List<Pattern>>> DERIVED = new WeakHashMap<>();

	private FuzzyPatterns()
	{
	}

	/** For each of the pattern's input items, what may go in its place: itself first. Empty unless inputs are fuzzy. */
	public static Map<ItemKey, List<ItemKey>> accepted(Level level, Pattern pattern)
	{
		if(!pattern.fuzzyInputs()||pattern.mode()!=Pattern.Mode.DIRECT)
			return Map.of();
		return ACCEPTED.computeIfAbsent(level.getRecipeManager(), manager -> new HashMap<>())
				.computeIfAbsent(pattern, p -> findAccepted(level, p));
	}

	/** The patterns a fuzzy-output pattern adds for related items. */
	public static List<Pattern> derived(Level level, Pattern pattern)
	{
		if(!pattern.fuzzyOutputs()||pattern.mode()!=Pattern.Mode.DIRECT||pattern.primaryIndex()<0)
			return List.of();
		return DERIVED.computeIfAbsent(level.getRecipeManager(), manager -> new HashMap<>())
				.computeIfAbsent(pattern, p -> findDerived(level, p));
	}

	private static Map<ItemKey, List<ItemKey>> findAccepted(Level level, Pattern pattern)
	{
		Optional<RecipeHolder<CraftingRecipe>> recipe = pattern.recipe(level);
		if(recipe.isEmpty())
			return Map.of();
		Map<ItemKey, List<ItemKey>> accepted = new LinkedHashMap<>();
		for(ItemKey input : pattern.inputTotals().keySet())
		{
			Set<ItemKey> items = new LinkedHashSet<>();
			items.add(input);
			for(Ingredient ingredient : recipe.get().value().getIngredients())
				if(ingredient.test(input.displayStack()))
					for(ItemStack candidate : ingredient.getItems())
					{
						ItemKey key = ItemKey.of(candidate.copyWithCount(1));
						// Swapped in everywhere it's used, the result has to stay the same, so no other recipe is meant.
						if(!items.contains(key)&&sameResult(level, swap(pattern, Map.of(input, key)), pattern))
							items.add(key);
					}
			accepted.put(input, List.copyOf(items));
		}
		return Collections.unmodifiableMap(accepted);
	}

	private static List<Pattern> findDerived(Level level, Pattern pattern)
	{
		ItemStack result = pattern.primaryOutput();
		Set<Item> related = related(result.getItem());
		List<ItemKey> inputs = new ArrayList<>(pattern.inputTotals().keySet());
		List<List<ItemKey>> choices = new ArrayList<>();
		long combinations = 1;
		for(ItemKey input : inputs)
		{
			List<ItemKey> options = new ArrayList<>();
			options.add(input);
			for(Item item : related(input.getItem()))
				if(item!=input.getItem())
					options.add(ItemKey.of(new ItemStack(item)));
			choices.add(options);
			combinations *= options.size();
		}
		if(related.isEmpty()||combinations>MAX_COMBINATIONS)
			return List.of();
		Map<ItemKey, Pattern> found = new LinkedHashMap<>();
		for(long combination = 1; combination<combinations; combination++)
		{
			long rest = combination;
			Map<ItemKey, ItemKey> swaps = new HashMap<>();
			for(int index = 0; index<inputs.size(); index++)
			{
				List<ItemKey> options = choices.get(index);
				swaps.put(inputs.get(index), options.get((int)(rest%options.size())));
				rest /= options.size();
			}
			Pattern swapped = swap(pattern, swaps);
			ItemStack made = resultOf(level, swapped);
			ItemKey madeKey = made.isEmpty()? null: ItemKey.of(made);
			if(madeKey!=null&&related.contains(made.getItem())&&!found.containsKey(madeKey))
				found.put(madeKey, new Pattern(Pattern.Mode.DIRECT, swapped.inputs(), List.of(made), Map.of(), Map.of(),
						pattern.fuzzyInputs(), false));
		}
		return List.copyOf(found.values());
	}

	/** Items sharing a tag with this one, not counting itself, from tags small enough to mean something. */
	private static Set<Item> related(Item item)
	{
		Set<Item> related = new LinkedHashSet<>();
		item.builtInRegistryHolder().tags().forEach(tag -> {
			List<Item> members = Pattern.itemsOf(tag);
			if(members.size()<=MAX_TAG_SIZE)
				related.addAll(members);
		});
		related.remove(item);
		return related;
	}

	private static Pattern swap(Pattern pattern, Map<ItemKey, ItemKey> swaps)
	{
		List<ItemStack> inputs = new ArrayList<>();
		for(ItemStack stack : pattern.inputs())
		{
			ItemKey swapped = stack.isEmpty()? null: swaps.get(ItemKey.of(stack));
			inputs.add(swapped==null? stack: swapped.toStack(stack.getCount()));
		}
		return new Pattern(Pattern.Mode.DIRECT, inputs, pattern.outputs());
	}

	private static boolean sameResult(Level level, Pattern swapped, Pattern pattern)
	{
		ItemStack made = resultOf(level, swapped);
		ItemStack expected = pattern.primaryOutput();
		return ItemStack.isSameItemSameComponents(made, expected)&&made.getCount()==expected.getCount();
	}

	/** What a crafting table makes from this grid, or nothing. */
	public static ItemStack resultOf(Level level, Pattern pattern)
	{
		CraftingInput input = pattern.craftingInput();
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(holder -> holder.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
	}
}
