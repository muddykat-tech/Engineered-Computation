package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** An edit to entry {@code index} of the held program floppy; SET_ITEM sets {@code item}, or else the carried stack. */
public record ProgramEditPayload(int containerId, int index, Action action, String tag, ItemStack item)
{
	public ProgramEditPayload(int containerId, int index, Action action, String tag)
	{
		this(containerId, index, action, tag, ItemStack.EMPTY);
	}

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeVarInt(index);
		buf.writeEnum(action);
		buf.writeUtf(tag, 256);
		buf.writeItem(item);
	}

	public static ProgramEditPayload decode(FriendlyByteBuf buf)
	{
		return new ProgramEditPayload(buf.readVarInt(), buf.readVarInt(), buf.readEnum(Action.class),
				buf.readUtf(256), buf.readItem());
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
			menu.edit(payload.index(), payload.action(), payload.tag(), payload.item());
	}
}
