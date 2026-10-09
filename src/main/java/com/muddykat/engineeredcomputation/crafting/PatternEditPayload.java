package com.muddykat.engineeredcomputation.crafting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import com.muddykat.engineeredcomputation.crafting.PatternFloppyMenu;

public record PatternEditPayload(int containerId, Action action, int slot, int amount, String tag)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeEnum(action);
		buf.writeVarInt(slot);
		buf.writeVarInt(amount);
		buf.writeUtf(tag);
	}

	public static PatternEditPayload decode(FriendlyByteBuf buf)
	{
		return new PatternEditPayload(buf.readVarInt(), buf.readEnum(Action.class), buf.readVarInt(),
				buf.readVarInt(), buf.readUtf());
	}

	public PatternEditPayload(int containerId, Action action, int slot, int amount)
	{
		this(containerId, action, slot, amount, "");
	}

	public enum Action
	{
		SET,
		ADJUST,
		CLEAR,
		SET_TAG,
		TOGGLE_MODE,
		CLEAR_ALL
	}

	public static void handle(PatternEditPayload payload, Player player)
	{
		if(player.containerMenu instanceof PatternFloppyMenu menu&&menu.containerId==payload.containerId())
			menu.edit(payload.action(), payload.slot(), payload.amount(), payload.tag());
	}
}
