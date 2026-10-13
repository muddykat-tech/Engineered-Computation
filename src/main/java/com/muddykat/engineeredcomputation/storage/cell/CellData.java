package com.muddykat.engineeredcomputation.storage.cell;

import java.util.Map;

import com.muddykat.engineeredcomputation.storage.ItemKey;

interface CellData
{
	long amount(ItemKey key);

	int types();

	long totalCount();

	void collect(Map<ItemKey, Long> into);

	void set(ItemKey key, long amount);

	CellContents snapshot();
}
