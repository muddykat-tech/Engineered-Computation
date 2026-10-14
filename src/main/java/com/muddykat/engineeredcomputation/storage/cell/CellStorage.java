package com.muddykat.engineeredcomputation.storage.cell;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveMode;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveStrategy;
import com.muddykat.engineeredcomputation.storage.cell.persist.IndexedCellFile;

/** Cell contents for one world, held in memory and written to {@code data/<modid>/} with the world's saves. */
public final class CellStorage
{
	private static final String SAVE_HOOK = EngineeredComputation.MODID+"_cell_storage";
	@Nullable
	private static CellStorage current;
	@Nullable
	private static MinecraftServer currentServer;

	private final Map<UUID, Cell> cells = new HashMap<>();
	private final Set<UUID> dirty = new HashSet<>();
	private final Path dir;
	private final CellSaveMode mode;
	private int sectorSize;
	private final CellRecords records;
	private CellSaveStrategy file;
	private CellUuidList uuids;

	private CellStorage(Path dir, CellSaveMode mode, int sectorSize, CellRecords records)
	{
		this.dir = dir;
		this.mode = mode;
		this.sectorSize = sectorSize;
		this.records = records;
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> get(event.getServer()));
		NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> closeCurrent());
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
				event -> {
					CellStorageCommand.register(event.getDispatcher());
					CellBackupCommand.register(event.getDispatcher());
				});
		CellSweep.register();
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
		if(current==null||currentServer!=server)
		{
			closeCurrent();
			Path dir = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(EngineeredComputation.MODID);
			try
			{
				current = open(dir, ECConfig.cellSaveMode(), ECConfig.sectorSize(), server.registryAccess());
			}
			catch(IOException e)
			{
				// Starting empty would overwrite the saved cells at the next save, so this has to stop the world.
				throw new UncheckedIOException("Could not load cell storage from "+dir, e);
			}
			currentServer = server;
			server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(() -> new SaveHook(server),
					(tag, registries) -> new SaveHook(server)), SAVE_HOOK);
		}
		return current;
	}

	/**
	 * Opens the cell storage in {@code dir}, first moving it over from the other save mode's file if that's the one on
	 * disk. A new file is written in full before the old one is deleted, so when both exist the configured one is
	 * complete and the other is left over from an interrupted switch.
	 */
	public static CellStorage open(Path dir, CellSaveMode mode, int sectorSize, HolderLookup.Provider registries)
			throws IOException
	{
		Files.createDirectories(dir);
		CellRecords records = new CellRecords(registries);
		Path wanted = dir.resolve(mode.fileName());
		for(CellSaveMode other : CellSaveMode.values())
		{
			Path old = dir.resolve(other.fileName());
			if(other==mode||!Files.exists(old))
				continue;
			if(!Files.exists(wanted))
			{
				Map<UUID, CellContents> moved = other.read(old, records);
				Path temp = wanted.resolveSibling(wanted.getFileName()+".migrating");
				Files.deleteIfExists(temp);
				try(CellSaveStrategy target = mode.open(temp, sectorSize, records))
				{
					target.save(moved, moved.keySet());
				}
				Files.move(temp, wanted, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				EngineeredComputation.LOGGER.info("Moved {} cells from {} to {} cell storage", moved.size(), other, mode);
			}
			Files.delete(old);
		}
		CellStorage storage = new CellStorage(dir, mode, sectorSize, records);
		storage.file = mode.open(wanted, sectorSize, records);
		storage.file.load().forEach(storage::put);
		storage.uuids = CellUuidList.load(dir.resolve(CellUuidList.FILE));
		storage.uuids.reconcile(storage.cells.keySet());
		return storage;
	}

	private static void closeCurrent()
	{
		if(current==null)
			return;
		current.saveOrLog();
		try
		{
			current.close();
		}
		catch(IOException e)
		{
			EngineeredComputation.LOGGER.error("Could not close cell storage", e);
		}
		current = null;
		currentServer = null;
	}

	public CellSaveMode mode()
	{
		return mode;
	}

	public Path file()
	{
		return dir.resolve(mode.fileName());
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

	public void setContents(UUID id, CellContents contents)
	{
		setContents(id, contents, null);
	}

	public void setContents(UUID id, CellContents contents, @Nullable Item cellItem)
	{
		cells.remove(id);
		put(id, contents);
		dirty.add(id);
		track(id, cellItem);
	}

	/** Writes every cell changed since the last save, then the id list. */
	public void save() throws IOException
	{
		if(!dirty.isEmpty())
		{
			Set<UUID> saving = Set.copyOf(dirty);
			file.save(Maps.transformValues(cells, Cell::snapshot), saving);
			dirty.removeAll(saving);
		}
		if(uuids.isDirty())
			uuids.save();
	}

	private boolean needsSaving()
	{
		return !dirty.isEmpty()||uuids.isDirty();
	}

	public Path directory()
	{
		return dir;
	}

	/** Cells given their id at or before {@code time}, in epoch milliseconds. */
	public Set<UUID> idsAssignedBy(long time)
	{
		return uuids.assignedBy(time);
	}

	public Set<UUID> ids()
	{
		return Set.copyOf(cells.keySet());
	}

	/** Writes the cells' contents to {@code backup} in the bulk format, then removes them from storage. */
	public void removeWithBackup(Set<UUID> ids, Path backup) throws IOException
	{
		List<CellBackup.Entry> removed = new ArrayList<>();
		for(UUID id : ids)
		{
			ResourceLocation cell = uuids.cellOf(id);
			removed.add(new CellBackup.Entry(id, contents(id),
					cell==null? null: BuiltInRegistries.ITEM.getOptional(cell).orElse(null)));
		}
		CellBackup.write(backup, removed, records);
		ids.forEach(id -> setContents(id, CellContents.EMPTY));
		save();
	}

	CellRecords records()
	{
		return records;
	}

	private void saveOrLog()
	{
		try
		{
			save();
		}
		catch(IOException e)
		{
			EngineeredComputation.LOGGER.error("Could not save cell storage, will retry at the next save", e);
		}
	}

	/** What's on disk, read fresh: what the world would load next time. */
	public Map<UUID, CellContents> readBack() throws IOException
	{
		return mode.read(file(), records);
	}

	/** Rewrites the indexed file without gaps, at {@code sectorSize} from now on. Null in bulk mode. */
	@Nullable
	public Compaction compact(int sectorSize) throws IOException
	{
		if(mode!=CellSaveMode.INDEXED)
			return null;
		save();
		long before = Files.size(file());
		int sectorBefore = ((IndexedCellFile)file).sectorSize();
		file.close();
		IndexedCellFile.rewrite(file(), sectorSize, records);
		this.sectorSize = sectorSize;
		file = IndexedCellFile.open(file(), sectorSize, records);
		return new Compaction(before, Files.size(file()), sectorBefore, sectorSize);
	}

	public void close() throws IOException
	{
		file.close();
	}

	@Nullable
	Cell find(UUID id)
	{
		return cells.get(id);
	}

	void set(UUID id, ItemKey key, long amount, Item cellItem)
	{
		Cell cell = cells.computeIfAbsent(id, i -> new Cell());
		cell.set(key, amount);
		if(cell.types()==0)
			cells.remove(id);
		dirty.add(id);
		track(id, cellItem);
	}

	private void track(UUID id, @Nullable Item cellItem)
	{
		if(cells.containsKey(id))
			uuids.add(id, cellItem);
		else
			uuids.remove(id);
	}

	private void put(UUID id, CellContents contents)
	{
		Cell cell = new Cell();
		contents.entries().forEach(entry -> cell.set(entry.key(), entry.count()));
		if(cell.types()>0)
			cells.put(id, cell);
	}

	public record Compaction(long bytesBefore, long bytesAfter, int sectorBefore, int sectorAfter)
	{
	}

	/**
	 * Saves the cells in the overworld's SavedData pass, which runs before any chunk is written, so a chunk on disk never
	 * holds a cell id whose contents aren't on disk yet. It writes no file of its own.
	 */
	private static final class SaveHook extends SavedData
	{
		private final MinecraftServer server;

		SaveHook(MinecraftServer server)
		{
			this.server = server;
		}

		@Override
		public boolean isDirty()
		{
			return currentServer==server&&current!=null&&current.needsSaving();
		}

		@Override
		public void save(File file, HolderLookup.Provider registries)
		{
			if(isDirty())
				current.saveOrLog();
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
		{
			return tag;
		}
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
