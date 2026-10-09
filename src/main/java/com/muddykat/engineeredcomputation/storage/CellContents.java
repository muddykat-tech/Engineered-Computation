package com.muddykat.engineeredcomputation.storage;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CellContents(List<Entry> entries)
{
	public static final CellContents EMPTY = new CellContents(List.of());

	public static final Codec<CellContents> CODEC =
			Entry.CODEC.listOf().xmap(CellContents::new, CellContents::entries);
	public static final StreamCodec<RegistryFriendlyByteBuf, CellContents> STREAM_CODEC =
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(CellContents::new, CellContents::entries);

	public CellContents
	{
		entries = List.copyOf(entries);
	}

	public static CellContents of(Map<ItemKey, Long> amounts)
	{
		return new CellContents(amounts.entrySet().stream()
				.filter(e -> e.getValue()>0)
				.map(e -> new Entry(e.getKey(), e.getValue()))
				.toList());
	}

	public int types()
	{
		return entries.size();
	}

	public long totalCount()
	{
		long total = 0;
		for(Entry entry : entries)
			total += entry.count();
		return total;
	}

	public record Entry(ItemKey key, long count)
	{
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemKey.CODEC.fieldOf("item").forGetter(Entry::key),
				Codec.LONG.fieldOf("count").forGetter(Entry::count)
		).apply(instance, Entry::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				ItemKey.STREAM_CODEC, Entry::key,
				ByteBufCodecs.VAR_LONG, Entry::count,
				Entry::new);
	}
}
