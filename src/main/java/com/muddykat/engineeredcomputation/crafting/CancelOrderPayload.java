package com.muddykat.engineeredcomputation.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public record CancelOrderPayload(int containerId, BlockPos computer, int id)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeBlockPos(computer);
		buf.writeVarInt(id);
	}

	public static CancelOrderPayload decode(FriendlyByteBuf buf)
	{
		return new CancelOrderPayload(buf.readVarInt(), buf.readBlockPos(), buf.readVarInt());
	}

	public static void handle(CancelOrderPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.cancelOrder(payload.computer(), payload.id());
	}
}
