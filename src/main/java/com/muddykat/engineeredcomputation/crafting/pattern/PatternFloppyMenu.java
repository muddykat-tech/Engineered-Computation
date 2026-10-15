package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

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
		boolean settable = validSlot&&!((direct||pattern.mode()==Pattern.Mode.SEND)&&output);
		Pattern edited = switch(action)
		{
			case SET ->
			{
				ItemStack carried = getCarried();
				if(direct&&output&&validSlot&&!carried.isEmpty())
					yield recipeFor(pattern, carried);
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
			case TOGGLE_MODE -> pattern.withMode(Pattern.Mode.values()[(pattern.mode().ordinal()+1)
					%Pattern.Mode.values().length]);
			case CLEAR_ALL -> new Pattern(pattern.mode(), List.of(), List.of());
			case TOGGLE_FUZZY_INPUTS -> pattern.withFuzzy(true, !pattern.fuzzyInputs());
			case TOGGLE_FUZZY_OUTPUTS -> pattern.withFuzzy(false, !pattern.fuzzyOutputs());
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
		if(pattern.mode()==Pattern.Mode.DIRECT&&pattern.firstInput()<0&&pattern.primaryIndex()>=0)
		{
			floppy().set(ECDataComponents.PATTERN.get(), recipeFor(pattern(), pattern.primaryOutput()));
			broadcastChanges();
			return;
		}
		Pattern filled = pattern.mode()==Pattern.Mode.DIRECT
				? new Pattern(Pattern.Mode.DIRECT, counted(pattern.inputs(), 1), List.of())
						.withCraftedOutput(player.level())
				: new Pattern(Pattern.Mode.ABSTRACT, counted(pattern.inputs(), 0), counted(pattern.outputs(), 0),
						pattern.inputTags(), pattern.outputTags());
		floppy().set(ECDataComponents.PATTERN.get(), filled);
		broadcastChanges();
	}

	/**
	 * A crafting recipe for the item laid out on the grid, or the pattern unchanged if there's none. Asking again for
	 * the same item steps to its next recipe.
	 */
	private Pattern recipeFor(Pattern current, ItemStack wanted)
	{
		Level level = player.level();
		List<RecipeHolder<CraftingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)
				.stream().filter(holder -> ItemStack.isSameItem(holder.value().getResultItem(level.registryAccess()), wanted))
				.sorted(Comparator.comparing(holder -> holder.id().toString())).toList();
		int at = current.mode()==Pattern.Mode.DIRECT? current.recipe(level).map(recipes::indexOf).orElse(-1): -1;
		for(int step = 1; step<=recipes.size(); step++)
		{
			Pattern laid = new Pattern(Pattern.Mode.DIRECT, laidOut(recipes.get((at+step)%recipes.size()).value()),
					List.of(), Map.of(), Map.of(), current.fuzzyInputs(), current.fuzzyOutputs()).withCraftedOutput(level);
			if(ItemStack.isSameItem(laid.primaryOutput(), wanted))
				return laid;
		}
		return current;
	}

	private static List<ItemStack> laidOut(CraftingRecipe recipe)
	{
		List<ItemStack> grid = new ArrayList<>(Collections.nCopies(Pattern.INPUTS, ItemStack.EMPTY));
		List<Ingredient> ingredients = recipe.getIngredients();
		if(recipe instanceof ShapedRecipe shaped)
		{
			for(int row = 0; row<shaped.getHeight()&&row<3; row++)
				for(int col = 0; col<shaped.getWidth()&&col<3; col++)
					grid.set(row*3+col, first(ingredients.get(row*shaped.getWidth()+col)));
		}
		else
			for(int index = 0; index<ingredients.size()&&index<Pattern.INPUTS; index++)
				grid.set(index, first(ingredients.get(index)));
		return grid;
	}

	private static ItemStack first(Ingredient ingredient)
	{
		ItemStack[] items = ingredient.getItems();
		return items.length==0? ItemStack.EMPTY: items[0].copyWithCount(1);
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
