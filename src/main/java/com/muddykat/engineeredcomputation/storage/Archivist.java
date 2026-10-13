package com.muddykat.engineeredcomputation.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.muddykat.engineeredcomputation.storage.cell.CellInventory;

public final class Archivist
{
	public static final int ARCHIVE_STACKS = 16;
	public static final int RETURN_STACKS = 2;

	private Archivist()
	{
	}

	public static long run(NetworkStorage storage, List<CellInventory> reels, long budget)
	{
		long moved = 0;
		for(CellInventory reel : reels)
		{
			if(moved>=budget)
				break;
			List<CellInventory> sources = new ArrayList<>();
			List<CellInventory> elsewhere = new ArrayList<>();
			for(CellInventory cell : storage.getCells())
			{
				if(cell.getStack()==reel.getStack()||cell.getRole()==StorageRole.BULK)
					continue;
				elsewhere.add(cell);
				if(cell.getPriority()<=reel.getPriority())
					sources.add(cell);
			}
			moved += tidy(reel, sources, elsewhere, budget-moved);
		}
		return moved;
	}

	private static long tidy(CellInventory reel, List<CellInventory> sources, List<CellInventory> elsewhere, long budget)
	{
		long moved = 0;
		Map<ItemKey, Long> onReel = new HashMap<>();
		reel.collect(onReel);
		Map<ItemKey, Long> outside = new HashMap<>();
		sources.forEach(cell -> cell.collect(outside));

		for(ItemKey key : onReel.keySet())
			if(moved<budget&&outside.getOrDefault(key, 0L)>0)
				moved += move(key, Math.min(outside.get(key), budget-moved), new NetworkStorage(sources), reel);

		NetworkStorage others = new NetworkStorage(elsewhere);
		for(Map.Entry<ItemKey, Long> entry : onReel.entrySet())
		{
			ItemKey key = entry.getKey();
			if(moved>=budget||entry.getValue()>=stacks(key, RETURN_STACKS)||outside.getOrDefault(key, 0L)>0)
				continue;
			long amount = Math.min(reel.getAmount(key), budget-moved);
			long fits = others.insert(key, amount, true);
			if(fits>0)
			{
				long taken = reel.extract(key, fits, false);
				others.insert(key, taken, false);
				moved += taken;
			}
		}

		if(moved<budget&&reel.getFreeTypes()>0)
		{
			ItemKey biggest = null;
			for(Map.Entry<ItemKey, Long> entry : outside.entrySet())
				if(!onReel.containsKey(entry.getKey())&&entry.getValue()>=stacks(entry.getKey(), ARCHIVE_STACKS)
						&&(biggest==null||entry.getValue()>outside.get(biggest)))
					biggest = entry.getKey();
			if(biggest!=null)
				moved += move(biggest, Math.min(outside.get(biggest), budget-moved), new NetworkStorage(sources), reel);
		}
		return moved;
	}

	private static long move(ItemKey key, long amount, NetworkStorage from, CellInventory reel)
	{
		long fits = Math.min(amount, reel.insert(key, amount, true));
		if(fits<=0)
			return 0;
		long taken = from.extract(key, fits, false);
		reel.insert(key, taken, false);
		return taken;
	}

	private static long stacks(ItemKey key, int stacks)
	{
		return (long)stacks*key.getMaxStackSize();
	}
}
