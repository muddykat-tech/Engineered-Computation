package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.adapter.ProgramEntry;

/** A deck of up to eight Filter Cards: an Advanced Tube Station with it takes what any of them match. */
public class ProgramDeckItem extends DeckItem
{
	public static final int CAPACITY = 8;
	private static final int BAR_COLOUR = 0xE08A2E;
	private static final String FILTER_DECK = "filter_deck";

	public ProgramDeckItem(Properties properties)
	{
		super(properties, CAPACITY);
	}

	/** The deck's cards' filters. */
	public static List<ProgramEntry> filters(ItemStack deck)
	{
		CompoundTag tag = deck.getTag();
		if(!(deck.getItem() instanceof ProgramDeckItem)||tag==null||!tag.contains(FILTER_DECK))
			return List.of();
		return FilterDeck.CODEC.parse(NbtOps.INSTANCE, tag.get(FILTER_DECK)).result().orElse(FilterDeck.EMPTY).cards();
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
			deck.removeTagKey(FILTER_DECK);
		else
			FilterDeck.CODEC.encodeStart(NbtOps.INSTANCE, new FilterDeck(cards.stream().map(FilterCardItem::filter)
					.toList())).result().ifPresent(encoded -> deck.getOrCreateTag().put(FILTER_DECK, encoded));
	}

	@Override
	protected int barColour()
	{
		return BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		List<ProgramEntry> filters = filters(stack);
		lines.add(new TranslatableComponent("tooltip.engineered_computation.program_deck.cards", filters.size(),
				CAPACITY).withStyle(ChatFormatting.GRAY));
		for(ProgramEntry filter : filters)
			lines.add(new TextComponent(" ").append(FilterCardItem.describe(filter)));
	}
}
