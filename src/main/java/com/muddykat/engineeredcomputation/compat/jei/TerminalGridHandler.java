package com.muddykat.engineeredcomputation.compat.jei;

import java.util.List;
import java.util.Optional;

import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;

import net.minecraft.client.renderer.Rect2i;

import com.muddykat.engineeredcomputation.client.screen.terminal.TerminalScreen;

/** Lets JEI's keys work on the terminal's item grid, whose items aren't in real slots. */
final class TerminalGridHandler implements IGuiContainerHandler<TerminalScreen<?>>
{
	/** Keeps JEI clear of the teletype's Filter Card popup, which can be dragged anywhere. */
	@Override
	public List<Rect2i> getGuiExtraAreas(TerminalScreen<?> screen)
	{
		return screen.extraAreas();
	}

	@Override
	public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
			IClickableIngredientFactory builder, TerminalScreen<?> screen, double mouseX, double mouseY)
	{
		return screen.gridIngredientAt(mouseX, mouseY)
				.flatMap(cell -> builder.createBuilder(cell.stack()).buildWithArea(cell.area()));
	}
}
