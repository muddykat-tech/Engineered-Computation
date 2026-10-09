package com.muddykat.engineeredcomputation.crafting;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public record TerminalCraftingPayload(int containerId, List<ItemKey> craftables, List<PatternOffer> offers,
		List<OrderSummary> orders)
		implements CustomPacketPayload
{
	public static final int MAX = 4096;

	public static final Type<TerminalCraftingPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "terminal_crafting"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalCraftingPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TerminalCraftingPayload::containerId,
					ItemKey.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), TerminalCraftingPayload::craftables,
					PatternOffer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), TerminalCraftingPayload::offers,
					OrderSummary.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), TerminalCraftingPayload::orders,
					TerminalCraftingPayload::new);

	@Override
	public Type<TerminalCraftingPayload> type()
	{
		return TYPE;
	}

	public static void handle(TerminalCraftingPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyCrafting(payload);
	}
}
