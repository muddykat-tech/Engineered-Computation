package com.muddykat.engineeredcomputation.catalogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.muddykat.engineeredcomputation.storage.CellContents;

public record CatalogueCard(GlobalPos station, Component label, CellContents tally, long talliedAt)
{
	public static final long NEVER = -1;

	public static final Codec<CatalogueCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("station").forGetter(CatalogueCard::station),
			ComponentSerialization.CODEC.fieldOf("label").forGetter(CatalogueCard::label),
			CellContents.CODEC.fieldOf("tally").forGetter(CatalogueCard::tally),
			Codec.LONG.fieldOf("tallied_at").forGetter(CatalogueCard::talliedAt)
	).apply(instance, CatalogueCard::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CatalogueCard> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC, CatalogueCard::station,
			ComponentSerialization.STREAM_CODEC, CatalogueCard::label,
			CellContents.STREAM_CODEC, CatalogueCard::tally,
			ByteBufCodecs.VAR_LONG, CatalogueCard::talliedAt,
			CatalogueCard::new);

	public CatalogueCard withTally(CellContents tally, long now)
	{
		return new CatalogueCard(station, label, tally, now);
	}

	public CatalogueCard withReport(Component label, CellContents tally, long now)
	{
		return new CatalogueCard(station, label, tally, now);
	}
}
