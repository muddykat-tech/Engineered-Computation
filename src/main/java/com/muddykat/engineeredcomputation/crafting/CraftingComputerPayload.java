package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record CraftingComputerPayload(int containerId, List<OrderView> orders)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeCollection(orders, (out, order) -> order.write(out));
	}

	public static CraftingComputerPayload decode(FriendlyByteBuf buf)
	{
		return new CraftingComputerPayload(buf.readVarInt(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, CraftingComputerState.MAX_ORDERS),
						OrderView::read));
	}

	public static void handle(CraftingComputerPayload payload, Player player)
	{
		if(player.containerMenu instanceof CraftingComputerMenu menu&&menu.containerId==payload.containerId())
			menu.applyOrders(payload.orders());
	}
}
