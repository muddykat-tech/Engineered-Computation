package com.muddykat.engineeredcomputation.storage.cell;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public enum CellStatus
{
	EMPTY,
	SPACE,
	NEARLY_FULL,
	FULL;

	private static final int BITS = 2;

	public static CellStatus of(ItemStack stack)
	{
		CellInventory cell = CellInventory.peek(stack);
		if(cell==null)
			return EMPTY;
		boolean bytes = cell.isBytesFull(), types = cell.isTypesFull();
		return bytes&&types? FULL: bytes||types? NEARLY_FULL: SPACE;
	}

	public static int pack(IItemHandler slots)
	{
		int packed = 0;
		for(int slot = 0; slot<slots.getSlots(); slot++)
			packed |= of(slots.getStackInSlot(slot)).ordinal()<<slot*BITS;
		return packed;
	}

	public static CellStatus unpack(int packed, int slot)
	{
		return values()[packed>>slot*BITS&(1<<BITS)-1];
	}
}
