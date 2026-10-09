package com.muddykat.engineeredcomputation.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import com.muddykat.engineeredcomputation.network.StatusCode;

public record OrderSummary(BlockPos computer, int id, OrderTarget target, long amount, float progress,
		StatusCode status)
{
	public static OrderSummary read(FriendlyByteBuf buf)
	{
		return new OrderSummary(buf.readBlockPos(), buf.readVarInt(), OrderTarget.read(buf), buf.readVarLong(),
				buf.readFloat(), buf.readEnum(StatusCode.class));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeBlockPos(computer);
		buf.writeVarInt(id);
		target.write(buf);
		buf.writeVarLong(amount);
		buf.writeFloat(progress);
		buf.writeEnum(status);
	}

	static OrderSummary of(BlockPos computer, CraftingOrder order)
	{
		return new OrderSummary(computer, order.id(), order.shownTarget(), order.amount(), order.progress(),
				order.status());
	}
}
