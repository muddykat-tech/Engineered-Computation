package com.muddykat.engineeredcomputation.client.screen.terminal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.terminal.TypewriterMenu;

public class TypewriterScreen extends TerminalScreen<TypewriterMenu>
{
	public TypewriterScreen(TypewriterMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
	}
}
