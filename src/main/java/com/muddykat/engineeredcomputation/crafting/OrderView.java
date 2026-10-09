package com.muddykat.engineeredcomputation.crafting;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record OrderView(OrderTarget target, long amount, StatusCode status, float progress, List<Node> tree)
{
	public static final int MAX_NODES = 64;

	public static final StreamCodec<RegistryFriendlyByteBuf, OrderView> STREAM_CODEC = StreamCodec.composite(
			OrderTarget.STREAM_CODEC, OrderView::target,
			ByteBufCodecs.VAR_LONG, OrderView::amount,
			NeoForgeStreamCodecs.enumCodec(StatusCode.class), OrderView::status,
			ByteBufCodecs.FLOAT, OrderView::progress,
			Node.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NODES)), OrderView::tree,
			OrderView::new);

	public record Node(int depth, ItemKey item, long amount, boolean crafted, boolean machine, float progress)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Node> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Node::depth,
				ItemKey.STREAM_CODEC, Node::item,
				ByteBufCodecs.VAR_LONG, Node::amount,
				ByteBufCodecs.BOOL, Node::crafted,
				ByteBufCodecs.BOOL, Node::machine,
				ByteBufCodecs.FLOAT, Node::progress,
				Node::new);
	}
}
