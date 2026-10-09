package com.muddykat.engineeredcomputation.terminal;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.storage.CellContents;

public record TerminalContentsPayload(
		int containerId, boolean reset, List<CellContents.Entry> entries, TerminalStats stats)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeBoolean(reset);
		buf.writeCollection(entries, (out, entry) -> entry.write(out));
		stats.write(buf);
	}

	public static TerminalContentsPayload decode(FriendlyByteBuf buf)
	{
		return new TerminalContentsPayload(buf.readVarInt(), buf.readBoolean(), buf.readList(CellContents.Entry::read),
				TerminalStats.read(buf));
	}

	public static void handle(TerminalContentsPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.applyUpdate(payload);
	}
}
