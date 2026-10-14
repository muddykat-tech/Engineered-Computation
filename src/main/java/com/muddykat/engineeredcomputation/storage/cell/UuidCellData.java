package com.muddykat.engineeredcomputation.storage.cell;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

record UuidCellData(ItemStack stack, CellStorage storage) implements CellData
{
	/**
	 * Moves contents saved on the stack by older versions into cell storage. Only done where the stack's holder is told
	 * to save, or the world could keep the old stack and migrate it again under a new id.
	 */
	static void migrate(ItemStack stack, CellStorage storage)
	{
		CellContents contents = stack.get(ECDataComponents.CELL_CONTENTS.get());
		stack.remove(ECDataComponents.CELL_CONTENTS.get());
		if(contents==null||contents.types()==0)
			return;
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		if(id==null)
			id = UUID.randomUUID();
		storage.setContents(id, contents, stack.getItem());
		stack.set(ECDataComponents.CELL_UUID.get(), id);
		stack.set(ECDataComponents.CELL_SUMMARY.get(), new CellSummary(contents.types(), contents.totalCount()));
	}

	@Nullable
	private CellStorage.Cell cell()
	{
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		return id==null? null: storage.find(id);
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

	/** An emptied cell drops its id so it stacks and compares equal to a new one again. */
	@Override
	public void set(ItemKey key, long amount)
	{
		UUID id = stack.get(ECDataComponents.CELL_UUID.get());
		if(id==null)
		{
			id = UUID.randomUUID();
			stack.set(ECDataComponents.CELL_UUID.get(), id);
		}
		storage.set(id, key, amount, stack.getItem());
		CellStorage.Cell cell = storage.find(id);
		if(cell==null)
		{
			stack.remove(ECDataComponents.CELL_UUID.get());
			stack.remove(ECDataComponents.CELL_SUMMARY.get());
		}
		else
			stack.set(ECDataComponents.CELL_SUMMARY.get(), new CellSummary(cell.types(), cell.totalCount()));
	}

	@Override
	public CellContents snapshot()
	{
		CellStorage.Cell cell = cell();
		return cell==null? CellContents.EMPTY: cell.snapshot();
	}
}
