package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

/** Edits the held {@link ProgrammableItem}'s entries: a Program Floppy's or a Filter Card's. */
public class ProgramFloppyMenu extends AbstractContainerMenu
{
	public static final int ENTRIES_X = 8;
	public static final int ENTRIES_Y = 20;
	public static final int INVENTORY_Y = 84;
	private static final int HIDDEN = -10000;

	private final Player player;
	private final InteractionHand hand;

	public ProgramFloppyMenu(int id, Inventory inventory, FriendlyByteBuf buf)
	{
		this(id, inventory, buf.readEnum(InteractionHand.class));
	}

	public ProgramFloppyMenu(int id, Inventory inventory, InteractionHand hand)
	{
		super(ECMenuTypes.PROGRAM_FLOPPY.get(), id);
		this.player = inventory.player;
		this.hand = hand;
		int heldSlot = hand==InteractionHand.MAIN_HAND? inventory.selected: Inventory.SLOT_OFFHAND;
		for(int row = 0; row<3; row++)
			for(int col = 0; col<9; col++)
				addSlot(slot(inventory, col+row*9+9, 8+col*18, INVENTORY_Y+row*18, heldSlot));
		for(int col = 0; col<9; col++)
			addSlot(slot(inventory, col, 8+col*18, INVENTORY_Y+58, heldSlot));
		addSlot(slot(inventory, Inventory.SLOT_OFFHAND, HIDDEN, HIDDEN, heldSlot));
	}

	private static Slot slot(Inventory inventory, int index, int x, int y, int heldSlot)
	{
		return new Slot(inventory, index, x, y)
		{
			@Override
			public boolean mayPickup(Player player)
			{
				return index!=heldSlot;
			}

			@Override
			public boolean isActive()
			{
				return x!=HIDDEN;
			}
		};
	}

	public ItemStack floppy()
	{
		return player.getItemInHand(hand);
	}

	public Program program()
	{
		return ProgramFloppyItem.program(floppy());
	}

	/** How many entries the held item has. */
	public int size()
	{
		return floppy().getItem() instanceof ProgrammableItem item? item.programSize(): 0;
	}

	void edit(int index, ProgramEditPayload.Action action, String tag, ItemStack item)
	{
		if(index<0||index>=size())
			return;
		ProgramEntry entry = switch(action)
		{
			case SET_ITEM -> ProgramEntry.of(item.isEmpty()? getCarried(): item);
			case SET_TAG ->
			{
				ResourceLocation id = ResourceLocation.tryParse(tag.trim().replaceFirst("^#", ""));
				yield id==null? null: ProgramEntry.of(TagKey.create(Registries.ITEM, id));
			}
			case CLEAR -> ProgramEntry.EMPTY;
		};
		if(entry!=null)
			ProgramFloppyItem.setProgram(floppy(), program().with(index, entry));
		broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return floppy().getItem() instanceof ProgrammableItem;
	}
}
