package com.muddykat.engineeredcomputation.crafting.order;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

public record CancelOrderPayload(int containerId, BlockPos computer, int id) implements CustomPacketPayload
{
	public static final Type<CancelOrderPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "cancel_order"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CancelOrderPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CancelOrderPayload::containerId,
			BlockPos.STREAM_CODEC, CancelOrderPayload::computer,
			ByteBufCodecs.VAR_INT, CancelOrderPayload::id,
			CancelOrderPayload::new);

	@Override
	public Type<CancelOrderPayload> type()
	{
		return TYPE;
	}

	public static void handle(CancelOrderPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof FloppyTerminalMenu menu&&menu.containerId==payload.containerId())
			menu.cancelOrder(payload.computer(), payload.id());
		else if(context.player().containerMenu instanceof CraftingComputerMenu menu
				&&menu.containerId==payload.containerId())
			menu.cancelOrder(payload.id());
	}
}
