package com.muddykat.engineeredcomputation.compat.ponder;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.muddykat.engineeredcomputation.client.render.AnimationClock;

/** Only loaded on the client when Ponder is installed, so nothing else may refer to Ponder's classes. */
public final class PonderCompat
{
	private PonderCompat()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(FMLClientSetupEvent.class, event -> PonderIndex.addPlugin(new ECPonderPlugin()));
		// Scenes keep their own time, as the game is paused behind them
		AnimationClock.addSceneClock(level -> level instanceof PonderLevel
				? AnimationTickHolder.getRenderTime(level): Double.NaN);
	}
}
