package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;

/** A deck of up to eight Filter Cards: an Advanced Tube Station with it takes what any of them match. */
public class ProgramDeckItem extends DeckItem
{
	public static final int CAPACITY = 8;
	private static final int BAR_COLOUR = 0xE08A2E;

	public ProgramDeckItem(Properties properties)
	{
		super(properties, CAPACITY);
	}

	/** The deck's cards' filters. */
	public static List<ProgramEntry> filters(ItemStack deck)
	{
		return deck.getItem() instanceof ProgramDeckItem? deck.getOrDefault(ECDataComponents.FILTER_DECK.get(),
				FilterDeck.EMPTY).cards(): List.of();
	}

	@Override
	public boolean isCard(ItemStack stack)
	{
		return stack.getItem() instanceof FilterCardItem;
	}

	@Override
	public List<ItemStack> cards(ItemStack deck)
	{
		return filters(deck).stream().map(FilterCardItem::of).toList();
	}

	@Override
	public void setCards(ItemStack deck, List<ItemStack> cards)
	{
		if(cards.isEmpty())
			deck.remove(ECDataComponents.FILTER_DECK.get());
		else
			deck.set(ECDataComponents.FILTER_DECK.get(), new FilterDeck(cards.stream().map(FilterCardItem::filter)
					.toList()));
	}

	@Override
	protected int barColour()
	{
		return BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		List<ProgramEntry> filters = filters(stack);
		lines.add(Component.translatable("tooltip.engineered_computation.program_deck.cards", filters.size(), CAPACITY)
				.withStyle(ChatFormatting.GRAY));
		for(ProgramEntry filter : filters)
			lines.add(Component.literal(" ").append(FilterCardItem.describe(filter)));
	}
}
