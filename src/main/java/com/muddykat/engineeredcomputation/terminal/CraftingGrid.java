package com.muddykat.engineeredcomputation.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CraftingGrid
{
	public static final int WIDTH = 3;
	public static final int SIZE = WIDTH*WIDTH;

	private final NonNullList<ItemStack> slots = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final Runnable onChanged;
	private boolean linked = true;
	private int version;

	public CraftingGrid(Runnable onChanged)
	{
		this.onChanged = onChanged;
	}

	static CraftingGrid detached()
	{
		return new CraftingGrid(() -> {});
	}

	NonNullList<ItemStack> slots()
	{
		return slots;
	}

	public ItemStack getItem(int slot)
	{
		return slots.get(slot);
	}

	public boolean isLinked()
	{
		return linked;
	}

	void setLinked(boolean linked)
	{
		if(this.linked!=linked)
		{
			this.linked = linked;
			changed();
		}
	}

	int version()
	{
		return version;
	}

	void changed()
	{
		version++;
		onChanged.run();
	}

	public void dropContents(Level level, BlockPos pos)
	{
		if(!linked)
			Containers.dropContents(level, pos, slots);
		slots.replaceAll(stack -> ItemStack.EMPTY);
	}

	public CompoundTag save()
	{
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("linked", linked);
		ContainerHelper.saveAllItems(tag, slots, true);
		return tag;
	}

	public void load(CompoundTag tag)
	{
		linked = !tag.contains("linked")||tag.getBoolean("linked");
		slots.replaceAll(stack -> ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, slots);
	}
}
