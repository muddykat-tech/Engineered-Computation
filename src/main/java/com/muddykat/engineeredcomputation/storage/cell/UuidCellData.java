package com.muddykat.engineeredcomputation.storage.cell;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

record UuidCellData(ItemStack stack, @Nullable CellStorage storage) implements CellData
{
	boolean readable()
	{
		return storage!=null;
	}

	@Nullable
	private CellStorage.Cell cell()
	{
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		return id==null||storage==null? null: storage.find(id);
	}

	@Override
	public long amount(ItemKey key)
	{
		CellStorage.Cell cell = cell();
		return cell==null? 0: cell.amount(key);
	}

	@Override
	public int types()
	{
		CellStorage.Cell cell = cell();
		return cell==null? 0: cell.types();
	}

	@Override
	public long totalCount()
	{
		CellStorage.Cell cell = cell();
		return cell==null? 0: cell.totalCount();
	}

	@Override
	public void collect(Map<ItemKey, Long> into)
	{
		CellStorage.Cell cell = cell();
		if(cell!=null)
			cell.collect(into);
	}

	@Override
	public void set(ItemKey key, long amount)
	{
		if(storage==null)
			throw new IllegalStateException("UUID cell contents only exist on the server");
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		if(id==null)
		{
			id = UUID.randomUUID();
			stack.set(ECDataComponents.CELL_UUID.get(), id);
		}
		storage.set(id, key, amount);
	}

	@Override
	public CellContents snapshot()
	{
		CellStorage.Cell cell = cell();
		return cell==null? CellContents.EMPTY: cell.snapshot();
	}
}
