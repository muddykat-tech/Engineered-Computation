package com.muddykat.engineeredcomputation.program;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public interface ProgrammableItem
{
	int programSize();

	static void open(ServerPlayer player, InteractionHand hand)
	{
		ProgramFloppyMenu.openHeld(player, hand);
	}
}
