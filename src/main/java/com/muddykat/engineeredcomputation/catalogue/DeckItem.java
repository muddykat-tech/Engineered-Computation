package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A deck of punch cards in one item: Catalogue Cards (CardDeckItem) or Filter Cards (ProgramDeckItem). Cards are filed
 * bundle-style - right-click a card onto the deck, or the deck onto a card; right-click it with an empty hand to take
 * the last one out - and laid out by using the deck in hand (CardDeckMenu).
 */
public abstract class DeckItem extends Item
{
	private final int capacity;

	protected DeckItem(Properties properties, int capacity)
	{
		super(properties);
		this.capacity = capacity;
	}

	public int getCapacity()
	{
		return capacity;
	}

	/** Whether {@code stack} is a card this deck files. */
	public abstract boolean isCard(ItemStack stack);

	/** The deck's cards, one item each, in filing order. */
	public abstract List<ItemStack> cards(ItemStack deck);

	public abstract void setCards(ItemStack deck, List<ItemStack> cards);

	protected abstract int barColour();

	private boolean file(ItemStack deck, ItemStack card)
	{
		List<ItemStack> cards = cards(deck);
		if(!isCard(card)||cards.size()>=capacity)
			return false;
		List<ItemStack> more = new ArrayList<>(cards);
		more.add(card.copyWithCount(1));
		setCards(deck, more);
		card.shrink(1);
		return true;
	}

	private ItemStack takeLast(ItemStack deck)
	{
		List<ItemStack> cards = cards(deck);
		if(cards.isEmpty())
			return ItemStack.EMPTY;
		setCards(deck, cards.subList(0, cards.size()-1));
		return cards.get(cards.size()-1);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack deck, Slot slot, ClickAction action, Player player)
	{
		if(action!=ClickAction.SECONDARY)
			return false;
		ItemStack other = slot.getItem();
		if(other.isEmpty())
		{
			ItemStack card = takeLast(deck);
			if(!card.isEmpty())
			{
				slot.safeInsert(card);
				player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.1F);
			}
		}
		else if(slot.allowModification(player)&&isCard(other)&&file(deck, other))
		{
			slot.setChanged();
			player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F);
		}
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack deck, ItemStack other, Slot slot, ClickAction action,
			Player player, SlotAccess carried)
	{
		if(action!=ClickAction.SECONDARY||!slot.allowModification(player))
			return false;
		if(other.isEmpty())
		{
			ItemStack card = takeLast(deck);
			if(card.isEmpty())
				return false;
			carried.set(card);
			player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.1F);
			return true;
		}
		if(!isCard(other))
			return false;
		if(file(deck, other))
			player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F);
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(player instanceof ServerPlayer serverPlayer)
			CardDeckMenu.open(serverPlayer, hand);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return !cards(stack).isEmpty();
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		return Math.min(13, Mth.ceil(13F*cards(stack).size()/capacity));
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		return barColour();
	}
}
