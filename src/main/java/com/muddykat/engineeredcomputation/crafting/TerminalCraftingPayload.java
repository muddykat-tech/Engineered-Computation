package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public record TerminalCraftingPayload(int containerId, List<ItemKey> craftables, List<PatternOffer> offers,
		List<OrderSummary> orders)
{
	public static final int MAX = 4096;

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeCollection(craftables, (out, key) -> key.write(out));
		buf.writeCollection(offers, (out, offer) -> offer.write(out));
		buf.writeCollection(orders, (out, order) -> order.write(out));
	}

	public static TerminalCraftingPayload decode(FriendlyByteBuf buf)
	{
		return new TerminalCraftingPayload(buf.readVarInt(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX), ItemKey::read),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX), PatternOffer::read),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX), OrderSummary::read));
	}

	public static void handle(TerminalCraftingPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyCrafting(payload);
	}
}
