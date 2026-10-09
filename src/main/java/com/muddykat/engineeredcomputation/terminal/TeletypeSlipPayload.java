package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.RequestSlip;

public record TeletypeSlipPayload(int containerId, List<RequestSlip.Line> lines, Component outlet, boolean home,
		List<Outlet> outlets) implements CustomPacketPayload
{
	public record Outlet(BlockPos pos, Component label, boolean chosen)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Outlet> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Outlet::pos,
				ComponentSerialization.STREAM_CODEC, Outlet::label,
				ByteBufCodecs.BOOL, Outlet::chosen,
				Outlet::new);
	}

	public static final Type<TeletypeSlipPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "teletype_slip"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeletypeSlipPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TeletypeSlipPayload::containerId,
			RequestSlip.Line.STREAM_CODEC.apply(ByteBufCodecs.list(RequestSlip.SIZE)), TeletypeSlipPayload::lines,
			ComponentSerialization.STREAM_CODEC, TeletypeSlipPayload::outlet,
			ByteBufCodecs.BOOL, TeletypeSlipPayload::home,
			Outlet.STREAM_CODEC.apply(ByteBufCodecs.list()), TeletypeSlipPayload::outlets,
			TeletypeSlipPayload::new);

	@Override
	public Type<TeletypeSlipPayload> type()
	{
		return TYPE;
	}

	public static void handle(TeletypeSlipPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applySlip(payload);
	}
}
