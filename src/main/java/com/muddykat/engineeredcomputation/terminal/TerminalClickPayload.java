package com.muddykat.engineeredcomputation.terminal;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record TerminalClickPayload(int containerId, Optional<ItemKey> key, TerminalAction action)
		implements CustomPacketPayload
{
	public static final Type<TerminalClickPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_click"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalClickPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalClickPayload::containerId,
			ByteBufCodecs.optional(ItemKey.STREAM_CODEC), TerminalClickPayload::key,
			NeoForgeStreamCodecs.enumCodec(TerminalAction.class), TerminalClickPayload::action,
			TerminalClickPayload::new);

	@Override
	public Type<TerminalClickPayload> type()
	{
		return TYPE;
	}

	public static void handle(TerminalClickPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.handleClick(payload.key().orElse(null), payload.action());
	}
}
