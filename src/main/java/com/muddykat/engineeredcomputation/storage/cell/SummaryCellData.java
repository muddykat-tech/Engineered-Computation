package com.muddykat.engineeredcomputation.storage.cell;

import java.util.Map;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

record SummaryCellData(ItemStack stack) implements CellData
{
	private CellSummary summary()
	{
		return stack.getOrDefault(ECDataComponents.CELL_SUMMARY.get(), new CellSummary(0, 0));
	}

	@Override
	public long amount(ItemKey key)
	{
		return 0;
	}

	@Override
	public int types()
	{
		return summary().types();
	}

	@Override
	public long totalCount()
	{
		return summary().count();
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
	}

	@Override
	public void set(ItemKey key, long amount)
	{
		throw new IllegalStateException("Cell contents can only change on the server");
	}

	@Override
	public CellContents snapshot()
	{
		return CellContents.EMPTY;
	}
}
