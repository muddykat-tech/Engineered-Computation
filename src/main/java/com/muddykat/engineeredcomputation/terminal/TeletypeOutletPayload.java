package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public record TeletypeOutletPayload(int containerId, BlockPos outlet) implements CustomPacketPayload
{
	public static final Type<TeletypeOutletPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "teletype_outlet"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeletypeOutletPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TeletypeOutletPayload::containerId,
			BlockPos.STREAM_CODEC, TeletypeOutletPayload::outlet,
			TeletypeOutletPayload::new);

	@Override
	public Type<TeletypeOutletPayload> type()
	{
		return TYPE;
	}

	public static void handle(TeletypeOutletPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.chooseOutlet(payload.outlet());
	}
}
