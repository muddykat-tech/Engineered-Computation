package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CardDeck(List<CatalogueCard> cards)
{
	public static final CardDeck EMPTY = new CardDeck(List.of());

	public static final Codec<CardDeck> CODEC = CatalogueCard.CODEC.listOf().xmap(CardDeck::new, CardDeck::cards);
	public static final StreamCodec<RegistryFriendlyByteBuf, CardDeck> STREAM_CODEC =
			CatalogueCard.STREAM_CODEC.apply(ByteBufCodecs.list()).map(CardDeck::new, CardDeck::cards);

	public CardDeck
	{
		cards = List.copyOf(cards);
	}

	public int size()
	{
		return cards.size();
	}

	public boolean isEmpty()
	{
		return cards.isEmpty();
	}

	public CardDeck with(CatalogueCard card)
	{
		List<CatalogueCard> more = new ArrayList<>(cards);
		more.add(card);
		return new CardDeck(more);
	}

	public CardDeck withoutLast()
	{
		return new CardDeck(cards.subList(0, cards.size()-1));
	}

	public CardDeck with(int index, CatalogueCard card)
	{
		List<CatalogueCard> changed = new ArrayList<>(cards);
		changed.set(index, card);
		return new CardDeck(changed);
	}
}
