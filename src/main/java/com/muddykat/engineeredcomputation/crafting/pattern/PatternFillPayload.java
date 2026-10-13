package com.muddykat.engineeredcomputation.crafting.pattern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public record PatternFillPayload(int containerId, Pattern pattern) implements CustomPacketPayload
{
	public static final Type<PatternFillPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "pattern_fill"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PatternFillPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PatternFillPayload::containerId,
			Pattern.STREAM_CODEC, PatternFillPayload::pattern,
			PatternFillPayload::new);

	@Override
	public Type<PatternFillPayload> type()
	{
		return TYPE;
	}

	public static void handle(PatternFillPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof PatternFloppyMenu menu&&menu.containerId==payload.containerId())
			menu.fill(payload.pattern());
	}
}
