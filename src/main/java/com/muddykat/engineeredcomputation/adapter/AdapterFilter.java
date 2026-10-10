package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;

/**
 * What an adapter moves: with an allow list, what the held item (or Program Floppy) matches; with a block list
 * ({@code blocking}), everything else - so an empty block list blocks nothing.
 */
public record AdapterFilter(ItemStack held, boolean blocking)
{
	public static boolean isProgram(ItemStack stack)
	{
		return stack.getItem() instanceof ProgramFloppyItem;
	}

	public static boolean accepts(ItemStack floppy, ComputerEra era)
	{
		return isProgram(floppy)&&takesPrograms(era);
	}

	public static boolean takesPrograms(ComputerEra era)
	{
		return era==ComputerEra.FLOPPY;
	}

	public boolean isEmpty()
	{
		return held.isEmpty()||isProgram(held)&&program().isEmpty();
	}

	public boolean allows(ItemStack stack)
	{
		return matches(stack)!=blocking;
	}

	private boolean matches(ItemStack stack)
	{
		if(held.isEmpty())
			return false;
		return isProgram(held)? program().matches(stack): stack.is(held.getItem());
	}

	private Program program()
	{
		return held.getOrDefault(ECDataComponents.PROGRAM.get(), Program.EMPTY);
	}
}
