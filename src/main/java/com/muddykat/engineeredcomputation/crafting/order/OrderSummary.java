package com.muddykat.engineeredcomputation.crafting.order;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import com.muddykat.engineeredcomputation.network.StatusCode;

public record OrderSummary(BlockPos computer, int id, OrderTarget target, long amount, float progress,
		StatusCode status)
{
	public static final StreamCodec<RegistryFriendlyByteBuf, OrderSummary> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OrderSummary::computer,
			ByteBufCodecs.VAR_INT, OrderSummary::id,
			OrderTarget.STREAM_CODEC, OrderSummary::target,
			ByteBufCodecs.VAR_LONG, OrderSummary::amount,
			ByteBufCodecs.FLOAT, OrderSummary::progress,
			NeoForgeStreamCodecs.enumCodec(StatusCode.class), OrderSummary::status,
			OrderSummary::new);

	public static OrderSummary of(BlockPos computer, CraftingOrder order)
	{
		return new OrderSummary(computer, order.id(), order.shownTarget(), order.amount(), order.progress(),
				order.status());
	}
}
