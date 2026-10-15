package com.muddykat.engineeredcomputation.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.storage.ItemKey;

/** What a data network's Storage Analyzers report, which the network can see but doesn't hold. */
public final class RemoteStock
{
	/** How much of an item is in analyzed inventories, and how much in analyzed Pneumatic Networks. */
	public record Stock(ItemKey key, long stored, long pneumatic)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Stock> STREAM_CODEC = StreamCodec.composite(
				ItemKey.STREAM_CODEC, Stock::key,
				ByteBufCodecs.VAR_LONG, Stock::stored,
				ByteBufCodecs.VAR_LONG, Stock::pneumatic,
				Stock::new);

		public long total()
		{
			return stored+pneumatic;
		}
	}

	private RemoteStock()
	{
	}

	/** Each inventory counts once, however many analyzers see it, as the analyzers would get it: see RemoteSources. */
	public static List<Stock> of(Level level, StorageNetwork network)
	{
		if(network.isTooLarge()||!network.isPowered(level))
			return List.of();
		Map<BlockPos, StorageAnalyzerBlockEntity.Place> places = new HashMap<>();
		for(StorageAnalyzerBlockEntity analyzer : network.getAnalyzers())
			analyzer.places().forEach((pos, place) -> places.merge(pos, place, (a, b) -> a.pneumatic()? b: a));
		Map<ItemKey, long[]> totals = new LinkedHashMap<>();
		places.values().forEach(place -> place.contents().forEach((key, count) ->
				totals.computeIfAbsent(key, k -> new long[2])[place.pneumatic()? 1: 0] += count));
		List<Stock> stock = new ArrayList<>();
		totals.forEach((key, counts) -> stock.add(new Stock(key, counts[0], counts[1])));
		return stock;
	}
}
