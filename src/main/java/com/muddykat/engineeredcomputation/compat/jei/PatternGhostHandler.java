package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.client.screen.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFillPayload;

final class PatternGhostHandler implements IGhostIngredientHandler<PatternFloppyScreen>
{
	@Override
	public <I> List<Target<I>> getTargetsTyped(PatternFloppyScreen screen, ITypedIngredient<I> ingredient,
			boolean doStart)
	{
		ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		List<Target<I>> targets = new ArrayList<>();
		if(stack.isEmpty())
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
					ItemStack one = stack.copyWithCount(1);
					// A direct pattern given only a result needs to request that item's recipe.
					if(index>=Pattern.INPUTS&&pattern.mode()==Pattern.Mode.DIRECT)
						pattern = new Pattern(Pattern.Mode.DIRECT, List.of(), List.of(one));
					else
						pattern = index<Pattern.INPUTS? pattern.withInput(index, one):
								pattern.withOutput(index-Pattern.INPUTS, one);
					PacketDistributor.sendToServer(new PatternFillPayload(screen.getMenu().containerId, pattern));
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
