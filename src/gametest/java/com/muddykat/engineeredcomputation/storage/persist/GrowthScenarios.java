package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveStrategy;

/** Workloads that grow, shrink and fragment cell storage, works for any file save strategy. */
final class GrowthScenarios
{
	static final int MAX_TYPES = 63;

	static final NamedScenario GAINING_TYPES = new NamedScenario(
			"512 cells gain 1-3 new types every save, 40 saves", work -> {
		List<UUID> ids = work.addCells(512, 8, 8);
		work.save(Set.copyOf(ids));
		for(int round = 0; round<40; round++)
		{
			ids.forEach(id -> work.grow(id, 1+work.random.nextInt(3)));
			work.save(Set.copyOf(ids));
		}
	});

	static final NamedScenario COUNTS_ONLY = new NamedScenario(
			"512 cells, only counts change, 40 saves", work -> {
		List<UUID> ids = work.addCells(512, 8, MAX_TYPES);
		work.save(Set.copyOf(ids));
		for(int round = 0; round<40; round++)
		{
			ids.forEach(work::churn);
			work.save(Set.copyOf(ids));
		}
	});

	static final NamedScenario NEW_CELLS = new NamedScenario(
			"128 new cells per save up to 2048, 10% of the rest change counts", work -> {
		for(int round = 0; round<16; round++)
		{
			List<UUID> existing = List.copyOf(work.live.keySet());
			Set<UUID> dirty = new HashSet<>(work.addCells(128, 8, MAX_TYPES));
			for(UUID id : existing)
				if(work.random.nextInt(10)==0)
				{
					work.churn(id);
					dirty.add(id);
				}
			work.save(dirty);
		}
	});

	static final NamedScenario MIXED = new NamedScenario(
			"1024 cells, each save: 5% gain types, 20% change counts, 1% destroyed, 1% new; 50 saves", work -> {
		work.save(Set.copyOf(work.addCells(1024, 8, MAX_TYPES)));
		for(int round = 0; round<50; round++)
		{
			Set<UUID> dirty = new HashSet<>();
			for(UUID id : List.copyOf(work.live.keySet()))
			{
				int roll = work.random.nextInt(100);
				if(roll<5)
					work.grow(id, 1+work.random.nextInt(3));
				else if(roll<25)
					work.churn(id);
				else if(roll<26)
					work.destroy(id);
				else
					continue;
				dirty.add(id);
			}
			dirty.addAll(work.addCells(10, 8, MAX_TYPES));
			work.save(dirty);
		}
	});

	static final NamedScenario INTERLEAVED_GROWTH = new NamedScenario(
			"GAPS: 512 cells of named gear side by side, every other one gains 2 types per save, 30 saves", work -> {
		List<UUID> ids = work.addGearCells(512, 4);
		work.save(Set.copyOf(ids));
		for(int round = 0; round<30; round++)
		{
			for(int i = 0; i<ids.size(); i++)
				if(i%2==0)
					work.addGear(ids.get(i), 2);
				else
					work.churn(ids.get(i));
			work.save(Set.copyOf(ids));
		}
	});

	static final NamedScenario SMALL_REPLACED = new NamedScenario(
			"GAPS: 2048 small cells, every other one destroyed for a large gear cell, twice", work -> {
		List<UUID> small = work.addCells(2048, 4, 4);
		work.save(Set.copyOf(small));
		for(int round = 0; round<2; round++)
		{
			Set<UUID> dirty = new HashSet<>();
			List<UUID> kept = new ArrayList<>();
			for(int i = 0; i<small.size(); i++)
				if(i%2==0)
				{
					work.destroy(small.get(i));
					dirty.add(small.get(i));
				}
				else
					kept.add(small.get(i));
			dirty.addAll(work.addGearCells(small.size()/2, 40));
			work.save(dirty);
			small = kept;
		}
	});

	static final List<NamedScenario> ALL = List.of(GAINING_TYPES, COUNTS_ONLY, NEW_CELLS, MIXED,
			INTERLEAVED_GROWTH, SMALL_REPLACED);

	private GrowthScenarios()
	{
	}

	record NamedScenario(String title, Scenario run)
	{
	}

	interface Scenario
	{
		void run(Workload work) throws IOException;
	}

	/** Every variant replays the same changes: the random source is reseeded per run. */
	static final class Workload
	{
		private static final Item[] ITEMS = BuiltInRegistries.ITEM.stream()
				.filter(item -> item!=Items.AIR&&!(item instanceof EraDataCell)).toArray(Item[]::new);
		private static final Item[] GEAR = BuiltInRegistries.ITEM.stream()
				.filter(item -> new ItemStack(item).isDamageableItem()).toArray(Item[]::new);

		CellSaveStrategy strategy;
		final Random random = new Random(7);
		final Map<UUID, CellContents> live = new LinkedHashMap<>();
		private final Map<UUID, Integer> firstItem = new HashMap<>();
		private final Map<UUID, Integer> position = new HashMap<>();
		private final TreeSet<Integer> freePositions = new TreeSet<>();
		long written, saveNanos, worstSaveNanos;
		int saves;

		/** Two cells per drive, 16 drives per network, 4 drives per chunk, as in {@link CellLayout#synthetic}. */
		String driveOf(UUID id)
		{
			return "drive_"+position.get(id)/2;
		}

		String networkOf(UUID id)
		{
			return "network_"+position.get(id)/2/16;
		}

		String chunkOf(UUID id)
		{
			return "chunk_"+position.get(id)/2/4;
		}

		void destroy(UUID id)
		{
			live.remove(id);
			freePositions.add(position.remove(id));
		}

		List<UUID> addCells(int count, int minTypes, int maxTypes)
		{
			List<UUID> added = new ArrayList<>();
			for(int i = 0; i<count; i++)
			{
				UUID id = newCell();
				firstItem.put(id, random.nextInt(ITEMS.length-MAX_TYPES));
				grow(id, minTypes+random.nextInt(maxTypes-minTypes+1));
				added.add(id);
			}
			return added;
		}

		/** Named, damaged gear: every stack is its own type and much bigger to save than a plain item. */
		List<UUID> addGearCells(int count, int types)
		{
			List<UUID> added = new ArrayList<>();
			for(int i = 0; i<count; i++)
			{
				UUID id = newCell();
				addGear(id, types);
				added.add(id);
			}
			return added;
		}

		/** A new cell takes the bay of the last destroyed one, like swapping cells in a drive. */
		private UUID newCell()
		{
			UUID id = new UUID(random.nextLong(), random.nextLong());
			live.put(id, CellContents.EMPTY);
			Integer free = freePositions.pollFirst();
			position.put(id, free==null? position.size(): free);
			return id;
		}

		void grow(UUID id, int types)
		{
			Map<ItemKey, Long> amounts = amounts(id);
			int first = firstItem.get(id);
			int target = Math.min(MAX_TYPES, amounts.size()+types);
			for(int i = amounts.size(); i<target; i++)
				amounts.put(ItemKey.of(new ItemStack(ITEMS[first+i])), 1L+random.nextInt(100_000));
			live.put(id, CellContents.of(amounts));
		}

		void addGear(UUID id, int types)
		{
			Map<ItemKey, Long> amounts = amounts(id);
			int target = Math.min(MAX_TYPES, amounts.size()+types);
			while(amounts.size()<target)
			{
				ItemStack gear = new ItemStack(GEAR[random.nextInt(GEAR.length)]);
				gear.set(DataComponents.DAMAGE, random.nextInt(gear.getMaxDamage()));
				gear.set(DataComponents.CUSTOM_NAME, Component.literal(randomName()));
				amounts.put(ItemKey.of(gear), 1L);
			}
			live.put(id, CellContents.of(amounts));
		}

		private String randomName()
		{
			StringBuilder name = new StringBuilder();
			for(int i = 0; i<10; i++)
				name.append((char)('a'+random.nextInt(26)));
			return name.toString();
		}

		void churn(UUID id)
		{
			Map<ItemKey, Long> amounts = amounts(id);
			amounts.replaceAll((key, count) -> 1L+random.nextInt(100_000));
			live.put(id, CellContents.of(amounts));
		}

		private Map<ItemKey, Long> amounts(UUID id)
		{
			Map<ItemKey, Long> amounts = new LinkedHashMap<>();
			live.get(id).entries().forEach(entry -> amounts.put(entry.key(), entry.count()));
			return amounts;
		}

		void save(Set<UUID> dirty) throws IOException
		{
			long start = System.nanoTime();
			written += strategy.save(live, dirty).bytesWritten();
			long nanos = System.nanoTime()-start;
			saveNanos += nanos;
			worstSaveNanos = Math.max(worstSaveNanos, nanos);
			saves++;
		}
	}
}
