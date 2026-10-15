package com.muddykat.engineeredcomputation.remote;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

public record TerminalRemotePayload(int containerId, List<RemoteStock.Stock> stock) implements CustomPacketPayload
{
	public static final int MAX = 4096;

	public static final Type<TerminalRemotePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_remote"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRemotePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TerminalRemotePayload::containerId,
					RemoteStock.Stock.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), TerminalRemotePayload::stock,
					TerminalRemotePayload::new);

	@Override
	public Type<TerminalRemotePayload> type()
	{
		return TYPE;
	}

	public static void handle(TerminalRemotePayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof FloppyTerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyRemote(payload);
	}
}
