package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record OrderView(OrderTarget target, long amount, StatusCode status, float progress, List<Node> tree)
{
	public static final int MAX_NODES = 64;

	public static OrderView read(FriendlyByteBuf buf)
	{
		return new OrderView(OrderTarget.read(buf), buf.readVarLong(), buf.readEnum(StatusCode.class), buf.readFloat(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_NODES), Node::read));
	}

	public void write(FriendlyByteBuf buf)
	{
		target.write(buf);
		buf.writeVarLong(amount);
		buf.writeEnum(status);
		buf.writeFloat(progress);
		buf.writeCollection(tree, (out, node) -> node.write(out));
	}

	public record Node(int depth, ItemKey item, long amount, boolean crafted, boolean machine, float progress)
	{
		public static Node read(FriendlyByteBuf buf)
		{
			return new Node(buf.readVarInt(), ItemKey.read(buf), buf.readVarLong(), buf.readBoolean(),
					buf.readBoolean(),
					buf.readFloat());
		}

		public void write(FriendlyByteBuf buf)
		{
			buf.writeVarInt(depth);
			item.write(buf);
			buf.writeVarLong(amount);
			buf.writeBoolean(crafted);
			buf.writeBoolean(machine);
			buf.writeFloat(progress);
		}
	}
}
