package com.muddykat.engineeredcomputation.registration;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.catalogue.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;

public final class ECCatalogue
{
	private static final int SMALLEST_DECK = 8;
	public static final int DECK_TIERS = 4;

	public static final DeferredItem<CatalogueCardItem> CARD = ECRegistrationHolder.ITEMS.register("catalogue_card",
			() -> new CatalogueCardItem(new Item.Properties()));

	private static final List<DeferredItem<CardDeckItem>> DECKS = IntStream.range(0, DECK_TIERS)
			.mapToObj(tier -> SMALLEST_DECK<<tier)
			.map(capacity -> ECRegistrationHolder.ITEMS.register("card_deck_"+capacity,
					() -> new CardDeckItem(new Item.Properties().stacksTo(1), capacity)))
			.toList();

	private ECCatalogue()
	{
	}

	public static void init()
	{
	}

	public static List<DeferredItem<CardDeckItem>> decks()
	{
		return DECKS;
	}

	public static int tierOf(Item item)
	{
		for(int tier = 0; tier<DECKS.size(); tier++)
			if(item==DECKS.get(tier).get())
				return tier;
		return -1;
	}
}
