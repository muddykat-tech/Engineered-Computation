package com.muddykat.engineeredcomputation.storage.cell;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public final class CellStorage extends SavedData
{
	private static final String NAME = EngineeredComputation.MODID+"_cells";
	private static final String CELLS = "cells";
	private static final String ID = "id";
	private static final String ITEMS = "items";

	private final Map<UUID, Cell> cells = new HashMap<>();

	private CellStorage()
	{
	}

	/** Null on the client, which never holds UUID cell contents. */
	@Nullable
	public static CellStorage get()
	{
		if(Thread.currentThread().getThreadGroup()!=SidedThreadGroups.SERVER)
			return null;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server==null? null: get(server);
	}

	public static CellStorage get(MinecraftServer server)
	{
		return server.overworld().getDataStorage().computeIfAbsent(
				new SavedData.Factory<>(CellStorage::new, CellStorage::load), NAME);
	}

	public int size()
	{
		return cells.size();
	}

	public CellContents contents(UUID id)
	{
		Cell cell = cells.get(id);
		return cell==null? CellContents.EMPTY: cell.snapshot();
	}

	@Nullable
	Cell find(UUID id)
	{
		return cells.get(id);
	}

	void set(UUID id, ItemKey key, long amount)
	{
		Cell cell = cells.computeIfAbsent(id, i -> new Cell());
		cell.set(key, amount);
		if(cell.types()==0)
			cells.remove(id);
		setDirty();
	}

	public static CellStorage load(CompoundTag tag, HolderLookup.Provider registries)
	{
		CellStorage storage = new CellStorage();
		RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
		for(Tag entry : tag.getList(CELLS, Tag.TAG_COMPOUND))
		{
			CompoundTag cellTag = (CompoundTag)entry;
			UUID id = UUIDUtil.CODEC.parse(NbtOps.INSTANCE, cellTag.get(ID)).getOrThrow();
			CellContents.CODEC.parse(ops, cellTag.get(ITEMS))
					.resultOrPartial(error -> EngineeredComputation.LOGGER.error("Dropping bad contents of cell {}: {}", id, error))
					.ifPresent(contents -> {
						Cell cell = new Cell();
						contents.entries().forEach(e -> cell.set(e.key(), e.count()));
						if(cell.types()>0)
							storage.cells.put(id, cell);
					});
		}
		return storage;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
	{
		RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
		ListTag list = new ListTag();
		cells.forEach((id, cell) -> {
			CompoundTag cellTag = new CompoundTag();
			cellTag.put(ID, UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, id).getOrThrow());
			cellTag.put(ITEMS, CellContents.CODEC.encodeStart(ops, cell.snapshot()).getOrThrow());
			list.add(cellTag);
		});
		tag.put(CELLS, list);
		return tag;
	}

	static final class Cell
	{
		private final Map<ItemKey, Long> amounts = new LinkedHashMap<>();
		private long total;

		long amount(ItemKey key)
		{
			return amounts.getOrDefault(key, 0L);
		}

		int types()
		{
			return amounts.size();
		}

		long totalCount()
		{
			return total;
		}

		void collect(Map<ItemKey, Long> into)
		{
			amounts.forEach((key, count) -> into.merge(key, count, Long::sum));
		}

		void set(ItemKey key, long amount)
		{
			Long old = amount>0? amounts.put(key, amount): amounts.remove(key);
			total += amount-(old==null? 0: old);
		}

		CellContents snapshot()
		{
			return CellContents.of(amounts);
		}
	}
}
