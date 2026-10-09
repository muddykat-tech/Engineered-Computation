package com.muddykat.engineeredcomputation.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public record PatternOffer(BlockPos processor, int slot, Pattern pattern)
{
	public static PatternOffer read(FriendlyByteBuf buf)
	{
		return new PatternOffer(buf.readBlockPos(), buf.readVarInt(), Pattern.read(buf));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeBlockPos(processor);
		buf.writeVarInt(slot);
		pattern.write(buf);
	}

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
		public static Choice read(FriendlyByteBuf buf)
		{
			return new Choice(PatternOffer.read(buf),
					buf.readMap(HashMap::new, FriendlyByteBuf::readVarInt, in -> in.readById(BuiltInRegistries.ITEM)));
		}

		public void write(FriendlyByteBuf buf)
		{
			offer.write(buf);
			buf.writeMap(picks, FriendlyByteBuf::writeVarInt, (out, item) -> out.writeId(BuiltInRegistries.ITEM, item));
		}
	}
}
