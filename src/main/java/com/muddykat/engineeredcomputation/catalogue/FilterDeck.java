package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;

import com.mojang.serialization.Codec;

import com.muddykat.engineeredcomputation.adapter.ProgramEntry;

/** A Program Deck's Filter Cards, each kept as its filter (all a Filter Card holds), in filing order. */
public record FilterDeck(List<ProgramEntry> cards)
{
	public static final FilterDeck EMPTY = new FilterDeck(List.of());

	public static final Codec<FilterDeck> CODEC = ProgramEntry.CODEC.listOf().xmap(FilterDeck::new, FilterDeck::cards);

	public FilterDeck
	{
		cards = List.copyOf(cards.subList(0, Math.min(ProgramDeckItem.CAPACITY, cards.size())));
	}

	public boolean isEmpty()
	{
		return cards.isEmpty();
	}
}
