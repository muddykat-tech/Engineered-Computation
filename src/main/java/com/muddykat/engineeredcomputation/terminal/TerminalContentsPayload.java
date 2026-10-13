package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;

public record TerminalContentsPayload(
		int containerId, boolean reset, List<CellContents.Entry> entries, TerminalStats stats)
		implements CustomPacketPayload
{
	public static final Type<TerminalContentsPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_contents"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContentsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalContentsPayload::containerId,
			ByteBufCodecs.BOOL, TerminalContentsPayload::reset,
			CellContents.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalContentsPayload::entries,
			TerminalStats.STREAM_CODEC, TerminalContentsPayload::stats,
			TerminalContentsPayload::new);

	@Override
	public Type<TerminalContentsPayload> type()
	{
		return TYPE;
	}

	public static void handle(TerminalContentsPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyUpdate(payload);
	}
}
