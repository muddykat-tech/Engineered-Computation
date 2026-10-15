package com.muddykat.engineeredcomputation.crafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.crafting.pattern.ProvidedPattern;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record CraftingPlan(ItemKey target, Optional<TagKey<Item>> targetTag, long amount, List<Step> steps,
		Map<ItemKey, Long> fromStorage, Map<ItemKey, Long> toCraft, Map<ItemKey, Long> missing, List<Request> requests)
{
	public record Step(ProvidedPattern provided, long runs)
	{
	}

	/** Items a Storage Analyzer is to get from what it faces. */
	public record Request(BlockPos analyzer, ItemKey key, long amount)
	{
	}

	public Map<ItemKey, Long> requested()
	{
		Map<ItemKey, Long> requested = new LinkedHashMap<>();
		for(Request request : requests)
			requested.merge(request.key(), request.amount(), Long::sum);
		return requested;
	}

	public boolean isPossible()
	{
		return missing.isEmpty()&&(!steps.isEmpty()||!requests.isEmpty());
	}
}
