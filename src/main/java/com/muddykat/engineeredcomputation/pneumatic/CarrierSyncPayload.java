package com.muddykat.engineeredcomputation.pneumatic;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public record CarrierSyncPayload(List<Flight> launched, List<Long> gone) implements CustomPacketPayload
{
	public static final Type<CarrierSyncPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "carrier_sync"));

	public record Flight(long id, List<Long> path, long launched)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Flight> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG, Flight::id,
				ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), Flight::path,
				ByteBufCodecs.VAR_LONG, Flight::launched,
				Flight::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, CarrierSyncPayload> STREAM_CODEC = StreamCodec.composite(
			Flight.STREAM_CODEC.apply(ByteBufCodecs.list()), CarrierSyncPayload::launched,
			ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), CarrierSyncPayload::gone,
			CarrierSyncPayload::new);

	@Override
	public Type<CarrierSyncPayload> type()
	{
		return TYPE;
	}

	public static void handle(CarrierSyncPayload payload, IPayloadContext context)
	{
		CarrierFlights.apply(payload);
	}
}
