package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public record OrderPlanPayload(int containerId, ItemKey target, long amount, List<Line> lines, StatusCode status,
		boolean placed)
{
	public static final int MAX_LINES = 64;

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		target.write(buf);
		buf.writeVarLong(amount);
		buf.writeCollection(lines, (out, line) -> line.write(out));
		buf.writeEnum(status);
		buf.writeBoolean(placed);
	}

	public static OrderPlanPayload decode(FriendlyByteBuf buf)
	{
		return new OrderPlanPayload(buf.readVarInt(), ItemKey.read(buf), buf.readVarLong(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_LINES), Line::read),
						buf.readEnum(StatusCode.class),
				buf.readBoolean());
	}

	public record Line(ItemKey item, long fromStorage, long crafted, long missing)
	{
		public void write(FriendlyByteBuf buf)
		{
			item.write(buf);
			buf.writeVarLong(fromStorage);
			buf.writeVarLong(crafted);
			buf.writeVarLong(missing);
		}

		public static Line read(FriendlyByteBuf buf)
		{
			return new Line(ItemKey.read(buf), buf.readVarLong(), buf.readVarLong(), buf.readVarLong());
		}
	}

	public static void handle(OrderPlanPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyPlan(payload);
	}
}
