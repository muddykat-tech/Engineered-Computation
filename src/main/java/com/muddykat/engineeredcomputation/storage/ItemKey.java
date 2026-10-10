package com.muddykat.engineeredcomputation.storage;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;


public final class ItemKey
{
	public static final Codec<ItemKey> CODEC = ItemStack.CODEC.xmap(ItemKey::of, key -> key.stack);

	private final ItemStack stack;
	private final int hash;

	private ItemKey(ItemStack stack)
	{
		this.stack = stack;
		this.hash = 31*(31+stack.getItem().hashCode())+(stack.getTag()==null? 0: stack.getTag().hashCode());
	}

	public static ItemKey read(FriendlyByteBuf buf)
	{
		return of(buf.readItem());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeItem(stack);
	}

	public static ItemKey of(ItemStack stack)
	{
		if(stack.isEmpty())
			throw new IllegalArgumentException("Empty stacks have no item key");
		return new ItemKey(ItemHandlerHelper.copyStackWithSize(stack, 1));
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
		return ItemHandlerHelper.copyStackWithSize(stack, count);
	}

	public ItemStack displayStack()
	{
		return stack;
	}

	public boolean matches(ItemStack other)
	{
		return ItemStack.isSameItemSameTags(stack, other);
	}

	@Override
	public boolean equals(Object o)
	{
		return o==this||o instanceof ItemKey other&&hash==other.hash&&ItemStack.isSameItemSameTags(stack, other.stack);
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
