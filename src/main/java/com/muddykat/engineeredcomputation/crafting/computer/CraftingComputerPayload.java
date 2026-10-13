package com.muddykat.engineeredcomputation.crafting.computer;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.crafting.order.OrderView;

public record CraftingComputerPayload(int containerId, List<OrderView> orders) implements CustomPacketPayload
{
	public static final Type<CraftingComputerPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "crafting_computer"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CraftingComputerPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, CraftingComputerPayload::containerId,
					OrderView.STREAM_CODEC.apply(ByteBufCodecs.list(CraftingComputerState.MAX_ORDERS)),
					CraftingComputerPayload::orders,
					CraftingComputerPayload::new);

	@Override
	public Type<CraftingComputerPayload> type()
	{
		return TYPE;
	}

	public static void handle(CraftingComputerPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof CraftingComputerMenu menu&&menu.containerId==payload.containerId())
			menu.applyOrders(payload.orders());
	}
}
