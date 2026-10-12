package com.muddykat.engineeredcomputation.compat.jei;

import java.util.Optional;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;

import com.muddykat.engineeredcomputation.terminal.TerminalScreen;

/** JEI's keys (recipes, uses, bookmarks) on the terminal's item grid, whose items aren't in slots of their own. */
final class TerminalGridHandler implements IGuiContainerHandler<TerminalScreen>
{
	private final IIngredientManager ingredients;

	TerminalGridHandler(IIngredientManager ingredients)
	{
		this.ingredients = ingredients;
	}

	@Override
	public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(TerminalScreen screen, double mouseX,
			double mouseY)
	{
		return screen.gridIngredientAt(mouseX, mouseY).flatMap(cell -> ingredients.createClickableIngredient(
				VanillaTypes.ITEM_STACK, cell.stack(), cell.area(), false));
	}
}
