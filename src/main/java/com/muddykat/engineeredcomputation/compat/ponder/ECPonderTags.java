package com.muddykat.engineeredcomputation.compat.ponder;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECContent;

/** Groups in Ponder's index; titles are the English fallback for the ponder.tag lang keys. */
public final class ECPonderTags
{
	public static final ResourceLocation ERA1 = id(ComputerEra.PUNCHCARD);

	private ECPonderTags()
	{
	}

	static void register(PonderTagRegistrationHelper<ResourceLocation> helper)
	{
		helper.registerTag(ERA1)
				.addToIndex()
				.item(ECContent.casingItem(ComputerEra.PUNCHCARD).get())
				.title("Analogue Era")
				.description("Punch cards, Tube Stations and the Tabulator Machine")
				.register();
	}

	private static ResourceLocation id(ComputerEra era)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, era.getSerializedName());
	}
}
