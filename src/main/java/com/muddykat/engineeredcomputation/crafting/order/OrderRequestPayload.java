package com.muddykat.engineeredcomputation.crafting.order;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternOffer;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

public record OrderRequestPayload(int containerId, ItemKey target, long amount, boolean place,
		Optional<PatternOffer.Choice> offer)
		implements CustomPacketPayload
{
	public static final Type<OrderRequestPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "order_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OrderRequestPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OrderRequestPayload::containerId,
			ItemKey.STREAM_CODEC, OrderRequestPayload::target,
			ByteBufCodecs.VAR_LONG, OrderRequestPayload::amount,
			ByteBufCodecs.BOOL, OrderRequestPayload::place,
			ByteBufCodecs.optional(PatternOffer.Choice.STREAM_CODEC), OrderRequestPayload::offer,
			OrderRequestPayload::new);

	public OrderRequestPayload(int containerId, ItemKey target, long amount, boolean place)
	{
		this(containerId, target, amount, place, Optional.empty());
	}

	@Override
	public Type<OrderRequestPayload> type()
	{
		return TYPE;
	}

	public static void handle(OrderRequestPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof FloppyTerminalMenu menu&&menu.containerId==payload.containerId())
			menu.handleOrderRequest(payload.target(), payload.amount(), payload.place(), payload.offer());
	}
}
