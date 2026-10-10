package com.muddykat.engineeredcomputation.adapter;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public record ProgramEntry(Optional<Holder<Item>> item, Optional<TagKey<Item>> tag)
{
	public static final ProgramEntry EMPTY = new ProgramEntry(Optional.empty(), Optional.empty());

	public static final Codec<ProgramEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Registry.ITEM.holderByNameCodec().optionalFieldOf("item").forGetter(ProgramEntry::item),
			TagKey.codec(Registry.ITEM_REGISTRY).optionalFieldOf("tag").forGetter(ProgramEntry::tag)
	).apply(instance, ProgramEntry::new));

	public static ProgramEntry read(FriendlyByteBuf buf)
	{
		return new ProgramEntry(
				buf.readOptional(in -> Registry.ITEM.byId(in.readVarInt()).builtInRegistryHolder()),
				buf.readOptional(in -> TagKey.create(Registry.ITEM_REGISTRY, in.readResourceLocation())));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeOptional(item, (out, holder) -> out.writeVarInt(Registry.ITEM.getId(holder.value())));
		buf.writeOptional(tag, (out, value) -> out.writeResourceLocation(value.location()));
	}

	public static ProgramEntry of(ItemStack stack)
	{
		return stack.isEmpty()? EMPTY: new ProgramEntry(Optional.of(stack.getItem().builtInRegistryHolder()),
				Optional.empty());
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
		return item.map(holder -> stack.is(holder.value())).orElse(false)||tag.map(stack::is).orElse(false);
	}
}
