package com.muddykat.engineeredcomputation.pneumatic.station;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public record StationNamePayload(int containerId, String name) implements CustomPacketPayload
{
	public static final Type<StationNamePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "station_name"));

	public static final StreamCodec<RegistryFriendlyByteBuf, StationNamePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, StationNamePayload::containerId,
			ByteBufCodecs.stringUtf8(TubeStationBlockEntity.MAX_NAME*4), StationNamePayload::name,
			StationNamePayload::new);

	@Override
	public Type<StationNamePayload> type()
	{
		return TYPE;
	}

	public static void handle(StationNamePayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof NamedStationMenu menu
				&&context.player().containerMenu.containerId==payload.containerId())
			menu.rename(payload.name());
	}
}
