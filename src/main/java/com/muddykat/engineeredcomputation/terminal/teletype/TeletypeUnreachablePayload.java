package com.muddykat.engineeredcomputation.terminal.teletype;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record TeletypeUnreachablePayload(int containerId, List<ItemKey> unreachable) implements CustomPacketPayload
{
	public static final Type<TeletypeUnreachablePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_unreachable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeletypeUnreachablePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TeletypeUnreachablePayload::containerId,
					ItemKey.STREAM_CODEC.apply(ByteBufCodecs.list()), TeletypeUnreachablePayload::unreachable,
					TeletypeUnreachablePayload::new);

	@Override
	public Type<TeletypeUnreachablePayload> type()
	{
		return TYPE;
	}

	public static void handle(TeletypeUnreachablePayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TeletypeMenu menu&&menu.containerId==payload.containerId())
			menu.applyUnreachable(payload);
	}
}
