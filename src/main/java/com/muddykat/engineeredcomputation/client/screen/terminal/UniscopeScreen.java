package com.muddykat.engineeredcomputation.client.screen.terminal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.terminal.UniscopeMenu;

public class UniscopeScreen extends TerminalScreen<UniscopeMenu>
{
	public UniscopeScreen(UniscopeMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
	}
}
