package com.muddykat.engineeredcomputation.storage;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;


public final class ItemKey
{
	public static final Codec<ItemKey> CODEC = ItemStack.CODEC.xmap(ItemKey::of, key -> key.stack);

	private final ItemStack stack;
	private final int hash;
	@Nullable
	private ItemKey received;
	@Nullable
	private ItemStack display;

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

	/**
	 * Finds the stored key a client sent back. Items can come back with different NBT (share tags) and without their
	 * capabilities, so a key that has been to the client and back may no longer equal the one it was made from.
	 */
	public static ItemKey resolve(ItemKey sent, Collection<ItemKey> known)
	{
		if(known.contains(sent))
			return sent;
		for(ItemKey key : known)
			if(key.getItem()==sent.getItem()&&key.received().equals(sent))
				return key;
		return sent;
	}

	private ItemKey received()
	{
		if(received==null)
			received = roundTrip().roundTrip();
		return received;
	}

	private ItemKey roundTrip()
	{
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try
		{
			write(buf);
			return read(buf);
		}
		finally
		{
			buf.release();
		}
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

	/**
	 * A copy for drawing and tooltips. Some mods write into the stacks they draw which can screw with how we tell items
	 * apart from each other, if we don't use a copy our item guards will stop you from removing or taking via gui
	 */
	public ItemStack displayStack()
	{
		if(display==null)
			display = stack.copy();
		return display;
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
