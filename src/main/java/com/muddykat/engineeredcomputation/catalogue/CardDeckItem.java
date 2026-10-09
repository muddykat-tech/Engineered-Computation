package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;

public class CardDeckItem extends Item
{
	private static final int BAR_COLOUR = 0x6A8FCF;
	private static final int LISTED = 6;

	private final int capacity;

	public CardDeckItem(Properties properties, int capacity)
	{
		super(properties);
		this.capacity = capacity;
	}

	public int getCapacity()
	{
		return capacity;
	}

	public static CardDeck deck(ItemStack stack)
	{
		return stack.getItem() instanceof CardDeckItem? stack.getOrDefault(ECDataComponents.CARD_DECK.get(), CardDeck.EMPTY)
				: CardDeck.EMPTY;
	}

	public static void setDeck(ItemStack stack, CardDeck deck)
	{
		if(deck.isEmpty())
			stack.remove(ECDataComponents.CARD_DECK.get());
		else
			stack.set(ECDataComponents.CARD_DECK.get(), deck);
	}

	private boolean file(ItemStack deckStack, ItemStack cardStack)
	{
		CatalogueCard card = CatalogueCardItem.card(cardStack);
		CardDeck deck = deck(deckStack);
		if(card==null||deck.size()>=capacity)
			return false;
		setDeck(deckStack, deck.with(card));
		cardStack.shrink(1);
		return true;
	}

	private static ItemStack takeLast(ItemStack deckStack)
	{
		CardDeck deck = deck(deckStack);
		if(deck.isEmpty())
			return ItemStack.EMPTY;
		setDeck(deckStack, deck.withoutLast());
		return CatalogueCardItem.punched(deck.cards().get(deck.size()-1));
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack deckStack, Slot slot, ClickAction action, Player player)
	{
		if(action!=ClickAction.SECONDARY)
			return false;
		ItemStack other = slot.getItem();
		if(other.isEmpty())
		{
			ItemStack card = takeLast(deckStack);
			if(!card.isEmpty())
			{
				slot.safeInsert(card);
				player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.1F);
			}
		}
		else if(slot.allowModification(player)&&CatalogueCardItem.card(other)!=null&&file(deckStack, other))
		{
			slot.setChanged();
			player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F);
		}
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack deckStack, ItemStack other, Slot slot, ClickAction action,
			Player player, SlotAccess carried)
	{
		if(action!=ClickAction.SECONDARY||!slot.allowModification(player))
			return false;
		if(other.isEmpty())
		{
			ItemStack card = takeLast(deckStack);
			if(card.isEmpty())
				return false;
			carried.set(card);
			player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.1F);
			return true;
		}
		if(CatalogueCardItem.card(other)==null)
			return false;
		if(file(deckStack, other))
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
	public InteractionResult useOn(UseOnContext context)
	{
		Player player = context.getPlayer();
		if(player==null||!player.isSecondaryUseActive()
				||!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MultiblockPartBlockEntity part))
			return InteractionResult.PASS;
		return EraDriveState.insertIntoBay(part, player, context.getHand(), context.getClickLocation());
	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return !deck(stack).isEmpty();
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		return Math.min(13, Mth.ceil(13F*deck(stack).size()/capacity));
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		return BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		CardDeck deck = deck(stack);
		lines.add(Component.translatable("tooltip.engineered_computation.card_deck.cards", deck.size(), capacity)
				.withStyle(ChatFormatting.GRAY));
		for(int i = 0; i<Math.min(LISTED, deck.size()); i++)
			lines.add(Component.literal(" ").append(deck.cards().get(i).label()).withStyle(ChatFormatting.DARK_AQUA));
		if(deck.size()>LISTED)
			lines.add(Component.translatable("tooltip.engineered_computation.card_deck.more", deck.size()-LISTED)
					.withStyle(ChatFormatting.DARK_GRAY));
	}
}
