package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;


public record CardDeck(List<CatalogueCard> cards)
{
	public static final CardDeck EMPTY = new CardDeck(List.of());

	public static final Codec<CardDeck> CODEC = CatalogueCard.CODEC.listOf().xmap(CardDeck::new, CardDeck::cards);
	public CardDeck
	{
		cards = List.copyOf(cards);
	}

	public static CardDeck read(FriendlyByteBuf buf)
	{
		return new CardDeck(buf.readList(CatalogueCard::read));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeCollection(cards, (out, card) -> card.write(out));
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
