package com.muddykat.engineeredcomputation.crafting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public record CraftingPlan(ItemKey target, Optional<TagKey<Item>> targetTag, long amount, List<Step> steps,
		Map<ItemKey, Long> fromStorage, Map<ItemKey, Long> toCraft, Map<ItemKey, Long> missing)
{
	public record Step(ProvidedPattern provided, long runs)
	{
	}

	public boolean isPossible()
	{
		return missing.isEmpty()&&!steps.isEmpty();
	}
}
