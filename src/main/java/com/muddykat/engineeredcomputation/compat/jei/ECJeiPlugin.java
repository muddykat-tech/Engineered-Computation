package com.muddykat.engineeredcomputation.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.ResourceLocation;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.client.screen.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.client.screen.program.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.TerminalScreen;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

/** Only loaded when JEI is installed. */
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
		registration.addRecipeTransferHandler(new TerminalGridTransfer<>(registration.getTransferHelper(),
				VideoTerminalMenu.class, ECMenuTypes.VIDEO_TERMINAL), RecipeTypes.CRAFTING);
		registration.addRecipeTransferHandler(new TerminalGridTransfer<>(registration.getTransferHelper(),
				FloppyTerminalMenu.class, ECMenuTypes.FLOPPY_TERMINAL), RecipeTypes.CRAFTING);
		registration.addUniversalRecipeTransferHandler(new PatternTransfer(registration.getTransferHelper()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		ECTerminals.items().forEach((era, item) -> {
			if(TerminalFeatures.of(era).crafting())
				registration.addRecipeCatalyst(item.get(), RecipeTypes.CRAFTING);
		});
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration)
	{
		registration.addGuiContainerHandler(terminalScreens(), new TerminalGridHandler());
		registration.addGhostIngredientHandler(PatternFloppyScreen.class, new PatternGhostHandler());
		registration.addGhostIngredientHandler(ProgramFloppyScreen.class, new ProgramGhostHandler());
	}

	@SuppressWarnings("unchecked")
	private static Class<TerminalScreen<?>> terminalScreens()
	{
		return (Class<TerminalScreen<?>>)(Class<?>)TerminalScreen.class;
	}
}
