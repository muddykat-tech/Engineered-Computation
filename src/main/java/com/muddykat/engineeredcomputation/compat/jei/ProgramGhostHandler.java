package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.client.screen.program.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;

final class ProgramGhostHandler implements IGhostIngredientHandler<ProgramFloppyScreen>
{
	@Override
	public <I> List<Target<I>> getTargetsTyped(ProgramFloppyScreen screen, ITypedIngredient<I> ingredient,
			boolean doStart)
	{
		ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		List<Target<I>> targets = new ArrayList<>();
		if(stack.isEmpty())
			return targets;
		for(int entry = 0; entry<screen.getMenu().size(); entry++)
		{
			int index = entry;
			Rect2i area = screen.entryArea(entry);
			targets.add(new Target<>()
			{
				@Override
				public Rect2i getArea()
				{
					return area;
				}

				@Override
				public void accept(I dropped)
				{
					PacketDistributor.sendToServer(new ProgramEditPayload(screen.getMenu().containerId, index,
							ProgramEditPayload.Action.SET_ITEM, "", stack.copyWithCount(1)));
				}
			});
		}
		return targets;
	}

	@Override
	public void onComplete()
	{
	}
}
