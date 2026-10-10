package com.muddykat.engineeredcomputation.compat.jei;

import java.util.Optional;

import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;

import com.muddykat.engineeredcomputation.terminal.TerminalScreen;

/** JEI's keys (recipes, uses, bookmarks) on the terminal's item grid, whose items aren't in slots of their own. */
final class TerminalGridHandler implements IGuiContainerHandler<TerminalScreen>
{
	@Override
	public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
			IClickableIngredientFactory builder, TerminalScreen screen, double mouseX, double mouseY)
	{
		return screen.gridIngredientAt(mouseX, mouseY)
				.flatMap(cell -> builder.createBuilder(cell.stack()).buildWithArea(cell.area()));
	}
}
