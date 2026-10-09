package com.muddykat.engineeredcomputation.crafting;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public record OrderTarget(ItemKey item, Optional<TagKey<Item>> tag)
{
	public static OrderTarget read(FriendlyByteBuf buf)
	{
		return new OrderTarget(ItemKey.read(buf),
				buf.readOptional(in -> TagKey.create(Registries.ITEM, in.readResourceLocation())));
	}

	public void write(FriendlyByteBuf buf)
	{
		item.write(buf);
		buf.writeOptional(tag, (out, value) -> out.writeResourceLocation(value.location()));
	}

	public Component name()
	{
		return tag.<Component>map(key -> Component.literal("#"+key.location()))
				.orElseGet(() -> item.displayStack().getHoverName());
	}

	public ItemStack icon(long gameTime)
	{
		if(tag.isEmpty())
			return item.displayStack();
		List<Item> items = Pattern.itemsOf(tag.get());
		return items.isEmpty()? item.displayStack(): new ItemStack(items.get((int)(gameTime/20%items.size())));
	}

	public boolean isTag()
	{
		return tag.isPresent();
	}
}
