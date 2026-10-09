package com.muddykat.engineeredcomputation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;
import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class ClientHooks
{
	private ClientHooks()
	{
	}

	/** The local player, for packets the server sent. */
	public static Player player()
	{
		return Minecraft.getInstance().player;
	}

	public static void openGuide(ComputerEra era)
	{
		Minecraft.getInstance().setScreen(GuideScreen.open(era));
	}
}
