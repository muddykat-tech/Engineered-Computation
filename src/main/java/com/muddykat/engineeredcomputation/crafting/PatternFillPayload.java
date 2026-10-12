package com.muddykat.engineeredcomputation.crafting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/** A whole pattern for the held pattern floppy (see PatternFloppyMenu.fill). */
public record PatternFillPayload(int containerId, Pattern pattern)
{
	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		pattern.write(buf);
	}

	public static PatternFillPayload decode(FriendlyByteBuf buf)
	{
		return new PatternFillPayload(buf.readVarInt(), Pattern.read(buf));
	}

	public static void handle(PatternFillPayload payload, Player player)
	{
		if(player.containerMenu instanceof PatternFloppyMenu menu&&menu.containerId==payload.containerId())
			menu.fill(payload.pattern());
	}
}
