package com.muddykat.engineeredcomputation.catalogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import com.muddykat.engineeredcomputation.storage.CellContents;

public record CatalogueCard(GlobalPos station, Component label, CellContents tally, long talliedAt)
{
	public static final long NEVER = -1;

	private static final Codec<Component> LABEL_CODEC =
			Codec.STRING.xmap(Component.Serializer::fromJson, Component.Serializer::toJson);

	public static final Codec<CatalogueCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("station").forGetter(CatalogueCard::station),
			LABEL_CODEC.fieldOf("label").forGetter(CatalogueCard::label),
			CellContents.CODEC.fieldOf("tally").forGetter(CatalogueCard::tally),
			Codec.LONG.fieldOf("tallied_at").forGetter(CatalogueCard::talliedAt)
	).apply(instance, CatalogueCard::new));

	public static CatalogueCard read(FriendlyByteBuf buf)
	{
		return new CatalogueCard(
				GlobalPos.of(ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation()),
						buf.readBlockPos()),
				buf.readComponent(), CellContents.read(buf), buf.readVarLong());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeResourceLocation(station.dimension().location());
		buf.writeBlockPos(station.pos());
		buf.writeComponent(label);
		tally.write(buf);
		buf.writeVarLong(talliedAt);
	}

	public CatalogueCard withTally(CellContents tally, long now)
	{
		return new CatalogueCard(station, label, tally, now);
	}

	public CatalogueCard withReport(Component label, CellContents tally, long now)
	{
		return new CatalogueCard(station, label, tally, now);
	}
}
