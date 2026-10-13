package com.muddykat.engineeredcomputation.catalogue.deck;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class CardDeckMenu extends AbstractContainerMenu
{
	public static final int COLUMNS = 8;
	public static final int CARDS_X = 17;
	public static final int CARDS_Y = 18;

	private final Player player;
	private final InteractionHand hand;
	private final int capacity;
	@Nullable
	private final DeckItem deckItem;
	private final SimpleContainer cards;
	private final int deckSlot;

	public CardDeckMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, inventory, buf.readEnum(InteractionHand.class), buf.readVarInt(), List.of());
	}

	private CardDeckMenu(int id, Inventory inventory, InteractionHand hand, int capacity, List<ItemStack> filed)
	{
		super(ECMenuTypes.CARD_DECK.get(), id);
		this.player = inventory.player;
		this.hand = hand;
		this.capacity = capacity;
		this.deckItem = player.getItemInHand(hand).getItem() instanceof DeckItem item? item: null;
		this.cards = new SimpleContainer(capacity);
		for(int i = 0; i<filed.size()&&i<capacity; i++)
			cards.setItem(i, filed.get(i));
		if(!player.level().isClientSide)
			cards.addListener(container -> writeBack());

		for(int i = 0; i<capacity; i++)
			addSlot(new Slot(cards, i, CARDS_X+1+(i%COLUMNS)*18, CARDS_Y+1+(i/COLUMNS)*18)
			{
				@Override
				public boolean mayPlace(ItemStack stack)
				{
					return isCard(stack);
				}

				@Override
				public int getMaxStackSize()
				{
					return 1;
				}
			});
		int inventoryY = inventoryY(capacity);
		int held = hand==InteractionHand.MAIN_HAND? inventory.selected: -1;
		int deck = -1;
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(new Slot(inventory, col+row*9+9, 8+col*18, inventoryY+row*18));
		for(int col = 0; col<9; col++)
		{
			if(col==held)
			{
				deck = slots.size();
				addSlot(new Slot(inventory, col, 8+col*18, inventoryY+58)
				{
					@Override
					public boolean mayPickup(Player player)
					{
						return false;
					}

					@Override
					public boolean mayPlace(ItemStack stack)
					{
						return false;
					}
				});
			}
			else
				addSlot(new Slot(inventory, col, 8+col*18, inventoryY+58));
		}
		this.deckSlot = deck;
	}

	public static void open(ServerPlayer player, InteractionHand hand)
	{
		ItemStack deck = player.getItemInHand(hand);
		if(!(deck.getItem() instanceof DeckItem item))
			return;
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CardDeckMenu(id, inventory, hand,
				item.getCapacity(), item.cards(deck)), deck.getHoverName()), buf -> {
			buf.writeEnum(hand);
			buf.writeVarInt(item.getCapacity());
		});
	}

	public static int inventoryY(int capacity)
	{
		return CARDS_Y+rows(capacity)*18+14;
	}

	public static int rows(int capacity)
	{
		return (capacity+COLUMNS-1)/COLUMNS;
	}

	public int getCapacity()
	{
		return capacity;
	}

	private void writeBack()
	{
		ItemStack deck = player.getItemInHand(hand);
		if(!(deck.getItem() instanceof DeckItem item))
			return;
		List<ItemStack> filed = new ArrayList<>();
		for(int i = 0; i<cards.getContainerSize(); i++)
			if(item.isCard(cards.getItem(i)))
				filed.add(cards.getItem(i));
		item.setCards(deck, filed);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player)
	{
		if(clickType==ClickType.SWAP&&(button==player.getInventory().selected&&hand==InteractionHand.MAIN_HAND
				||button==Inventory.SLOT_OFFHAND&&hand==InteractionHand.OFF_HAND))
			return;
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(!slot.hasItem()||index==deckSlot)
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved = index<capacity? moveItemStackTo(stack, capacity, slots.size(), true)
				: isCard(stack)&&moveItemStackTo(stack, 0, capacity, false);
		if(!moved)
			return ItemStack.EMPTY;
		if(stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.getItemInHand(hand).getItem() instanceof DeckItem;
	}

	private boolean isCard(ItemStack stack)
	{
		return deckItem!=null&&deckItem.isCard(stack);
	}
}
