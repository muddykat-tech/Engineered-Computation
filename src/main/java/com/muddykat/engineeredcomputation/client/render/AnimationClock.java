package com.muddykat.engineeredcomputation.client.render;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// Just a handler so we can do the fancy animations properly, should make sure they're smooth on laggy servers
// as this hands it off to the client
public final class AnimationClock
{
	private static long ticks;

	private AnimationClock()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
			if(!Minecraft.getInstance().isPaused())
				ticks++;
		});
	}

	public static double now(float partialTick)
	{
		return ticks+partialTick;
	}
}
