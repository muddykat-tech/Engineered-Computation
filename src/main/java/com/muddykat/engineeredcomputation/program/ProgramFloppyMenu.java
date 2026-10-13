package com.muddykat.engineeredcomputation.program;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class ProgramFloppyMenu extends AbstractContainerMenu implements ProgramEditor
{
	public static final int ENTRIES_X = 8;
	public static final int ENTRIES_Y = 20;
	public static final int INVENTORY_Y = 84;
	private static final int HIDDEN = -10000;

	private final Player player;
	private final InteractionHand hand;

	public ProgramFloppyMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
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

	public static void openHeld(ServerPlayer player, InteractionHand hand)
	{
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ProgramFloppyMenu(id, inventory, hand),
				player.getItemInHand(hand).getHoverName()), buf -> buf.writeEnum(hand));
	}

	public ItemStack floppy()
	{
		return player.getItemInHand(hand);
	}

	public Program program()
	{
		return floppy().getOrDefault(ECDataComponents.PROGRAM.get(), Program.EMPTY);
	}

	public int size()
	{
		return floppy().getItem() instanceof ProgrammableItem item? item.programSize(): 0;
	}

	@Override
	public void edit(int index, ProgramEditPayload.Action action, String text, ItemStack item)
	{
		if(index<0||index>=size())
			return;
		ProgramEntry entry = ProgramEditor.entry(action, text, item.isEmpty()? getCarried(): item);
		if(entry!=null)
			floppy().set(ECDataComponents.PROGRAM.get(), program().with(index, entry));
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
