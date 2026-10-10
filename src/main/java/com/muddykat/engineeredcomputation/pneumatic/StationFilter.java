package com.muddykat.engineeredcomputation.pneumatic;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.catalogue.ProgramDeckItem;

/**
 * An Advanced Tube Station's filter: its Filter Card's or Program Deck's filters, as an allow list (take only what they
 * match) or a block list (take everything else - so an empty one blocks nothing). It decides every deposit to the
 * station, items its inventory already holds included.
 */
public record StationFilter(List<ProgramEntry> filters, boolean blocking)
{
	/** The filter {@code card} (a Filter Card or Program Deck) makes, or null if it's neither. */
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
