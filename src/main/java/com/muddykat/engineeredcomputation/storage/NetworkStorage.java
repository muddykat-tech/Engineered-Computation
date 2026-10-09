package com.muddykat.engineeredcomputation.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NetworkStorage implements ItemStorage
{
	public static final NetworkStorage EMPTY = new NetworkStorage(List.of());

	private final List<CellInventory> cells;

	public NetworkStorage(List<CellInventory> cells)
	{
		this.cells = List.copyOf(cells);
	}

	public List<CellInventory> getCells()
	{
		return cells;
	}

	@Override
	public long insert(ItemKey key, long amount, boolean simulate)
	{
		long inserted = 0;
		for(CellInventory cell : insertOrder(key))
			if(inserted<amount)
				inserted += cell.insert(key, amount-inserted, simulate);
		return inserted;
	}

	public List<CellInventory> insertOrder(ItemKey key)
	{
		List<Ranked> holding = new ArrayList<>();
		List<Ranked> others = new ArrayList<>();
		for(int i = 0; i<cells.size(); i++)
		{
			CellInventory cell = cells.get(i);
			Ranked ranked = new Ranked(cell, i, cell.getPriority(), cell.getRole());
			(cell.getAmount(key)>0? holding: others).add(ranked);
		}
		holding.sort(Comparator.comparingInt((Ranked r) -> -r.priority)
				.thenComparing(r -> r.role!=StorageRole.BULK)
				.thenComparingInt(Ranked::index));
		Map<Ranked, Long> freeTypes = new HashMap<>();
		others.forEach(r -> freeTypes.put(r, r.cell.getFreeTypes()));
		others.sort(Comparator.comparing((Ranked r) -> r.role==StorageRole.BULK)
				.thenComparingInt(r -> -r.priority)
				.thenComparingInt(r -> r.role.ordinal())
				.thenComparingLong(r -> -freeTypes.get(r))
				.thenComparingInt(Ranked::index));
		List<CellInventory> order = new ArrayList<>(cells.size());
		holding.forEach(r -> order.add(r.cell));
		others.forEach(r -> order.add(r.cell));
		return order;
	}

	@Override
	public long extract(ItemKey key, long amount, boolean simulate)
	{
		long extracted = 0;
		for(CellInventory cell : extractOrder())
			if(extracted<amount)
				extracted += cell.extract(key, amount-extracted, simulate);
		return extracted;
	}

	public List<CellInventory> extractOrder()
	{
		List<Ranked> ranked = new ArrayList<>(cells.size());
		for(int i = 0; i<cells.size(); i++)
			ranked.add(new Ranked(cells.get(i), i, cells.get(i).getPriority(), cells.get(i).getRole()));
		ranked.sort(Comparator.comparing((Ranked r) -> r.role==StorageRole.BULK)
				.thenComparingInt(Ranked::priority)
				.thenComparingInt(Ranked::index));
		return ranked.stream().map(Ranked::cell).toList();
	}

	private record Ranked(CellInventory cell, int index, int priority, StorageRole role)
	{
	}

	@Override
	public long getAmount(ItemKey key)
	{
		long total = 0;
		for(CellInventory cell : cells)
			total += cell.getAmount(key);
		return total;
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
		for(CellInventory cell : cells)
			cell.collect(into);
	}
}
