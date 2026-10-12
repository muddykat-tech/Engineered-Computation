package com.muddykat.engineeredcomputation.compat.jei;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;

import com.muddykat.engineeredcomputation.terminal.TerminalScreen;

/** JEI's keys (recipes, uses, bookmarks) on the terminal's item grid, whose items aren't in slots of their own. */
final class TerminalGridHandler implements IGuiContainerHandler<TerminalScreen>
{
	@Override
	@Nullable
	public Object getIngredientUnderMouse(TerminalScreen screen, double mouseX, double mouseY)
	{
		return screen.gridIngredientAt(mouseX, mouseY).map(TerminalScreen.GridIngredient::stack).orElse(null);
	}
}
