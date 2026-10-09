package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.adapter.ProgramFloppyMenu;

public record ProgramEditPayload(int containerId, int index, Action action, String tag)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeVarInt(index);
		buf.writeEnum(action);
		buf.writeUtf(tag, 256);
	}

	public static ProgramEditPayload decode(FriendlyByteBuf buf)
	{
		return new ProgramEditPayload(buf.readVarInt(), buf.readVarInt(), buf.readEnum(Action.class),
				buf.readUtf(256));
	}

	public enum Action
	{
		SET_ITEM,
		SET_TAG,
		CLEAR
	}

	public static void handle(ProgramEditPayload payload, Player player)
	{
		if(player.containerMenu instanceof ProgramFloppyMenu menu&&menu.containerId==payload.containerId())
			menu.edit(payload.index(), payload.action(), payload.tag());
	}
}
