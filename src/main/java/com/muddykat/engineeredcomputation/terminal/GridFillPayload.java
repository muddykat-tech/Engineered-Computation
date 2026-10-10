package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.muddykat.engineeredcomputation.EngineeredComputation;

/** A recipe for the terminal's crafting grid: per slot, the items that would do (see TerminalMenu.fillGrid). */
public record GridFillPayload(int containerId, List<List<ItemStack>> slots, boolean max) implements CustomPacketPayload
{
	/** The most items sent for one slot (a tag's worth, say). */
	public static final int OPTIONS = 64;

	public static final Type<GridFillPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "grid_fill"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GridFillPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GridFillPayload::containerId,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(OPTIONS)).apply(ByteBufCodecs.list(CraftingGrid.SIZE)),
			GridFillPayload::slots,
			ByteBufCodecs.BOOL, GridFillPayload::max,
			GridFillPayload::new);

	@Override
	public Type<GridFillPayload> type()
	{
		return TYPE;
	}

	public static void handle(GridFillPayload payload, IPayloadContext context)
	{
		if(context.player().containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.fillGrid(payload.slots(), payload.max());
	}
}
