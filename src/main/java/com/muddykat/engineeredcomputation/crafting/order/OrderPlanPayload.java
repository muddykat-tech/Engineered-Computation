package com.muddykat.engineeredcomputation.crafting.order;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

public record OrderPlanPayload(int containerId, ItemKey target, long amount, List<Line> lines, StatusCode status,
		boolean placed) implements CustomPacketPayload
{
	public static final int MAX_LINES = 64;

	public static final Type<OrderPlanPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "order_plan"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OrderPlanPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OrderPlanPayload::containerId,
			ItemKey.STREAM_CODEC, OrderPlanPayload::target,
			ByteBufCodecs.VAR_LONG, OrderPlanPayload::amount,
			Line.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)), OrderPlanPayload::lines,
			NeoForgeStreamCodecs.enumCodec(StatusCode.class), OrderPlanPayload::status,
			ByteBufCodecs.BOOL, OrderPlanPayload::placed,
			OrderPlanPayload::new);

	public record Line(ItemKey item, long fromStorage, long crafted, long missing)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
				ItemKey.STREAM_CODEC, Line::item,
				ByteBufCodecs.VAR_LONG, Line::fromStorage,
				ByteBufCodecs.VAR_LONG, Line::crafted,
				ByteBufCodecs.VAR_LONG, Line::missing,
				Line::new);
	}

	@Override
	public Type<OrderPlanPayload> type()
	{
		return TYPE;
	}

	public static void handle(OrderPlanPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof FloppyTerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyPlan(payload);
	}
}
