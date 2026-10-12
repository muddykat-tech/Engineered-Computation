package com.muddykat.engineeredcomputation.compat.jei;

import java.util.List;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Marks the recipe slots nothing could fill, in red, without stopping the transfer. (JEI 10's errors have no
 * tooltip of their own, so unlike on newer JEI there's no line saying why.)
 */
record MissingItemsError(List<IRecipeSlotView> missing) implements IRecipeTransferError
{
	private static final int HIGHLIGHT = 0x66FF0000;

	@Override
	public Type getType()
	{
		return Type.COSMETIC;
	}

	@Override
	public void showError(PoseStack pose, int mouseX, int mouseY, IRecipeSlotsView recipeSlots, int recipeX,
			int recipeY)
	{
		pose.pushPose();
		pose.translate(recipeX, recipeY, 0);
		for(IRecipeSlotView slot : missing)
			slot.drawHighlight(pose, HIGHLIGHT);
		pose.popPose();
	}
}
