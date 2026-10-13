package com.muddykat.engineeredcomputation.storage.cell;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.ItemStorage;
import com.muddykat.engineeredcomputation.storage.StorageRole;

public final class CellInventory implements ItemStorage
{
	public static final int ITEMS_PER_BYTE = 8;

	private final ItemStack stack;
	private final EraDataCell cell;
	private final Runnable onChanged;

	private CellInventory(ItemStack stack, EraDataCell cell, Runnable onChanged)
	{
		this.stack = stack;
		this.cell = cell;
		this.onChanged = onChanged;
	}

	@Nullable
	public static CellInventory of(ItemStack stack, Runnable onChanged)
	{
		return stack.getItem() instanceof EraDataCell cell? new CellInventory(stack, cell, onChanged): null;
	}

	public ItemStack getStack()
	{
		return stack;
	}

	public CellContents getContents()
	{
		return getContents(stack);
	}

	public StorageRole getRole()
	{
		return StorageRole.of(cell.getEra());
	}

	public int getPriority()
	{
		return priority(stack);
	}

	public static int priority(ItemStack stack)
	{
		return stack.getOrDefault(ECDataComponents.PRIORITY.get(), 0);
	}

	public static void setPriority(ItemStack stack, int priority)
	{
		if(priority==0)
			stack.remove(ECDataComponents.PRIORITY.get());
		else
			stack.set(ECDataComponents.PRIORITY.get(), priority);
	}

	public long getFreeTypes()
	{
		return Math.max(0, getTotalTypes()-getContents().types());
	}

	/** A cell holding items can't go into storage, or storage could nest inside itself. */
	private static boolean holdsItems(ItemKey key)
	{
		return key.getItem() instanceof EraDataCell&&getContents(key.toStack(1)).types()>0;
	}

	public static CellContents getContents(ItemStack stack)
	{
		return stack.getOrDefault(ECDataComponents.CELL_CONTENTS.get(), CellContents.EMPTY);
	}

	public long getTotalBytes()
	{
		return cell.getTotalBytes();
	}

	public long getUsedBytes()
	{
		return getUsedBytes(getContents());
	}

	public long getTotalTypes()
	{
		return cell.getTotalTypes();
	}

	public boolean isBytesFull()
	{
		CellContents contents = getContents();
		return getUsedBytes(contents)>=getTotalBytes()&&unusedItemCount(contents.totalCount())==0;
	}

	public boolean isTypesFull()
	{
		return getContents().types()>=getTotalTypes();
	}

	private long getUsedBytes(CellContents contents)
	{
		long count = contents.totalCount();
		long bytesForItems = (count+unusedItemCount(count))/ITEMS_PER_BYTE;
		return (long)contents.types()*cell.getBytesPerType()+bytesForItems;
	}

	private static long unusedItemCount(long count)
	{
		long remainder = count%ITEMS_PER_BYTE;
		return remainder==0? 0: ITEMS_PER_BYTE-remainder;
	}

	private boolean canHoldNewType(CellContents contents, long freeBytes)
	{
		long bytesPerType = cell.getBytesPerType();
		long remainingTypes = Math.min(freeBytes/bytesPerType, cell.getTotalTypes()-contents.types());
		return (freeBytes>bytesPerType||freeBytes==bytesPerType&&unusedItemCount(contents.totalCount())>0)
				&&remainingTypes>0;
	}

	@Override
	public long insert(ItemKey key, long amount, boolean simulate)
	{
		if(amount<=0||holdsItems(key))
			return 0;
		CellContents contents = getContents();
		Map<ItemKey, Long> amounts = toMap(contents);
		long current = amounts.getOrDefault(key, 0L);

		long freeBytes = cell.getTotalBytes()-getUsedBytes(contents);
		long room = Math.max(0, freeBytes*ITEMS_PER_BYTE+unusedItemCount(contents.totalCount()));
		if(current==0)
		{
			if(!canHoldNewType(contents, freeBytes))
				return 0;
			room -= (long)cell.getBytesPerType()*ITEMS_PER_BYTE;
			if(room<=0)
				return 0;
		}

		long inserted = Math.min(amount, room);
		if(!simulate&&inserted>0)
		{
			amounts.put(key, current+inserted);
			write(amounts);
		}
		return inserted;
	}

	@Override
	public long extract(ItemKey key, long amount, boolean simulate)
	{
		if(amount<=0)
			return 0;
		Map<ItemKey, Long> amounts = toMap(getContents());
		long current = amounts.getOrDefault(key, 0L);
		long extracted = Math.min(amount, current);
		if(!simulate&&extracted>0)
		{
			if(extracted==current)
				amounts.remove(key);
			else
				amounts.put(key, current-extracted);
			write(amounts);
		}
		return extracted;
	}

	@Override
	public long getAmount(ItemKey key)
	{
		for(CellContents.Entry entry : getContents().entries())
			if(entry.key().equals(key))
				return entry.count();
		return 0;
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
		for(CellContents.Entry entry : getContents().entries())
			into.merge(entry.key(), entry.count(), Long::sum);
	}

	private static Map<ItemKey, Long> toMap(CellContents contents)
	{
		Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		for(CellContents.Entry entry : contents.entries())
			amounts.put(entry.key(), entry.count());
		return amounts;
	}

	private void write(Map<ItemKey, Long> amounts)
	{
		if(amounts.isEmpty())
			stack.remove(ECDataComponents.CELL_CONTENTS.get());
		else
			stack.set(ECDataComponents.CELL_CONTENTS.get(), CellContents.of(amounts));
		onChanged.run();
	}
}
