package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import com.muddykat.engineeredcomputation.network.ECPayloads;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.CraftingGrid;
import com.muddykat.engineeredcomputation.terminal.GridFillPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

/**
 * JEI's [+] on a crafting recipe, into a terminal's crafting grid (the server lays it out: TerminalMenu.fillGrid).
 * Slots the network - or, for an unlinked grid, the player's inventory too - can't fill are marked red, but the
 * transfer still goes ahead: a linked slot is only a link, and the rest of the recipe is laid out regardless.
 */
final class TerminalGridTransfer implements IRecipeTransferHandler<TerminalMenu, CraftingRecipe>
{
	private final IRecipeTransferHandlerHelper helper;

	TerminalGridTransfer(IRecipeTransferHandlerHelper helper)
	{
		this.helper = helper;
	}

	@Override
	public Class<? extends TerminalMenu> getContainerClass()
	{
		return TerminalMenu.class;
	}

	@Override
	public Optional<MenuType<TerminalMenu>> getMenuType()
	{
		return Optional.of(ECMenuTypes.TERMINAL.get());
	}

	@Override
	public RecipeType<CraftingRecipe> getRecipeType()
	{
		return RecipeTypes.CRAFTING;
	}

	@Override
	@Nullable
	public IRecipeTransferError transferRecipe(TerminalMenu menu, CraftingRecipe recipe, IRecipeSlotsView recipeSlots,
			Player player, boolean maxTransfer, boolean doTransfer)
	{
		return transfer(menu, recipeSlots, player, maxTransfer, doTransfer);
	}

	@Nullable
	private IRecipeTransferError transfer(TerminalMenu menu, IRecipeSlotsView recipeSlots, Player player,
			boolean maxTransfer, boolean doTransfer)
	{
		if(!menu.getFeatures().crafting())
			return helper.createInternalError();
		List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		if(inputs.size()>CraftingGrid.SIZE)
			return helper.createInternalError();
		boolean linked = menu.isLinked();
		Map<ItemKey, Long> available = new HashMap<>();
		List<List<ItemStack>> options = new ArrayList<>(CraftingGrid.SIZE);
		List<IRecipeSlotView> missing = new ArrayList<>();
		for(IRecipeSlotView input : inputs)
		{
			List<ItemStack> stacks = input.getItemStacks().filter(stack -> !stack.isEmpty())
					.limit(GridFillPayload.OPTIONS).toList();
			options.add(stacks);
			if(!stacks.isEmpty()&&!reserve(stacks, available, menu, player, linked))
				missing.add(input);
		}
		if(doTransfer)
			ECPayloads.sendToServer(new GridFillPayload(menu.containerId, options, maxTransfer));
		return missing.isEmpty()? null: new MissingItemsError(missing, linked);
	}

	/** Counts one of any of {@code stacks} as used, if there's one left (in the network, or the inventory too). */
	private static boolean reserve(List<ItemStack> stacks, Map<ItemKey, Long> available, TerminalMenu menu,
			Player player, boolean linked)
	{
		for(ItemStack stack : stacks)
		{
			ItemKey key = ItemKey.of(stack);
			long left = available.computeIfAbsent(key,
					k -> menu.storedOnClient(stack)+(linked? 0: carried(player, stack)));
			if(left>0)
			{
				available.put(key, left-1);
				return true;
			}
		}
		return false;
	}

	private static int carried(Player player, ItemStack stack)
	{
		int count = 0;
		for(ItemStack held : player.getInventory().items)
			if(ItemStack.isSameItemSameTags(held, stack))
				count += held.getCount();
		return count;
	}
}
