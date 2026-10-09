package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public record TerminalUnreachablePayload(int containerId, List<ItemKey> unreachable)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeCollection(unreachable, (out, key) -> key.write(out));
	}

	public static TerminalUnreachablePayload decode(FriendlyByteBuf buf)
	{
		return new TerminalUnreachablePayload(buf.readVarInt(), buf.readList(ItemKey::read));
	}

	public static void handle(TerminalUnreachablePayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyUnreachable(payload);
	}
}
