package com.muddykat.engineeredcomputation.crafting.order;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record OrderView(int id, OrderTarget target, long amount, StatusCode status, float progress, long started,
		List<Node> tree)
{
	public static final int MAX_NODES = 64;
	private static final StreamCodec<RegistryFriendlyByteBuf, StatusCode> STATUS = NeoForgeStreamCodecs.enumCodec(
			StatusCode.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, List<Node>> TREE = Node.STREAM_CODEC.apply(
			ByteBufCodecs.list(MAX_NODES));

	public static final StreamCodec<RegistryFriendlyByteBuf, OrderView> STREAM_CODEC = StreamCodec.of(
			(buf, view) -> {
				buf.writeVarInt(view.id);
				OrderTarget.STREAM_CODEC.encode(buf, view.target);
				buf.writeVarLong(view.amount);
				STATUS.encode(buf, view.status);
				buf.writeFloat(view.progress);
				buf.writeVarLong(view.started);
				TREE.encode(buf, view.tree);
			},
			buf -> new OrderView(buf.readVarInt(), OrderTarget.STREAM_CODEC.decode(buf), buf.readVarLong(),
					STATUS.decode(buf), buf.readFloat(), buf.readVarLong(), TREE.decode(buf)));

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
