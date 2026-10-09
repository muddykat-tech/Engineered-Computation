package com.muddykat.engineeredcomputation.crafting;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public record OrderRequestPayload(int containerId, ItemKey target, long amount, boolean place,
		Optional<PatternOffer.Choice> offer)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		target.write(buf);
		buf.writeVarLong(amount);
		buf.writeBoolean(place);
		buf.writeOptional(offer, (out, value) -> value.write(out));
	}

	public static OrderRequestPayload decode(FriendlyByteBuf buf)
	{
		return new OrderRequestPayload(buf.readVarInt(), ItemKey.read(buf), buf.readVarLong(), buf.readBoolean(),
				buf.readOptional(PatternOffer.Choice::read));
	}

	public OrderRequestPayload(int containerId, ItemKey target, long amount, boolean place)
	{
		this(containerId, target, amount, place, Optional.empty());
	}

	public static void handle(OrderRequestPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.handleOrderRequest(payload.target(), payload.amount(), payload.place(), payload.offer());
	}
}
