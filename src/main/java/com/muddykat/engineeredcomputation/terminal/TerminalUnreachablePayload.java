package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record TerminalUnreachablePayload(int containerId, List<ItemKey> unreachable) implements CustomPacketPayload
{
	public static final Type<TerminalUnreachablePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_unreachable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalUnreachablePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TerminalUnreachablePayload::containerId,
					ItemKey.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalUnreachablePayload::unreachable,
					TerminalUnreachablePayload::new);

	@Override
	public Type<TerminalUnreachablePayload> type()
	{
		return TYPE;
	}

	public static void handle(TerminalUnreachablePayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyUnreachable(payload);
	}
}
