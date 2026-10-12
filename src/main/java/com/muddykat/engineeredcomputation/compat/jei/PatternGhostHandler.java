package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.PatternFillPayload;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.network.ECPayloads;

/** Items dragged from JEI onto a pattern floppy's slots: one of the item, as if set with it in hand. */
final class PatternGhostHandler implements IGhostIngredientHandler<PatternFloppyScreen>
{
	@Override
	public <I> List<Target<I>> getTargets(PatternFloppyScreen screen, I ingredient, boolean doStart)
	{
		List<Target<I>> targets = new ArrayList<>();
		if(!(ingredient instanceof ItemStack stack)||stack.isEmpty())
			return targets;
		for(int slot = 0; slot<Pattern.INPUTS+Pattern.OUTPUTS; slot++)
		{
			int index = slot;
			screen.settableSlotArea(slot).ifPresent(area -> targets.add(new Target<>()
			{
				@Override
				public Rect2i getArea()
				{
					return area;
				}

				@Override
				public void accept(I dropped)
				{
					Pattern pattern = screen.getMenu().pattern();
					ItemStack one = ItemHandlerHelper.copyStackWithSize(stack, 1);
					pattern = index<Pattern.INPUTS? pattern.withInput(index, one):
							pattern.withOutput(index-Pattern.INPUTS, one);
					ECPayloads.sendToServer(new PatternFillPayload(screen.getMenu().containerId, pattern));
				}
			}));
		}
		return targets;
	}

	@Override
	public void onComplete()
	{
	}
}
