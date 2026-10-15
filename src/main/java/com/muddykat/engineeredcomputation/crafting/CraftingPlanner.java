package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.ProvidedPattern;
import com.muddykat.engineeredcomputation.remote.RemoteSources;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;
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
	private final List<Remote> remote = new ArrayList<>();
	private final Map<Integer, Map<ItemKey, Long>> requests = new LinkedHashMap<>();

	private record Remote(BlockPos analyzer, StorageAnalyzerBlockEntity.Priority priority, Map<ItemKey, Long> left)
	{
	}

	private CraftingPlanner(List<ProvidedPattern> patterns, Map<ItemKey, Long> stock, List<RemoteSources.Source> remote)
	{
		this.patterns = patterns;
		this.stock = new HashMap<>(stock);
		for(RemoteSources.Source source : remote)
			this.remote.add(new Remote(source.analyzer(), source.priority(), new HashMap<>(source.stock())));
		for(int i = 0; i<patterns.size(); i++)
			if(!patterns.get(i).pattern().hasTags())
				for(ItemKey output : patterns.get(i).pattern().outputTotals().keySet())
					makers.computeIfAbsent(output, key -> new ArrayList<>()).add(i);
		makers.values().forEach(list -> list.sort((a, b) -> Boolean.compare(!patterns.get(a).isDirect(),
				!patterns.get(b).isDirect())));
	}

	public static CraftingPlan plan(ItemKey target, long amount, Map<ItemKey, Long> stock, List<ProvidedPattern> patterns)
	{
		return plan(target, amount, stock, patterns, List.of());
	}

	public static CraftingPlan plan(ItemKey target, long amount, Map<ItemKey, Long> stock, List<ProvidedPattern> patterns,
			List<RemoteSources.Source> remote)
	{
		CraftingPlanner planner = new CraftingPlanner(patterns, stock, remote);
		amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
		planner.need(target, amount, true);
		return planner.result(target, Optional.empty(), amount);
	}

	public static CraftingPlan planOffer(ProvidedPattern offer, long amount, Map<ItemKey, Long> stock,
			List<ProvidedPattern> patterns)
	{
		return planOffer(offer, amount, stock, patterns, List.of());
	}

	public static CraftingPlan planOffer(ProvidedPattern offer, long amount, Map<ItemKey, Long> stock,
			List<ProvidedPattern> patterns, List<RemoteSources.Source> remote)
	{
		List<ProvidedPattern> all = new ArrayList<>(patterns);
		all.add(offer);
		CraftingPlanner planner = new CraftingPlanner(all, stock, remote);
		amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
		Pattern resolved = offer.resolved();
		int primary = resolved.primaryIndex();
		boolean sends = resolved.sendsOnly();
		ItemKey target = ItemKey.of(sends? resolved.input(resolved.firstInput()): resolved.primaryOutput());
		if(!offer.isResolved())
			offer.pattern().inputTags().forEach((slot, tag) -> {
				if(!offer.choices().containsKey(slot))
					planner.missing.merge(ItemKey.of(offer.pattern().input(slot)), 1L, Long::sum);
			});
		else if(sends)
			planner.send(all.size()-1, resolved, amount);
		else
			planner.run(all.size()-1, resolved, target, amount);
		return planner.result(target, sends? Optional.empty(): resolved.outputTag(primary), amount);
	}

	/** Gets the items from what the Storage Analyzers report only, whatever their settings. */
	public static CraftingPlan planRequest(ItemKey target, long amount, List<RemoteSources.Source> remote)
	{
		CraftingPlanner planner = new CraftingPlanner(List.of(), Map.of(), remote);
		amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
		long left = amount;
		for(StorageAnalyzerBlockEntity.Priority priority : StorageAnalyzerBlockEntity.Priority.values())
			left -= planner.request(target, left, priority);
		if(left>0)
			planner.missing.put(target, left);
		return planner.result(target, Optional.empty(), amount);
	}

	private CraftingPlan result(ItemKey target, Optional<TagKey<Item>> targetTag, long amount)
	{
		List<CraftingPlan.Step> steps = new ArrayList<>();
		runs.forEach((index, count) -> steps.add(new CraftingPlan.Step(patterns.get(index), count)));
		List<CraftingPlan.Request> planned = new ArrayList<>();
		requests.forEach((source, items) -> items.forEach((key, count) ->
				planned.add(new CraftingPlan.Request(remote.get(source).analyzer(), key, count))));
		return new CraftingPlan(target, targetTag, amount, List.copyOf(steps), Map.copyOf(fromStorage),
				Map.copyOf(toCraft), Map.copyOf(missing), List.copyOf(planned));
	}

	private long request(ItemKey key, long amount, StorageAnalyzerBlockEntity.Priority priority)
	{
		long got = 0;
		for(int index = 0; index<remote.size()&&got<amount; index++)
		{
			if(remote.get(index).priority()!=priority)
				continue;
			long taken = take(remote.get(index).left(), key, amount-got);
			if(taken>0)
				requests.computeIfAbsent(index, s -> new LinkedHashMap<>()).merge(key, taken, Long::sum);
			got += taken;
		}
		return got;
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
		if(!target)
			amount -= request(key, amount, StorageAnalyzerBlockEntity.Priority.BEFORE_CRAFTING);
		if(amount<=0)
			return;
		Integer maker = makers.getOrDefault(key, List.of()).stream()
				.filter(index -> !making.contains(index)).findFirst().orElse(null);
		if(maker==null||making.size()>=MAX_DEPTH)
		{
			if(!target&&maker==null)
				amount -= request(key, amount, StorageAnalyzerBlockEntity.Priority.AFTER_CRAFTING);
			if(amount>0)
				missing.merge(key, amount, Long::sum);
			return;
		}
		run(maker, patterns.get(maker).resolved(), key, amount);
	}

	/** Runs a pattern that only sends its items away amount times. */
	private void send(int index, Pattern pattern, long count)
	{
		making.push(index);
		pattern.inputTotals().forEach((input, each) -> need(input, each*count, false));
		making.pop();
		runs.merge(index, count, Long::sum);
	}

	/** Takes what a fuzzy input accepts from storage, whichever there's most of first, then makes the rest. */
	private void needAny(ItemKey key, List<ItemKey> alternatives, long amount)
	{
		amount -= take(surplus, key, amount);
		List<ItemKey> byStock = new ArrayList<>(alternatives);
		byStock.sort(Comparator.comparingLong((ItemKey alternative) -> -stock.getOrDefault(alternative, 0L)));
		for(ItemKey alternative : byStock)
		{
			long taken = take(stock, alternative, amount);
			if(taken>0)
				fromStorage.merge(alternative, taken, Long::sum);
			amount -= taken;
		}
		if(amount>0)
			need(key, amount, false);
	}

	private void run(int index, Pattern pattern, ItemKey key, long amount)
	{
		long perRun = pattern.outputTotals().get(key);
		long count = (amount+perRun-1)/perRun;
		making.push(index);
		Map<ItemKey, List<ItemKey>> accepted = patterns.get(index).accepted();
		pattern.inputTotals().forEach((input, each) -> {
			List<ItemKey> alternatives = accepted.getOrDefault(input, List.of());
			if(alternatives.size()>1)
				needAny(input, alternatives, each*count);
			else
				need(input, each*count, false);
		});
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
