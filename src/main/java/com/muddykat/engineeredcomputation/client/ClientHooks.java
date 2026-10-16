package com.muddykat.engineeredcomputation.client;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

import com.muddykat.engineeredcomputation.compat.patchouli.PatchouliClientCompat;
import com.muddykat.engineeredcomputation.client.guide.GuideScreen;
import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class ClientHooks
{
	private ClientHooks()
	{
	}

	/** With Patchouli installed the guides open its handbook, unless {@code classic} asks for the edition itself. */
	public static void openGuide(ComputerEra era, boolean classic)
	{
		if(!classic&&ModList.get().isLoaded("patchouli"))
			PatchouliClientCompat.openEraCategory(era);
		else
			Minecraft.getInstance().setScreen(GuideScreen.open(era));
	}
}
