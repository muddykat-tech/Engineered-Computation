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
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFillPayload;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFloppyMenu;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

final class PatternTransfer implements IUniversalRecipeTransferHandler<PatternFloppyMenu>
{
	private final IRecipeTransferHandlerHelper helper;

	PatternTransfer(IRecipeTransferHandlerHelper helper)
	{
		this.helper = helper;
	}

	@Override
	public Class<? extends PatternFloppyMenu> getContainerClass()
	{
		return PatternFloppyMenu.class;
	}

	@Override
	public Optional<MenuType<PatternFloppyMenu>> getMenuType()
	{
		return Optional.of(ECMenuTypes.PATTERN_FLOPPY.get());
	}

	@Override
	@Nullable
	public IRecipeTransferError transferRecipe(PatternFloppyMenu menu, Object recipe, IRecipeSlotsView recipeSlots,
			Player player, boolean maxTransfer, boolean doTransfer)
	{
		boolean crafting = recipe instanceof RecipeHolder<?> holder&&holder.value() instanceof CraftingRecipe;
		Pattern pattern = crafting? crafting(recipeSlots): processing(recipeSlots);
		if(pattern==null)
			return helper.createUserErrorWithTooltip(
					Component.translatable("jei.engineered_computation.pattern.too_many", Pattern.INPUTS));
		if(crafting? pattern.isEmpty(): !pattern.isValid())
			return helper.createUserErrorWithTooltip(
					Component.translatable("jei.engineered_computation.pattern.no_items"));
		if(doTransfer)
			PacketDistributor.sendToServer(new PatternFillPayload(menu.containerId, pattern));
		return null;
	}

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
		// Extra outputs past the pattern's last slot are chance byproducts, so they're left off.
		return new Pattern(Pattern.Mode.ABSTRACT, inputs.stacks(), outputs.stacks(), inputs.tags(), outputs.tags());
	}

	/** Uses the ingredient JEI is showing right now, so a cycling one lands as what the player saw. */
	static ItemStack shown(IRecipeSlotView slot)
	{
		return slot.getDisplayedItemStack().or(() -> slot.getItemStacks().findFirst()).orElse(ItemStack.EMPTY).copy();
	}

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
				TagKey<Item> tag = view.getTagKey().flatMap(key -> key.cast(Registries.ITEM)).orElse(null);
				int same = -1;
				for(int i = 0; i<stacks.size()&&same<0; i++)
					if(ItemStack.isSameItemSameComponents(stacks.get(i), stack)&&Objects.equals(tags.get(i), tag))
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
