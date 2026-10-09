package com.muddykat.engineeredcomputation.pneumatic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record StationNamePayload(int containerId, String name)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeUtf(name, TubeStationBlockEntity.MAX_NAME*4);
	}

	public static StationNamePayload decode(FriendlyByteBuf buf)
	{
		return new StationNamePayload(buf.readVarInt(), buf.readUtf(TubeStationBlockEntity.MAX_NAME*4));
	}

	public static void handle(StationNamePayload payload, Player player)
	{
		if(player.containerMenu instanceof NamedStationMenu menu
				&&player.containerMenu.containerId==payload.containerId())
			menu.rename(payload.name());
	}
}
