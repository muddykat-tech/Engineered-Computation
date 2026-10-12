package com.muddykat.engineeredcomputation.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import com.muddykat.engineeredcomputation.adapter.ProgramEditPayload;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.network.ECPayloads;

/** Items dragged from JEI onto a program floppy's entries, as if set with the item in hand. */
final class ProgramGhostHandler implements IGhostIngredientHandler<ProgramFloppyScreen>
{
	@Override
	public <I> List<Target<I>> getTargets(ProgramFloppyScreen screen, I ingredient, boolean doStart)
	{
		List<Target<I>> targets = new ArrayList<>();
		if(!(ingredient instanceof ItemStack stack)||stack.isEmpty())
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
					ECPayloads.sendToServer(new ProgramEditPayload(screen.getMenu().containerId, index,
							ProgramEditPayload.Action.SET_ITEM, "", ItemHandlerHelper.copyStackWithSize(stack, 1)));
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
