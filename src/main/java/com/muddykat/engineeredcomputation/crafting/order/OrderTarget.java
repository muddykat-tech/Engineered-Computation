package com.muddykat.engineeredcomputation.crafting.order;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public record OrderTarget(ItemKey item, Optional<TagKey<Item>> tag)
{
	public static final StreamCodec<RegistryFriendlyByteBuf, OrderTarget> STREAM_CODEC = StreamCodec.composite(
			ItemKey.STREAM_CODEC, OrderTarget::item,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id),
					TagKey::location)), OrderTarget::tag,
			OrderTarget::new);

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
