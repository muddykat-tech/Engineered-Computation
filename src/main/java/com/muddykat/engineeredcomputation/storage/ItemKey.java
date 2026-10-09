package com.muddykat.engineeredcomputation.storage;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemKey
{
	public static final Codec<ItemKey> CODEC = ItemStack.SINGLE_ITEM_CODEC.xmap(ItemKey::of, key -> key.stack);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemKey> STREAM_CODEC =
			ItemStack.STREAM_CODEC.map(ItemKey::of, key -> key.stack);

	private final ItemStack stack;
	private final int hash;

	private ItemKey(ItemStack stack)
	{
		this.stack = stack;
		this.hash = ItemStack.hashItemAndComponents(stack);
	}

	public static ItemKey of(ItemStack stack)
	{
		if(stack.isEmpty())
			throw new IllegalArgumentException("Empty stacks have no item key");
		return new ItemKey(stack.copyWithCount(1));
	}

	public Item getItem()
	{
		return stack.getItem();
	}

	public int getMaxStackSize()
	{
		return stack.getMaxStackSize();
	}

	public ItemStack toStack(int count)
	{
		return stack.copyWithCount(count);
	}

	public ItemStack displayStack()
	{
		return stack;
	}

	public boolean matches(ItemStack other)
	{
		return ItemStack.isSameItemSameComponents(stack, other);
	}

	@Override
	public boolean equals(Object o)
	{
		return o==this||o instanceof ItemKey other&&hash==other.hash&&ItemStack.isSameItemSameComponents(stack, other.stack);
	}

	@Override
	public int hashCode()
	{
		return hash;
	}

	@Override
	public String toString()
	{
		return "ItemKey["+stack+"]";
	}
}
