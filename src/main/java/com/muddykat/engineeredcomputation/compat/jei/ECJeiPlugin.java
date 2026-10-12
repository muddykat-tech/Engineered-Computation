package com.muddykat.engineeredcomputation.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.TerminalScreen;

/**
 * JEI integration, found by JEI through {@link JeiPlugin} (only JEI loads this package): its [+] button fills a
 * terminal's crafting grid from a crafting recipe and a pattern floppy from any recipe, items drag from JEI onto
 * pattern and program floppy slots, and JEI's keys work on the terminal's item grid.
 */
@JeiPlugin
public class ECJeiPlugin implements IModPlugin
{
	private static final ResourceLocation UID =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "jei");

	@Override
	public ResourceLocation getPluginUid()
	{
		return UID;
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration)
	{
		registration.addRecipeTransferHandler(new TerminalGridTransfer(registration.getTransferHelper()),
				RecipeTypes.CRAFTING);
		registration.addUniversalRecipeTransferHandler(new PatternTransfer(registration.getTransferHelper()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		ECTerminals.items().forEach((era, item) -> {
			if(TerminalFeatures.of(era).crafting())
				registration.addRecipeCatalyst(new ItemStack(item.get()), RecipeTypes.CRAFTING);
		});
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration)
	{
		registration.addGuiContainerHandler(TerminalScreen.class, new TerminalGridHandler());
		registration.addGhostIngredientHandler(PatternFloppyScreen.class, new PatternGhostHandler());
		registration.addGhostIngredientHandler(ProgramFloppyScreen.class, new ProgramGhostHandler());
	}
}
