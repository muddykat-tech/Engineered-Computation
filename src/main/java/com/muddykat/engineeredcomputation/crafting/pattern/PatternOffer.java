package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record PatternOffer(BlockPos processor, int slot, Pattern pattern)
{
	public static final StreamCodec<RegistryFriendlyByteBuf, PatternOffer> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PatternOffer::processor,
			ByteBufCodecs.VAR_INT, PatternOffer::slot,
			Pattern.STREAM_CODEC, PatternOffer::pattern,
			PatternOffer::new);

	public ItemStack shown(long gameTime)
	{
		ItemStack primary = pattern.primaryOutput();
		Optional<TagKey<Item>> tag = pattern.outputTag(pattern.primaryIndex());
		if(tag.isEmpty())
			return primary;
		List<Item> items = Pattern.itemsOf(tag.get());
		return items.isEmpty()? primary: new ItemStack(items.get((int)(gameTime/20%items.size())), primary.getCount());
	}

	public Optional<TagKey<Item>> outputTag()
	{
		return pattern.outputTag(pattern.primaryIndex());
	}

	public record Choice(PatternOffer offer, Map<Integer, Item> picks)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Choice> STREAM_CODEC = StreamCodec.composite(
				PatternOffer.STREAM_CODEC, Choice::offer,
				ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.registry(Registries.ITEM)),
				Choice::picks,
				Choice::new);
	}
}
