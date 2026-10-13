package com.muddykat.engineeredcomputation.client.render.drive;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.LightTexture;

import com.muddykat.engineeredcomputation.storage.cell.CellStatus;

final class FloppyBays
{
	private static final float INSERT_TICKS = 6;
	private static final float EJECT_TICKS = 4;

	static final float HIDDEN = -1;

	private final Map<Object, Bays> bays = new WeakHashMap<>();

	private static final class Bays
	{
		int filled;
		final double[] changedAt;
		final int[] tiers;

		Bays(int filled, int slots)
		{
			this.filled = filled;
			tiers = new int[slots];
			changedAt = new double[slots];
			Arrays.fill(changedAt, Double.NEGATIVE_INFINITY);
		}
	}

	float[] slideOut(Object owner, int filled, int slots, double now)
	{
		Bays state = bays.computeIfAbsent(owner, o -> new Bays(filled, slots));
		float[] out = new float[slots];
		for(int slot = 0; slot<slots; slot++)
		{
			boolean in = (filled&1<<slot)!=0;
			if(in == ((state.filled & 1 << slot) == 0))
				state.changedAt[slot] = now;
			double since = now-state.changedAt[slot];
			if(in)
				out[slot] = 1-DriveRenderer.ease((float)(since/INSERT_TICKS));
			else
				out[slot] = since<EJECT_TICKS? DriveRenderer.ease((float)(since/EJECT_TICKS)): HIDDEN;
		}
		state.filled = filled;
		return out;
	}

	int[] tiers(Object owner, int[] seen)
	{
		int[] tiers = bays.get(owner).tiers;
		for(int slot = 0; slot<tiers.length; slot++)
			if(seen[slot]>=0)
				tiers[slot] = seen[slot];
		return tiers.clone();
	}

	static int lightColour(CellStatus status)
	{
		return switch(status)
		{
			case EMPTY -> 0x5A281E;
			case SPACE -> 0x60CE70;
			case NEARLY_FULL -> 0xEEB03A;
			case FULL -> 0xD6402C;
		};
	}

	static int lightLevel(CellStatus status, int packedLight)
	{
		return status==CellStatus.EMPTY? packedLight: LightTexture.FULL_BRIGHT;
	}
}
