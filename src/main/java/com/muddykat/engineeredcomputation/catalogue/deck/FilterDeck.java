package com.muddykat.engineeredcomputation.catalogue.deck;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.muddykat.engineeredcomputation.program.ProgramEntry;

public record FilterDeck(List<ProgramEntry> cards)
{
	public static final FilterDeck EMPTY = new FilterDeck(List.of());

	public static final Codec<FilterDeck> CODEC = ProgramEntry.CODEC.listOf(0, ProgramDeckItem.CAPACITY)
			.xmap(FilterDeck::new, FilterDeck::cards);
	public static final StreamCodec<RegistryFriendlyByteBuf, FilterDeck> STREAM_CODEC =
			ProgramEntry.STREAM_CODEC.apply(ByteBufCodecs.list(ProgramDeckItem.CAPACITY)).map(FilterDeck::new,
					FilterDeck::cards);

	public FilterDeck
	{
		cards = List.copyOf(cards);
	}

	public boolean isEmpty()
	{
		return cards.isEmpty();
	}
}
