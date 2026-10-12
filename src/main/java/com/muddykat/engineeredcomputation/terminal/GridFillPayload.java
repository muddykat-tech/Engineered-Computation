package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A recipe for the terminal's crafting grid: per slot, the items that would do (see TerminalMenu.fillGrid). */
public record GridFillPayload(int containerId, List<List<ItemStack>> slots, boolean max)
{
	/** The most items sent for one slot (a tag's worth, say). */
	public static final int OPTIONS = 64;

	public void encode(FriendlyByteBuf buf)
	{
		buf.writeVarInt(containerId);
		buf.writeCollection(slots, (out, options) -> out.writeCollection(options, FriendlyByteBuf::writeItem));
		buf.writeBoolean(max);
	}

	public static GridFillPayload decode(FriendlyByteBuf buf)
	{
		return new GridFillPayload(buf.readVarInt(),
				buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, CraftingGrid.SIZE),
						in -> in.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, OPTIONS),
								FriendlyByteBuf::readItem)),
				buf.readBoolean());
	}

	public static void handle(GridFillPayload payload, Player player)
	{
		if(player.containerMenu instanceof TerminalMenu menu&&menu.containerId==payload.containerId())
			menu.fillGrid(payload.slots(), payload.max());
	}
}
