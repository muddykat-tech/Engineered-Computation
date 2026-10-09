package com.muddykat.engineeredcomputation.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class CellInventory implements ItemStorage
{
	public static final int ITEMS_PER_BYTE = 8;

	private static final String CONTENTS = "cell_contents";
	private static final String PRIORITY = "priority";

	private final ItemStack stack;
	private final EraDataCell cell;
	private final Runnable onChanged;
	/** The stored tag {@link #decoded} was read from: every write stores a new one, so this tells if it's current. */
	@Nullable
	private Tag decodedFrom;
	private CellContents decoded = CellContents.EMPTY;

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
		Tag stored = stack.getTag()==null? null: stack.getTag().get(CONTENTS);
		if(stored!=decodedFrom)
		{
			decoded = decode(stored);
			decodedFrom = stored;
		}
		return decoded;
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
		CompoundTag tag = stack.getTag();
		return tag==null? 0: Mth.clamp(tag.getInt(PRIORITY), EraDataCell.MIN_PRIORITY, EraDataCell.MAX_PRIORITY);
	}

	public static void setPriority(ItemStack stack, int priority)
	{
		if(priority==0)
			stack.removeTagKey(PRIORITY);
		else
			stack.getOrCreateTag().putInt(PRIORITY, priority);
	}

	public long getFreeTypes()
	{
		return Math.max(0, getTotalTypes()-getContents().types());
	}

	public static CellContents getContents(ItemStack stack)
	{
		return decode(stack.getTag()==null? null: stack.getTag().get(CONTENTS));
	}

	private static CellContents decode(@Nullable Tag stored)
	{
		return stored==null? CellContents.EMPTY
				: CellContents.CODEC.parse(NbtOps.INSTANCE, stored).result().orElse(CellContents.EMPTY);
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
		if(amount<=0||key.getItem() instanceof EraDataCell)
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
			stack.removeTagKey(CONTENTS);
		else
			CellContents.CODEC.encodeStart(NbtOps.INSTANCE, CellContents.of(amounts)).result()
					.ifPresent(encoded -> stack.getOrCreateTag().put(CONTENTS, encoded));
		onChanged.run();
	}
}
