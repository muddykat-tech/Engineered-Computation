package com.muddykat.engineeredcomputation.storage;

import java.util.Map;

public interface ItemStorage
{
	long insert(ItemKey key, long amount, boolean simulate);

	long extract(ItemKey key, long amount, boolean simulate);

	long getAmount(ItemKey key);

	void collect(Map<ItemKey, Long> into);
}
