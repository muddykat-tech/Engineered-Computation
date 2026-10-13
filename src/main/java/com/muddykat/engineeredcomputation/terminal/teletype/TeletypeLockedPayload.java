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

public record TeletypeLockedPayload(int containerId, List<Entry> locked) implements CustomPacketPayload
{
	public static final Type<TeletypeLockedPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "teletype_locked"));

	public record Entry(ItemKey key, long count)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				ItemKey.STREAM_CODEC, Entry::key,
				ByteBufCodecs.VAR_LONG, Entry::count,
				Entry::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, TeletypeLockedPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TeletypeLockedPayload::containerId,
					Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TeletypeLockedPayload::locked,
					TeletypeLockedPayload::new);

	@Override
	public Type<TeletypeLockedPayload> type()
	{
		return TYPE;
	}

	public static void handle(TeletypeLockedPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TeletypeMenu menu&&menu.containerId==payload.containerId())
			menu.applyLocked(payload);
	}
}
