package com.muddykat.engineeredcomputation.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

// Just a handler so we can do the fancy animations properly, should make sure they're smooth on laggy servers
// as this hands it off to the client
public final class AnimationClock
{
	private static long ticks;
	private static final List<ToDoubleFunction<Level>> sceneClocks = new ArrayList<>();

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

	/**
	 * For levels drawn outside the world, such as Ponder's scenes, which run while the game is paused. The clock gives
	 * NaN for levels it doesn't keep time for.
	 */
	public static void addSceneClock(ToDoubleFunction<Level> clock)
	{
		sceneClocks.add(clock);
	}

	public static double now(@Nullable Level level, float partialTick)
	{
		if(level!=null&&level!=Minecraft.getInstance().level)
			for(ToDoubleFunction<Level> clock : sceneClocks)
			{
				double now = clock.applyAsDouble(level);
				if(!Double.isNaN(now))
					return now;
			}
		return ticks+partialTick;
	}
}
