package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.registration.ECMenuTypes;

public class TypewriterMenu extends TerminalMenu
{
	public TypewriterMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf)
	{
		this(id, playerInventory, buf.readBlockPos());
	}

	public TypewriterMenu(int id, Inventory playerInventory, BlockPos terminalPos)
	{
		super(ECMenuTypes.TYPEWRITER.get(), id, playerInventory, terminalPos);
	}

	@Override
	public TerminalFeatures getFeatures()
	{
		return TerminalFeatures.GRID;
	}
}
