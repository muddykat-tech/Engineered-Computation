package com.muddykat.engineeredcomputation.client;

import net.minecraft.client.Minecraft;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;
import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class ClientHooks
{
	private ClientHooks()
	{
	}

	public static void openGuide(ComputerEra era)
	{
		Minecraft.getInstance().setScreen(GuideScreen.open(era));
	}
}
