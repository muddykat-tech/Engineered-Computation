package com.muddykat.engineeredcomputation.adapter;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ProgramEntry(Optional<Holder<Item>> item, Optional<TagKey<Item>> tag)
{
	public static final ProgramEntry EMPTY = new ProgramEntry(Optional.empty(), Optional.empty());

	public static final Codec<ProgramEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("item").forGetter(ProgramEntry::item),
			TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ProgramEntry::tag)
	).apply(instance, ProgramEntry::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProgramEntry> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM)), ProgramEntry::item,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id),
					TagKey::location)), ProgramEntry::tag,
			ProgramEntry::new);

	public static ProgramEntry of(ItemStack stack)
	{
		return stack.isEmpty()? EMPTY: new ProgramEntry(Optional.of(stack.getItemHolder()), Optional.empty());
	}

	public static ProgramEntry of(TagKey<Item> tag)
	{
		return new ProgramEntry(Optional.empty(), Optional.of(tag));
	}

	public boolean isEmpty()
	{
		return item.isEmpty()&&tag.isEmpty();
	}

	public boolean matches(ItemStack stack)
	{
		return item.map(stack::is).orElse(false)||tag.map(stack::is).orElse(false);
	}
}
