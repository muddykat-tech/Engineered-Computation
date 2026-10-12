package com.muddykat.engineeredcomputation.adapter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

/**
 * An item holding filter entries (items or tags) in its {@link Program}, edited in hand by {@link ProgramFloppyMenu}:
 * a Program Floppy has nine, a Filter Card one.
 */
public interface ProgrammableItem
{
	/** How many of the program's entries this item uses, from the first. */
	int programSize();

	static void open(ServerPlayer player, InteractionHand hand)
	{
		NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new ProgramFloppyMenu(id, inventory,
				hand), player.getItemInHand(hand).getHoverName()), buf -> buf.writeEnum(hand));
	}
}
