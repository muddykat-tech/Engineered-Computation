package com.muddykat.engineeredcomputation.storage.cell;

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
	private final CellData data;

	private CellInventory(ItemStack stack, EraDataCell cell, Runnable onChanged)
	{
		this.stack = stack;
		this.cell = cell;
		this.onChanged = onChanged;
		this.data = cell instanceof UuidDataCell? new UuidCellData(stack, CellStorage.get()): new ComponentCellData(stack);
	}

	@Nullable
	public static CellInventory of(ItemStack stack, Runnable onChanged)
	{
		return stack.getItem() instanceof EraDataCell cell? new CellInventory(stack, cell, onChanged): null;
	}

	public boolean isReadable()
	{
		return !(data instanceof UuidCellData uuid)||uuid.readable();
	}

	public ItemStack getStack()
	{
		return stack;
	}

	public CellContents getContents()
	{
		return data.snapshot();
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

	public int getTypes()
	{
		return data.types();
	}

	public long getFreeTypes()
	{
		return Math.max(0, getTotalTypes()-data.types());
	}

	/** A cell holding items can't go into storage, or storage could nest inside itself. */
	private static boolean holdsItems(ItemKey key)
	{
		return key.getItem() instanceof EraDataCell&&getContents(key.toStack(1)).types()>0;
	}

	public static CellContents getContents(ItemStack stack)
	{
		CellInventory inventory = of(stack, () -> {
		});
		return inventory==null? CellContents.EMPTY: inventory.getContents();
	}

	public long getTotalBytes()
	{
		return cell.getTotalBytes();
	}

	public long getUsedBytes()
	{
		long count = data.totalCount();
		long bytesForItems = (count+unusedItemCount(count))/ITEMS_PER_BYTE;
		return (long)data.types()*cell.getBytesPerType()+bytesForItems;
	}

	public long getTotalTypes()
	{
		return cell.getTotalTypes();
	}

	public boolean isBytesFull()
	{
		return getUsedBytes()>=getTotalBytes()&&unusedItemCount(data.totalCount())==0;
	}

	public boolean isTypesFull()
	{
		return data.types()>=getTotalTypes();
	}

	private static long unusedItemCount(long count)
	{
		long remainder = count%ITEMS_PER_BYTE;
		return remainder==0? 0: ITEMS_PER_BYTE-remainder;
	}

	private boolean canHoldNewType(long freeBytes)
	{
		long bytesPerType = cell.getBytesPerType();
		long remainingTypes = Math.min(freeBytes/bytesPerType, cell.getTotalTypes()-data.types());
		return (freeBytes>bytesPerType||freeBytes==bytesPerType&&unusedItemCount(data.totalCount())>0)
				&&remainingTypes>0;
	}

	@Override
	public long insert(ItemKey key, long amount, boolean simulate)
	{
		if(amount<=0||holdsItems(key))
			return 0;
		long current = data.amount(key);

		long freeBytes = cell.getTotalBytes()-getUsedBytes();
		long room = Math.max(0, freeBytes*ITEMS_PER_BYTE+unusedItemCount(data.totalCount()));
		if(current==0)
		{
			if(!canHoldNewType(freeBytes))
				return 0;
			room -= (long)cell.getBytesPerType()*ITEMS_PER_BYTE;
			if(room<=0)
				return 0;
		}

		long inserted = Math.min(amount, room);
		if(!simulate&&inserted>0)
		{
			data.set(key, current+inserted);
			onChanged.run();
		}
		return inserted;
	}

	@Override
	public long extract(ItemKey key, long amount, boolean simulate)
	{
		if(amount<=0)
			return 0;
		long current = data.amount(key);
		long extracted = Math.min(amount, current);
		if(!simulate&&extracted>0)
		{
			data.set(key, current-extracted);
			onChanged.run();
		}
		return extracted;
	}

	@Override
	public long getAmount(ItemKey key)
	{
		return data.amount(key);
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
		data.collect(into);
	}
}
