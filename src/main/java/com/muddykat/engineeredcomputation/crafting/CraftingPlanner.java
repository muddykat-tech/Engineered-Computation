package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public final class CraftingPlanner
{
	private static final int MAX_DEPTH = 32;
	public static final long MAX_AMOUNT = 1_000_000;

	private final List<ProvidedPattern> patterns;
	private final Map<ItemKey, List<Integer>> makers = new HashMap<>();
	private final Map<ItemKey, Long> stock;
	private final Map<ItemKey, Long> surplus = new HashMap<>();
	private final Map<ItemKey, Long> fromStorage = new LinkedHashMap<>();
	private final Map<ItemKey, Long> toCraft = new LinkedHashMap<>();
	private final Map<ItemKey, Long> missing = new LinkedHashMap<>();
	private final Map<Integer, Long> runs = new LinkedHashMap<>();
	private final Deque<Integer> making = new ArrayDeque<>();

	private CraftingPlanner(List<ProvidedPattern> patterns, Map<ItemKey, Long> stock)
	{
		this.patterns = patterns;
		this.stock = new HashMap<>(stock);
		for(int i = 0; i<patterns.size(); i++)
			if(!patterns.get(i).pattern().hasTags())
				for(ItemKey output : patterns.get(i).pattern().outputTotals().keySet())
					makers.computeIfAbsent(output, key -> new ArrayList<>()).add(i);
		makers.values().forEach(list -> list.sort((a, b) -> Boolean.compare(!patterns.get(a).isDirect(),
				!patterns.get(b).isDirect())));
	}

	public static CraftingPlan plan(ItemKey target, long amount, Map<ItemKey, Long> stock, List<ProvidedPattern> patterns)
	{
		CraftingPlanner planner = new CraftingPlanner(patterns, stock);
		amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
		planner.need(target, amount, true);
		return planner.result(target, Optional.empty(), amount);
	}

	public static CraftingPlan planOffer(ProvidedPattern offer, long amount, Map<ItemKey, Long> stock,
			List<ProvidedPattern> patterns)
	{
		List<ProvidedPattern> all = new ArrayList<>(patterns);
		all.add(offer);
		CraftingPlanner planner = new CraftingPlanner(all, stock);
		amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
		Pattern resolved = offer.resolved();
		int primary = resolved.primaryIndex();
		ItemKey target = ItemKey.of(resolved.primaryOutput());
		if(!offer.isResolved())
			offer.pattern().inputTags().forEach((slot, tag) -> {
				if(!offer.choices().containsKey(slot))
					planner.missing.merge(ItemKey.of(offer.pattern().input(slot)), 1L, Long::sum);
			});
		else
			planner.run(all.size()-1, resolved, target, amount);
		return planner.result(target, resolved.outputTag(primary), amount);
	}

	private CraftingPlan result(ItemKey target, Optional<TagKey<Item>> targetTag, long amount)
	{
		List<CraftingPlan.Step> steps = new ArrayList<>();
		runs.forEach((index, count) -> steps.add(new CraftingPlan.Step(patterns.get(index), count)));
		return new CraftingPlan(target, targetTag, amount, List.copyOf(steps), Map.copyOf(fromStorage),
				Map.copyOf(toCraft), Map.copyOf(missing));
	}

	private void need(ItemKey key, long amount, boolean target)
	{
		amount -= take(surplus, key, amount);
		if(!target)
		{
			long taken = take(stock, key, amount);
			if(taken>0)
				fromStorage.merge(key, taken, Long::sum);
			amount -= taken;
		}
		if(amount<=0)
			return;
		Integer maker = makers.getOrDefault(key, List.of()).stream()
				.filter(index -> !making.contains(index)).findFirst().orElse(null);
		if(maker==null||making.size()>=MAX_DEPTH)
		{
			missing.merge(key, amount, Long::sum);
			return;
		}
		run(maker, patterns.get(maker).resolved(), key, amount);
	}

	private void run(int index, Pattern pattern, ItemKey key, long amount)
	{
		long perRun = pattern.outputTotals().get(key);
		long count = (amount+perRun-1)/perRun;
		making.push(index);
		pattern.inputTotals().forEach((input, each) -> need(input, each*count, false));
		making.pop();
		runs.merge(index, count, Long::sum);
		pattern.outputTotals().forEach((output, each) -> {
			toCraft.merge(output, each*count, Long::sum);
			surplus.merge(output, each*count, Long::sum);
		});
		take(surplus, key, amount);
	}

	private static long take(Map<ItemKey, Long> from, ItemKey key, long amount)
	{
		long have = from.getOrDefault(key, 0L);
		long taken = Math.min(have, amount);
		if(taken>0)
			from.put(key, have-taken);
		return taken;
	}
}
