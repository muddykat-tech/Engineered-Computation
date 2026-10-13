package com.muddykat.engineeredcomputation.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.program.ProgramEntry;

public final class FilterCardDecorator implements IItemDecorator
{
	private static final float SCALE = 0.5F;

	@Override
	public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y)
	{
		ProgramEntry filter = FilterCardItem.filter(stack);
		ItemStack icon = filter.isEmpty()? ItemStack.EMPTY: ECGui.entryIcon(filter);
		if(icon.isEmpty())
			return false;
		graphics.pose().pushPose();
		// Items draw about 150 deep, so 160 should put the filter in front of the card.
		graphics.pose().translate(x+8, y+8, 160);
		graphics.pose().scale(SCALE, SCALE, SCALE);
		graphics.renderItem(icon, 0, 0);
		graphics.pose().popPose();
		return false;
	}
}
