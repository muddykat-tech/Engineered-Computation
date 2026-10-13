package com.muddykat.engineeredcomputation.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

record MissingItemsError(List<IRecipeSlotView> missing, boolean linked) implements IRecipeTransferError
{
	private static final int HIGHLIGHT = 0x66FF0000;

	@Override
	public Type getType()
	{
		return Type.COSMETIC;
	}

	@Override
	public void showError(GuiGraphics graphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlots, int recipeX,
			int recipeY)
	{
		graphics.pose().pushPose();
		graphics.pose().translate(recipeX, recipeY, 0);
		for(IRecipeSlotView slot : missing)
			slot.drawHighlight(graphics, HIGHLIGHT);
		graphics.pose().popPose();
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip)
	{
		tooltip.add(Component.translatable("jei.engineered_computation.missing").withStyle(ChatFormatting.RED));
		tooltip.add(Component.translatable(linked? "jei.engineered_computation.missing.linked":
				"jei.engineered_computation.missing.unlinked").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public int getMissingCountHint()
	{
		return missing.size();
	}
}
