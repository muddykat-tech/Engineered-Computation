package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record TeletypeOutletPayload(int containerId, BlockPos outlet)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeBlockPos(outlet);
	}

	public static TeletypeOutletPayload decode(FriendlyByteBuf buf)
	{
		return new TeletypeOutletPayload(buf.readVarInt(), buf.readBlockPos());
	}

	public static void handle(TeletypeOutletPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.chooseOutlet(payload.outlet());
	}
}
