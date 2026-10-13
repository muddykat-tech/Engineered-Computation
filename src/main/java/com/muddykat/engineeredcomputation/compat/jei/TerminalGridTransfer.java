package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.crafting.CraftingGrid;
import com.muddykat.engineeredcomputation.terminal.crafting.GridFillPayload;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;

/** Missing items are marked red but the transfer still happens, since a linked slot only needs a link. */
final class TerminalGridTransfer<M extends VideoTerminalMenu>
		implements IRecipeTransferHandler<M, RecipeHolder<CraftingRecipe>>
{
	private final IRecipeTransferHandlerHelper helper;
	private final Class<M> menuClass;
	private final Supplier<MenuType<M>> menuType;

	TerminalGridTransfer(IRecipeTransferHandlerHelper helper, Class<M> menuClass, Supplier<MenuType<M>> menuType)
	{
		this.helper = helper;
		this.menuClass = menuClass;
		this.menuType = menuType;
	}

	@Override
	public Class<? extends M> getContainerClass()
	{
		return menuClass;
	}

	@Override
	public Optional<MenuType<M>> getMenuType()
	{
		return Optional.of(menuType.get());
	}

	@Override
	public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType()
	{
		return RecipeTypes.CRAFTING;
	}

	@Override
	@Nullable
	public IRecipeTransferError transferRecipe(
			IRecipeTransferContext<RecipeHolder<CraftingRecipe>, M> context, boolean doTransfer)
	{
		IRecipeTransferError error = transfer(context.getContainer(), context.getRecipeSlots(), context.getPlayer(),
				context.isMaxTransfer(), doTransfer);
		if(doTransfer)
			context.completeRecipeTransfer(error==null||error.getType().allowsTransfer? RecipeTransferResult.SUCCESS:
					RecipeTransferResult.REJECTED);
		return error;
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public IRecipeTransferError transferRecipe(M menu, RecipeHolder<CraftingRecipe> recipe,
			IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
	{
		return transfer(menu, recipeSlots, player, maxTransfer, doTransfer);
	}

	@Nullable
	private IRecipeTransferError transfer(VideoTerminalMenu menu, IRecipeSlotsView recipeSlots, Player player,
			boolean maxTransfer, boolean doTransfer)
	{
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
			PacketDistributor.sendToServer(new GridFillPayload(menu.containerId, options, maxTransfer));
		return missing.isEmpty()? null: new MissingItemsError(missing, linked);
	}

	private static boolean reserve(List<ItemStack> stacks, Map<ItemKey, Long> available, VideoTerminalMenu menu,
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
			if(ItemStack.isSameItemSameComponents(held, stack))
				count += held.getCount();
		return count;
	}
}
