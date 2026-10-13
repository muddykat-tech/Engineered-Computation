package com.muddykat.engineeredcomputation.storage;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
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
	@Nullable
	private ItemKey received;
	@Nullable
	private ItemStack display;

	private ItemKey(ItemStack stack)
	{
		this.stack = stack;
		this.hash = ItemStack.hashItemAndComponents(stack);
	}

	/**
	 * An item can lose hidden details on its way to a player and back, so the player's copy is compared against what
	 * each stored item would look like after being sent there and back.
	 * some mods have items like this... looking at you Vault Hunters 3.
	 */
	public static ItemKey resolve(ItemKey sent, Collection<ItemKey> known, RegistryAccess registries)
	{
		if(known.contains(sent))
			return sent;
		for(ItemKey key : known)
			if(key.getItem()==sent.getItem()&&key.received(registries).equals(sent))
				return key;
		return sent;
	}

	private ItemKey received(RegistryAccess registries)
	{
		if(received==null)
			received = roundTrip(roundTrip(this, registries), registries);
		return received;
	}

	private static ItemKey roundTrip(ItemKey key, RegistryAccess registries)
	{
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try
		{
			STREAM_CODEC.encode(buf, key);
			return STREAM_CODEC.decode(buf);
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

	/** A copy, so nothing that draws or reads it can change the stored item. */
	public ItemStack displayStack()
	{
		if(display==null)
			display = stack.copy();
		return display;
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
