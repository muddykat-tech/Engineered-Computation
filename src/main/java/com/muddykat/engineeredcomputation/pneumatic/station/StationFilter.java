package com.muddykat.engineeredcomputation.pneumatic.station;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.catalogue.deck.ProgramDeckItem;
import com.muddykat.engineeredcomputation.program.ProgramEntry;

/** Applies to every deposit, even items the station already holds. */
public record StationFilter(List<ProgramEntry> filters, boolean blocking)
{
	@Nullable
	public static StationFilter of(ItemStack card, boolean blocking)
	{
		if(card.getItem() instanceof FilterCardItem)
			return new StationFilter(List.of(FilterCardItem.filter(card)), blocking);
		if(card.getItem() instanceof ProgramDeckItem)
			return new StationFilter(ProgramDeckItem.filters(card), blocking);
		return null;
	}

	public static boolean isCard(ItemStack stack)
	{
		return stack.getItem() instanceof FilterCardItem||stack.getItem() instanceof ProgramDeckItem;
	}

	public boolean allows(ItemStack stack)
	{
		return filters.stream().anyMatch(filter -> filter.matches(stack))!=blocking;
	}
}
