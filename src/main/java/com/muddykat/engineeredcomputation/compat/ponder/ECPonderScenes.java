package com.muddykat.engineeredcomputation.compat.ponder;

import java.util.ArrayList;
import java.util.List;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.compat.ponder.scenes.TabulatorScenes;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECTerminals;

/**
 * Keeps track of ponder associations with items
 * assets/engineered_computation/ponder/&lt;schematic&gt;.nbt, saved with a Structure Block.
 */
public final class ECPonderScenes
{
	public static final String TABULATOR_SETUP = "era1/tabulator_setup";
	public static final String TABULATOR_USAGE = "era1/tabulator_usage";
	public static final String TABULATOR_AUTOSMELTING = "era1/tabulator_autosmelting";

	private ECPonderScenes()
	{
	}

	static void register(PonderSceneRegistrationHelper<ResourceLocation> registration)
	{
		PonderSceneRegistrationHelper<Item> helper = registration.withKeyFunction(BuiltInRegistries.ITEM::getKey);

		List<Item> tabulator = new ArrayList<>();
		tabulator.add(ECContent.casingItem(ComputerEra.PUNCHCARD).get());
		tabulator.add(ECTerminals.items().get(ComputerEra.PUNCHCARD).get());
		tabulator.add(ECCatalogue.CARD.get());
		ECCatalogue.decks().forEach(deck -> tabulator.add(deck.get()));
		helper.forComponents(tabulator)
				.addStoryBoard(TABULATOR_SETUP, TabulatorScenes::setup, ECPonderTags.ERA1)
				.addStoryBoard(TABULATOR_USAGE, TabulatorScenes::usage, ECPonderTags.ERA1)
				.addStoryBoard(TABULATOR_AUTOSMELTING, TabulatorScenes::autosmelting, ECPonderTags.ERA1);
		helper.forComponents(ECPneumatics.TUBE.get().asItem(), ECPneumatics.RECEIVING_STATION.get().asItem(),
						ECPneumatics.PRESSURIZER.get().asItem())
				.addStoryBoard(TABULATOR_USAGE, TabulatorScenes::usage, ECPonderTags.ERA1);
		helper.forComponents(ECPneumatics.AUTOTUBE.get().asItem(), ECPneumatics.OBSERVING_AUTOTUBE.get().asItem())
				.addStoryBoard(TABULATOR_AUTOSMELTING, TabulatorScenes::autosmelting, ECPonderTags.ERA1);
	}
}
