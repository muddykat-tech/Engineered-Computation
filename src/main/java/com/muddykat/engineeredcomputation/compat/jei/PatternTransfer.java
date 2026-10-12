package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.PatternFillPayload;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyMenu;
import com.muddykat.engineeredcomputation.network.ECPayloads;

/**
 * JEI's [+] on any recipe, into the held pattern floppy (PatternFloppyMenu.fill). A crafting recipe becomes a
 * crafting pattern, laid out as JEI shows it; anything else a processing pattern of its item inputs and outputs, with
 * counts, the same items merged into one slot, and an ingredient that is a whole tag's items kept as that tag.
 */
final class PatternTransfer implements IRecipeTransferHandler<PatternFloppyMenu, Object>
{
	private final IRecipeTransferHandlerHelper helper;

	PatternTransfer(IRecipeTransferHandlerHelper helper)
	{
		this.helper = helper;
	}

	@Override
	public Class<PatternFloppyMenu> getContainerClass()
	{
		return PatternFloppyMenu.class;
	}

	@Override
	public Class<Object> getRecipeClass()
	{
		return Object.class;
	}

	@Override
	@Nullable
	public IRecipeTransferError transferRecipe(PatternFloppyMenu menu, Object recipe, IRecipeSlotsView recipeSlots,
			Player player, boolean maxTransfer, boolean doTransfer)
	{
		boolean crafting = recipe instanceof CraftingRecipe;
		Pattern pattern = crafting? crafting(recipeSlots): processing(recipeSlots);
		if(pattern==null)
			return helper.createUserErrorWithTooltip(
					new TranslatableComponent("jei.engineered_computation.pattern.too_many", Pattern.INPUTS));
		if(crafting? pattern.isEmpty(): !pattern.isValid())
			return helper.createUserErrorWithTooltip(
					new TranslatableComponent("jei.engineered_computation.pattern.no_items"));
		if(doTransfer)
			ECPayloads.sendToServer(new PatternFillPayload(menu.containerId, pattern));
		return null;
	}

	/** JEI's crafting grid, slot for slot; the server works out the output. */
	@Nullable
	private static Pattern crafting(IRecipeSlotsView recipeSlots)
	{
		List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		if(inputs.size()>Pattern.INPUTS)
			return null;
		return new Pattern(Pattern.Mode.DIRECT, inputs.stream().map(PatternTransfer::shown).toList(), List.of());
	}

	@Nullable
	private static Pattern processing(IRecipeSlotsView recipeSlots)
	{
		Slots inputs = Slots.of(recipeSlots.getSlotViews(RecipeIngredientRole.INPUT));
		Slots outputs = Slots.of(recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT));
		if(inputs.stacks().size()>Pattern.INPUTS)
			return null;
		// Outputs past the pattern's last are a recipe's extras (chance byproducts and the like): left off.
		return new Pattern(Pattern.Mode.ABSTRACT, inputs.stacks(), outputs.stacks(), inputs.tags(), outputs.tags());
	}

	/** The item JEI shows in a slot right now (so a cycling ingredient lands as what the player saw), if any. */
	static ItemStack shown(IRecipeSlotView slot)
	{
		return slot.getDisplayedItemStack().or(() -> slot.getItemStacks().findFirst()).orElse(ItemStack.EMPTY).copy();
	}

	/**
	 * The tag a slot stands for: JEI 10 doesn't say which tag a slot shows, so it's a tag holding exactly the slot's
	 * items (more than one of them), if there is one.
	 */
	private static Optional<TagKey<Item>> tagOf(IRecipeSlotView slot)
	{
		List<Item> items = slot.getItemStacks().map(ItemStack::getItem).distinct().toList();
		if(items.size()<2)
			return Optional.empty();
		return items.get(0).builtInRegistryHolder().tags()
				.filter(tag -> Registry.ITEM.getTag(tag).map(set -> set.size()==items.size()).orElse(false))
				.filter(tag -> items.stream().allMatch(item -> item.builtInRegistryHolder().is(tag)))
				.findFirst();
	}

	/** Slots' items, the same item (and tag) merged into one with the counts added up. */
	private record Slots(List<ItemStack> stacks, Map<Integer, TagKey<Item>> tags)
	{
		static Slots of(List<IRecipeSlotView> views)
		{
			List<ItemStack> stacks = new ArrayList<>();
			Map<Integer, TagKey<Item>> tags = new HashMap<>();
			for(IRecipeSlotView view : views)
			{
				ItemStack stack = shown(view);
				if(stack.isEmpty())
					continue;
				TagKey<Item> tag = tagOf(view).orElse(null);
				int same = -1;
				for(int i = 0; i<stacks.size()&&same<0; i++)
					if(ItemStack.isSameItemSameTags(stacks.get(i), stack)&&Objects.equals(tags.get(i), tag))
						same = i;
				if(same>=0)
					stacks.get(same).grow(stack.getCount());
				else
				{
					if(tag!=null)
						tags.put(stacks.size(), tag);
					stacks.add(stack);
				}
			}
			return new Slots(stacks, tags);
		}
	}
}
