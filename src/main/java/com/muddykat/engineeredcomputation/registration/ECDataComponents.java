package com.muddykat.engineeredcomputation.registration;

import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.deck.FilterDeck;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.program.Program;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellSummary;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;

public final class ECDataComponents
{
	public static final DeferredRegister.DataComponents REGISTER =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EngineeredComputation.MODID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CellContents>> CELL_CONTENTS =
			REGISTER.registerComponentType("cell_contents", builder -> builder
					.persistent(CellContents.CODEC)
					.networkSynchronized(CellContents.STREAM_CODEC)
					.cacheEncoding());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> CELL_UUID =
			REGISTER.registerComponentType("cell_uuid", builder -> builder
					.persistent(UUIDUtil.CODEC)
					.networkSynchronized(UUIDUtil.STREAM_CODEC));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CellSummary>> CELL_SUMMARY =
			REGISTER.registerComponentType("cell_summary", builder -> builder
					.persistent(CellSummary.CODEC)
					.networkSynchronized(CellSummary.STREAM_CODEC));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PRIORITY =
			REGISTER.registerComponentType("priority", builder -> builder
					.persistent(Codec.intRange(EraDataCell.MIN_PRIORITY, EraDataCell.MAX_PRIORITY))
					.networkSynchronized(ByteBufCodecs.VAR_INT));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Program>> PROGRAM =
			REGISTER.registerComponentType("program", builder -> builder
					.persistent(Program.CODEC)
					.networkSynchronized(Program.STREAM_CODEC)
					.cacheEncoding());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Pattern>> PATTERN =
			REGISTER.registerComponentType("pattern", builder -> builder
					.persistent(Pattern.CODEC)
					.networkSynchronized(Pattern.STREAM_CODEC)
					.cacheEncoding());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CatalogueCard>> CATALOGUE_CARD =
			REGISTER.registerComponentType("catalogue_card", builder -> builder
					.persistent(CatalogueCard.CODEC)
					.networkSynchronized(CatalogueCard.STREAM_CODEC)
					.cacheEncoding());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardDeck>> CARD_DECK =
			REGISTER.registerComponentType("card_deck", builder -> builder
					.persistent(CardDeck.CODEC)
					.networkSynchronized(CardDeck.STREAM_CODEC)
					.cacheEncoding());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterDeck>> FILTER_DECK =
			REGISTER.registerComponentType("filter_deck", builder -> builder
					.persistent(FilterDeck.CODEC)
					.networkSynchronized(FilterDeck.STREAM_CODEC)
					.cacheEncoding());

	private ECDataComponents()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		REGISTER.register(modEventBus);
	}
}
