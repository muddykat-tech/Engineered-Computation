package com.muddykat.engineeredcomputation.storage.cell;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECSounds;

/** Bay slots that remember what each one held, so a change can be heard as a cell going in or coming out. */
public abstract class CellBaySlots extends ItemStackHandler
{
	private Item[] held;

	protected CellBaySlots(int slots)
	{
		super(slots);
		held = new Item[slots];
	}

	@Override
	protected void onLoad()
	{
		held = new Item[getSlots()];
		for(int slot = 0; slot<getSlots(); slot++)
			held[slot] = getStackInSlot(slot).isEmpty()? null: getStackInSlot(slot).getItem();
	}

	@Override
	protected final void onContentsChanged(int slot)
	{
		ItemStack now = getStackInSlot(slot);
		Item before = held[slot];
		held[slot] = now.isEmpty()? null: now.getItem();
		if(!now.isEmpty()&&now.getItem()!=before)
			cellMoved(now, true);
		else if(now.isEmpty()&&before!=null)
			cellMoved(new ItemStack(before), false);
		contentsChanged(slot);
	}

	protected abstract void contentsChanged(int slot);

	protected void cellMoved(ItemStack cell, boolean inserted)
	{
	}

	public static void play(@Nullable Level level, BlockPos pos, ItemStack cell, boolean inserted)
	{
		SoundEvent sound = soundOf(cell, inserted);
		if(level==null||level.isClientSide||sound==null)
			return;
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 0.95F+level.random.nextFloat()*0.1F);
	}

	/** Only floppy disks have sounds so far; other cells go in and out silently. */
	@Nullable
	private static SoundEvent soundOf(ItemStack cell, boolean inserted)
	{
		if(!(cell.getItem() instanceof EraDataCell type)||type.getEra()!=ComputerEra.FLOPPY)
			return null;
		if(type.isTerminalDisk())
			return (inserted? ECSounds.FLOPPY_35_INSERT: ECSounds.FLOPPY_35_EJECT).get();
		return (inserted? ECSounds.FLOPPY_525_INSERT: ECSounds.FLOPPY_525_EJECT).get();
	}
}
