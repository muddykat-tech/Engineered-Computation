package com.muddykat.engineeredcomputation.storage.cell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Kept on the stack so the client can show fill levels without the contents, which only the server holds. */
public record CellSummary(int types, long count)
{
	public static final Codec<CellSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("types").forGetter(CellSummary::types),
			Codec.LONG.fieldOf("count").forGetter(CellSummary::count)
	).apply(instance, CellSummary::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CellSummary> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CellSummary::types,
			ByteBufCodecs.VAR_LONG, CellSummary::count,
			CellSummary::new);
}
