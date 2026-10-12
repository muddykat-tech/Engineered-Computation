package com.muddykat.engineeredcomputation.catalogue;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.adapter.Program;
import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyItem;
import com.muddykat.engineeredcomputation.adapter.ProgrammableItem;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;

/**
 * A punch card with an orange stripe holding one filter, an item or a tag, for an Advanced Tube Station (alone or in a
 * Program Deck). Set it by right-clicking an item onto it (or it onto an item) in an inventory, or by using it in hand
 * to pick an item or type a tag (ProgramFloppyMenu, one entry). The filter is kept in its program's first entry.
 */
public class FilterCardItem extends Item implements ProgrammableItem
{
	public FilterCardItem(Properties properties)
	{
		super(properties);
	}

	public static ProgramEntry filter(ItemStack card)
	{
		return card.getItem() instanceof FilterCardItem? ProgramFloppyItem.program(card).get(0): ProgramEntry.EMPTY;
	}

	public static void setFilter(ItemStack card, ProgramEntry filter)
	{
		if(filter.isEmpty())
			ProgramFloppyItem.clearProgram(card);
		else
			ProgramFloppyItem.setProgram(card, Program.EMPTY.with(0, filter));
	}

	/** A Filter Card holding {@code filter}. */
	public static ItemStack of(ProgramEntry filter)
	{
		ItemStack card = new ItemStack(ECCatalogue.FILTER_CARD.get());
		setFilter(card, filter);
		return card;
	}

	/** Whether {@code stack} can be a card's filter: anything but a card or deck (so they still file and stack). */
	private static boolean filters(ItemStack stack)
	{
		return !stack.isEmpty()&&!(stack.getItem() instanceof FilterCardItem)&&!(stack.getItem() instanceof DeckItem);
	}

	@Override
	public int programSize()
	{
		return 1;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack card, Slot slot, ClickAction action, Player player)
	{
		if(action!=ClickAction.SECONDARY||!filters(slot.getItem()))
			return false;
		setFilter(card, ProgramEntry.of(slot.getItem()));
		player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.3F);
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack card, ItemStack other, Slot slot, ClickAction action,
			Player player, SlotAccess carried)
	{
		if(action!=ClickAction.SECONDARY||!filters(other)||!slot.allowModification(player))
			return false;
		setFilter(card, ProgramEntry.of(other));
		slot.setChanged();
		player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.3F);
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		if(player instanceof ServerPlayer serverPlayer)
			ProgrammableItem.open(serverPlayer, hand);
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag)
	{
		lines.add(describe(filter(stack)));
		if(Screen.hasShiftDown())
			lines.add(Component.translatable("tooltip.engineered_computation.filter_card.set")
					.withStyle(ChatFormatting.DARK_GRAY));
		else
			lines.add(Component.translatable("tooltip.engineered_computation.hold_shift",
					Component.translatable("tooltip.engineered_computation.shift").withStyle(ChatFormatting.GRAY))
					.withStyle(ChatFormatting.DARK_GRAY));
	}

	/** A filter as a tooltip line. */
	static Component describe(ProgramEntry filter)
	{
		if(filter.item().isPresent())
			return filter.item().get().value().getDescription().copy().withStyle(ChatFormatting.GRAY);
		if(filter.tag().isPresent())
			return Component.literal("#"+filter.tag().get().location()).withStyle(ChatFormatting.DARK_AQUA);
		return Component.translatable("tooltip.engineered_computation.filter_card.blank")
				.withStyle(ChatFormatting.GRAY);
	}
}
