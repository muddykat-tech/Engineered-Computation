package com.muddykat.engineeredcomputation.program;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

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

import com.muddykat.engineeredcomputation.terminal.ItemSearch;

/** The server can't see display names (client side only), so name matches item ids instead ("iron" matches iron_ingot). */
public record ProgramEntry(Optional<Holder<Item>> item, Optional<TagKey<Item>> tag, Optional<String> name)
{
	public static final int MAX_NAME = 64;
	public static final ProgramEntry EMPTY = new ProgramEntry(Optional.empty(), Optional.empty(), Optional.empty());

	public static final Codec<ProgramEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("item").forGetter(ProgramEntry::item),
			TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ProgramEntry::tag),
			Codec.string(1, MAX_NAME).optionalFieldOf("name").forGetter(ProgramEntry::name)
	).apply(instance, ProgramEntry::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProgramEntry> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM)), ProgramEntry::item,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id),
					TagKey::location)), ProgramEntry::tag,
			ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(MAX_NAME)), ProgramEntry::name,
			ProgramEntry::new);

	public static ProgramEntry of(ItemStack stack)
	{
		return stack.isEmpty()? EMPTY: new ProgramEntry(Optional.of(stack.getItemHolder()), Optional.empty(),
				Optional.empty());
	}

	public static ProgramEntry of(TagKey<Item> tag)
	{
		return new ProgramEntry(Optional.empty(), Optional.of(tag), Optional.empty());
	}

	@Nullable
	public static ProgramEntry ofName(String name)
	{
		String trimmed = name.trim();
		if(trimmed.isEmpty())
			return null;
		return new ProgramEntry(Optional.empty(), Optional.empty(),
				Optional.of(trimmed.substring(0, Math.min(MAX_NAME, trimmed.length()))));
	}

	public boolean isEmpty()
	{
		return item.isEmpty()&&tag.isEmpty()&&name.isEmpty();
	}

	public boolean matches(ItemStack stack)
	{
		return item.map(stack::is).orElse(false)||tag.map(stack::is).orElse(false)
				||name.map(query -> idMatches(stack, query)).orElse(false);
	}

	private static boolean idMatches(ItemStack stack, String query)
	{
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return ItemSearch.matches(id.getPath().replace('_', ' '), id, query.replace('_', ' '));
	}
}
