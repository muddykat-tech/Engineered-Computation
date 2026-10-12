package com.muddykat.engineeredcomputation.client.render;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Ticks counted on the client for block animations. The level's game time is reset to the server's every second, so
 * on a lagging server it jumps back and forth and short animations skip.
 */
public final class AnimationClock
{
	private static long ticks;

	private AnimationClock()
	{
	}

	public static void register()
	{
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.ClientTickEvent.class, event -> {
			if(event.phase==TickEvent.Phase.END&&!Minecraft.getInstance().isPaused())
				ticks++;
		});
	}

	static double now(float partialTick)
	{
		return ticks+partialTick;
	}
}
