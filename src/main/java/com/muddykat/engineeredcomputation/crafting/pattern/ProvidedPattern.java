package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.storage.ItemKey;

/**
 * A pattern somewhere on the network. Accepted lists, for each of a fuzzy pattern's inputs, what may go in its place
 * (see FuzzyPatterns).
 */
public record ProvidedPattern(Pattern pattern, @Nullable BlockPos processor, int slot, Map<Integer, Item> choices,
		Map<ItemKey, List<ItemKey>> accepted)
{
	public static ProvidedPattern direct(Pattern pattern)
	{
		return new ProvidedPattern(pattern, null, -1, Map.of(), Map.of());
	}

	public static ProvidedPattern abstractAt(Pattern pattern, BlockPos processor, int slot)
	{
		return new ProvidedPattern(pattern, processor, slot, Map.of(), Map.of());
	}

	public ProvidedPattern accepting(Map<ItemKey, List<ItemKey>> accepted)
	{
		return new ProvidedPattern(pattern, processor, slot, choices, accepted);
	}

	public boolean isDirect()
	{
		return processor==null;
	}

	public ProvidedPattern choosing(Map<Integer, Item> choices)
	{
		return new ProvidedPattern(pattern, processor, slot, Map.copyOf(choices), accepted);
	}

	public Pattern resolved()
	{
		return pattern.inputTags().isEmpty()? pattern: pattern.resolve(choices).orElse(pattern);
	}

	public boolean isResolved()
	{
		return pattern.inputTags().isEmpty()||pattern.resolve(choices).isPresent();
	}
}
