package com.muddykat.engineeredcomputation.storage;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;


public record CellContents(List<Entry> entries)
{
	public static final CellContents EMPTY = new CellContents(List.of());

	public static final Codec<CellContents> CODEC =
			Entry.CODEC.listOf().xmap(CellContents::new, CellContents::entries);

	public CellContents
	{
		entries = List.copyOf(entries);
	}

	public static CellContents read(FriendlyByteBuf buf)
	{
		return new CellContents(buf.readList(Entry::read));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeCollection(entries, (out, entry) -> entry.write(out));
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

		public static Entry read(FriendlyByteBuf buf)
		{
			return new Entry(ItemKey.read(buf), buf.readVarLong());
		}

		public void write(FriendlyByteBuf buf)
		{
			key.write(buf);
			buf.writeVarLong(count);
		}
	}
}
