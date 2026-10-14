package com.muddykat.engineeredcomputation.compat.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class ECPonderPlugin implements PonderPlugin
{
	@Override
	public String getModId()
	{
		return EngineeredComputation.MODID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper)
	{
		ECPonderScenes.register(helper);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper)
	{
		ECPonderTags.register(helper);
	}
}
