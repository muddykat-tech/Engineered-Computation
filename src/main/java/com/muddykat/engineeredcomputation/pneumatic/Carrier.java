package com.muddykat.engineeredcomputation.pneumatic;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class Carrier
{
	public static final int TICKS_PER_BLOCK = 2;

	private final long id;
	private ItemStack items;
	private final long[] path;
	private final long launched;
	private int passed;

	Carrier(long id, ItemStack items, long[] path, long launched, int passed)
	{
		this.id = id;
		this.items = items;
		this.path = path;
		this.launched = launched;
		this.passed = passed;
	}

	static Carrier launch(long id, ItemStack items, List<BlockPos> path, long now)
	{
		return new Carrier(id, items, path.stream().mapToLong(BlockPos::asLong).toArray(), now, 0);
	}

	public long getId()
	{
		return id;
	}

	long[] getPathLongs()
	{
		return path.clone();
	}

	public ItemStack getItems()
	{
		return items;
	}

	void setItems(ItemStack items)
	{
		this.items = items;
	}

	public List<BlockPos> getPath()
	{
		return Arrays.stream(path).mapToObj(BlockPos::of).toList();
	}

	public BlockPos getDestination()
	{
		return BlockPos.of(path[path.length-1]);
	}

	public long getLaunched()
	{
		return launched;
	}

	int getPassed()
	{
		return passed;
	}

	void setPassed(int passed)
	{
		this.passed = passed;
	}

	BlockPos at(int index)
	{
		return BlockPos.of(path[index]);
	}

	int lastIndex()
	{
		return path.length-1;
	}

	int reached(long now)
	{
		return (int)Math.min(lastIndex(), Math.max(0, now-launched)/TICKS_PER_BLOCK);
	}

	CompoundTag save(HolderLookup.Provider registries)
	{
		CompoundTag tag = new CompoundTag();
		tag.put("items", items.saveOptional(registries));
		tag.putLongArray("path", path);
		tag.putLong("launched", launched);
		tag.putInt("passed", passed);
		tag.putLong("id", id);
		return tag;
	}

	static Carrier load(CompoundTag tag, HolderLookup.Provider registries)
	{
		return new Carrier(tag.getLong("id"), ItemStack.parseOptional(registries, tag.getCompound("items")),
				tag.getLongArray("path"),
				tag.getLong("launched"), tag.getInt("passed"));
	}
}
