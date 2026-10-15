package com.muddykat.engineeredcomputation.remote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.storage.ItemKey;

/** What a data network's Storage Analyzers can get for its orders. */
public final class RemoteSources
{
	public record Source(BlockPos analyzer, StorageAnalyzerBlockEntity.Priority priority, Map<ItemKey, Long> stock)
	{
	}

	private RemoteSources()
	{
	}

	/**
	 * Each inventory counts once: for the analyzer whose setting lets plans use it most, and one facing it directly
	 * over one reaching it through a Pneumatic Network, as taking from it straight away beats waiting for carriers.
	 */
	public static List<Source> of(Level level, StorageNetwork network)
	{
		if(network.isTooLarge()||!network.isPowered(level))
			return List.of();
		List<StorageAnalyzerBlockEntity> analyzers = new ArrayList<>(network.getAnalyzers());
		analyzers.sort(Comparator.comparing(StorageAnalyzerBlockEntity::getPriority)
				.thenComparing(analyzer -> analyzer.station()!=null));
		Set<BlockPos> claimed = new HashSet<>();
		List<Source> sources = new ArrayList<>();
		for(StorageAnalyzerBlockEntity analyzer : analyzers)
		{
			Map<ItemKey, Long> stock = new HashMap<>();
			analyzer.places().forEach((inventory, place) -> {
				if(claimed.add(inventory))
					place.contents().forEach((key, count) -> stock.merge(key, count, Long::sum));
			});
			if(!stock.isEmpty())
				sources.add(new Source(analyzer.getBlockPos(), analyzer.getPriority(), Map.copyOf(stock)));
		}
		return sources;
	}
}
