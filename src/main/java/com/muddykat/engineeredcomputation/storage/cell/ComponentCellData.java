package com.muddykat.engineeredcomputation.storage.cell;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

record ComponentCellData(ItemStack stack) implements CellData
{
	@Override
	public long amount(ItemKey key)
	{
		for(CellContents.Entry entry : snapshot().entries())
			if(entry.key().equals(key))
				return entry.count();
		return 0;
	}

	@Override
	public int types()
	{
		return snapshot().types();
	}

	@Override
	public long totalCount()
	{
		return snapshot().totalCount();
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
		for(CellContents.Entry entry : snapshot().entries())
			into.merge(entry.key(), entry.count(), Long::sum);
	}

	@Override
	public void set(ItemKey key, long amount)
	{
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(CellContents.Entry entry : snapshot().entries())
			amounts.put(entry.key(), entry.count());
		if(amount>0)
			amounts.put(key, amount);
		else
			amounts.remove(key);
		if(amounts.isEmpty())
			stack.remove(ECDataComponents.CELL_CONTENTS.get());
		else
			stack.set(ECDataComponents.CELL_CONTENTS.get(), CellContents.of(amounts));
	}

	@Override
	public CellContents snapshot()
	{
		return stack.getOrDefault(ECDataComponents.CELL_CONTENTS.get(), CellContents.EMPTY);
	}
}
