package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;

/** A deck of Catalogue Cards: one per Tube Station, filed into the Card Catalogue's bays. */
public class CardDeckItem extends DeckItem
{
	private static final String DECK = "card_deck";
	private static final int BAR_COLOUR = 0x6A8FCF;
	private static final int LISTED = 6;

	public CardDeckItem(Properties properties, int capacity)
	{
		super(properties, capacity);
	}

	public static CardDeck deck(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		return !(stack.getItem() instanceof CardDeckItem)||tag==null||!tag.contains(DECK)? CardDeck.EMPTY
				: CardDeck.CODEC.parse(NbtOps.INSTANCE, tag.get(DECK)).result().orElse(CardDeck.EMPTY);
	}

	public static void setDeck(ItemStack stack, CardDeck deck)
	{
		if(deck.isEmpty())
			stack.removeTagKey(DECK);
		else
			CardDeck.CODEC.encodeStart(NbtOps.INSTANCE, deck).result()
					.ifPresent(encoded -> stack.getOrCreateTag().put(DECK, encoded));
	}

	@Override
	public boolean isCard(ItemStack stack)
	{
		return CatalogueCardItem.card(stack)!=null;
	}

	@Override
	public List<ItemStack> cards(ItemStack deck)
	{
		return deck(deck).cards().stream().map(CatalogueCardItem::punched).toList();
	}

	@Override
	public void setCards(ItemStack deck, List<ItemStack> cards)
	{
		setDeck(deck, new CardDeck(cards.stream().map(CatalogueCardItem::card).filter(Objects::nonNull).toList()));
	}

	@Override
	protected int barColour()
	{
		return BAR_COLOUR;
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
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		CardDeck deck = deck(stack);
		lines.add(Component.translatable("tooltip.engineered_computation.card_deck.cards", deck.size(), getCapacity())
				.withStyle(ChatFormatting.GRAY));
		for(int i = 0; i<Math.min(LISTED, deck.size()); i++)
			lines.add(Component.literal(" ").append(deck.cards().get(i).label()).withStyle(ChatFormatting.DARK_AQUA));
		if(deck.size()>LISTED)
			lines.add(Component.translatable("tooltip.engineered_computation.card_deck.more", deck.size()-LISTED)
					.withStyle(ChatFormatting.DARK_GRAY));
	}
}
