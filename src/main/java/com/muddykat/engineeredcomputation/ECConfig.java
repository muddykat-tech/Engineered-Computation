package com.muddykat.engineeredcomputation;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ECConfig
{
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue TAPE_ARCHIVING = BUILDER
			.comment("Whether tape drives move a network's biggest stocks onto their reels, and small ones back off them.",
					"Off, tape still takes items that are already on it, and new ones once other storage is full.")
			.translation(EngineeredComputation.MODID+".configuration.tapeArchiving")
			.define("tapeArchiving", true);

	public static final ModConfigSpec SPEC = BUILDER.build();

	private ECConfig()
	{
	}

	public static boolean tapeArchiving()
	{
		return SPEC.isLoaded() ? TAPE_ARCHIVING.getAsBoolean() : TAPE_ARCHIVING.getDefault();
	}
}
