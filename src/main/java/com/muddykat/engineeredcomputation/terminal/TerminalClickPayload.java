package com.muddykat.engineeredcomputation.terminal;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public record TerminalClickPayload(int containerId, Optional<ItemKey> key, TerminalAction action)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeOptional(key, (out, value) -> value.write(out));
		buf.writeEnum(action);
	}

	public static TerminalClickPayload decode(FriendlyByteBuf buf)
	{
		return new TerminalClickPayload(buf.readVarInt(), buf.readOptional(ItemKey::read),
				buf.readEnum(TerminalAction.class));
	}

	public static void handle(TerminalClickPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.handleClick(payload.key().orElse(null), payload.action());
	}
}
