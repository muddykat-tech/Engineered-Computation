package com.muddykat.engineeredcomputation;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ECConfig
{
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

	public static final ForgeConfigSpec.BooleanValue TAPE_ARCHIVING = BUILDER
			.comment("Whether bulk network storage types will move a large stocks automatically, this will also ensure that it moves small (if they have any) stocks onto other drives when doing so.")
			.translation(EngineeredComputation.MODID+".configuration.tapeArchiving")
			.define("tapeArchiving", true);

	public static final ForgeConfigSpec SPEC = BUILDER.build();

	private ECConfig()
	{
	}

	public static boolean tapeArchiving()
	{
		return SPEC.isLoaded() ? TAPE_ARCHIVING.get() : TAPE_ARCHIVING.getDefault();
	}
}
