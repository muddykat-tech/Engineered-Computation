package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class PatternFloppyMenu extends AbstractContainerMenu
{
	public static final int INPUTS_X = 30;
	public static final int INPUTS_Y = 20;
	public static final int OUTPUTS_X = 120;
	public static final int OUTPUTS_Y = 20;
	public static final int TAG_Y = 78;
	public static final int INVENTORY_Y = 110;
	private static final int HIDDEN = -10000;

	private final Player player;
	private final InteractionHand hand;

	public PatternFloppyMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf)
	{
		this(id, inventory, buf.readEnum(InteractionHand.class));
	}

	public PatternFloppyMenu(int id, Inventory inventory, InteractionHand hand)
	{
		super(ECMenuTypes.PATTERN_FLOPPY.get(), id);
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

	public Pattern pattern()
	{
		return PatternFloppyItem.pattern(floppy());
	}

	public void edit(PatternEditPayload.Action action, int slot, int amount)
	{
		edit(action, slot, amount, "");
	}

	public void edit(PatternEditPayload.Action action, int slot, int amount, String tag)
	{
		if(!(floppy().getItem() instanceof PatternFloppyItem))
			return;
		Pattern pattern = pattern();
		boolean direct = pattern.mode()==Pattern.Mode.DIRECT;
		boolean output = slot>=Pattern.INPUTS;
		int index = output? slot-Pattern.INPUTS: slot;
		boolean validSlot = slot>=0&&index<(output? Pattern.OUTPUTS: Pattern.INPUTS);
		boolean settable = validSlot&&!(direct&&output);
		Pattern edited = switch(action)
		{
			case SET ->
			{
				ItemStack carried = getCarried();
				if(!settable||carried.isEmpty())
					yield pattern;
				int count = direct? 1: amount>0? Math.min(amount, carried.getCount()): carried.getCount();
				yield set(pattern, output, index, carried.copyWithCount(count));
			}
			case ADJUST ->
			{
				ItemStack current = output? pattern.output(index): pattern.input(index);
				if(!settable||direct||current.isEmpty())
					yield pattern;
				int count = Mth.clamp(current.getCount()+amount, 0, current.getMaxStackSize());
				yield set(pattern, output, index, count==0? ItemStack.EMPTY: current.copyWithCount(count));
			}
			case CLEAR -> settable? set(pattern, output, index, ItemStack.EMPTY): pattern;
			case SET_TAG ->
			{
				ResourceLocation id = ResourceLocation.tryParse(tag.trim().replaceFirst("^#", ""));
				yield !settable||direct||id==null? pattern:
						pattern.withTag(output, index, TagKey.create(Registries.ITEM, id));
			}
			case TOGGLE_MODE -> pattern.withMode(direct? Pattern.Mode.ABSTRACT: Pattern.Mode.DIRECT);
			case CLEAR_ALL -> new Pattern(pattern.mode(), List.of(), List.of());
		};
		if(edited.mode()==Pattern.Mode.DIRECT)
			edited = edited.withCraftedOutput(player.level());
		floppy().set(ECDataComponents.PATTERN.get(), edited);
		broadcastChanges();
	}

	public void fill(Pattern pattern)
	{
		if(!(floppy().getItem() instanceof PatternFloppyItem))
			return;
		Pattern filled = pattern.mode()==Pattern.Mode.DIRECT
				? new Pattern(Pattern.Mode.DIRECT, counted(pattern.inputs(), 1), List.of())
						.withCraftedOutput(player.level())
				: new Pattern(Pattern.Mode.ABSTRACT, counted(pattern.inputs(), 0), counted(pattern.outputs(), 0),
						pattern.inputTags(), pattern.outputTags());
		floppy().set(ECDataComponents.PATTERN.get(), filled);
		broadcastChanges();
	}

	private static List<ItemStack> counted(List<ItemStack> stacks, int count)
	{
		return stacks.stream().map(stack -> stack.isEmpty()? ItemStack.EMPTY:
				stack.copyWithCount(count>0? count: Mth.clamp(stack.getCount(), 1, stack.getMaxStackSize()))).toList();
	}

	private static Pattern set(Pattern pattern, boolean output, int index, ItemStack stack)
	{
		return output? pattern.withOutput(index, stack): pattern.withInput(index, stack);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return floppy().getItem() instanceof PatternFloppyItem;
	}
}
