package com.muddykat.engineeredcomputation.storage.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

/**
 * One gzip NBT file per group, rewritten whole when any cell in it changes, same way that SavedData per group does things.
 * A single group for every cell is the bulk file method.
 */
public final class GroupedFileStrategy implements CellSaveStrategy
{
	private final String name;
	private final Path dir;
	private final CellRecords records;
	private final Function<UUID, String> groupOf;
	private final Map<String, Set<UUID>> members = new HashMap<>();
	private final Map<UUID, String> savedIn = new HashMap<>();

	public GroupedFileStrategy(String name, Path dir, HolderLookup.Provider registries, Function<UUID, String> groupOf)
			throws IOException
	{
		this.name = name;
		this.dir = Files.createDirectories(dir);
		this.records = new CellRecords(registries);
		this.groupOf = groupOf;
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public SaveResult save(Map<UUID, CellContents> live, Set<UUID> dirty) throws IOException
	{
		Set<String> dirtyGroups = new LinkedHashSet<>();
		for(UUID id : dirty)
		{
			String old = savedIn.remove(id);
			if(old!=null)
			{
				members.get(old).remove(id);
				dirtyGroups.add(old);
			}
			if(live.containsKey(id))
			{
				String group = groupOf.apply(id);
				members.computeIfAbsent(group, g -> new LinkedHashSet<>()).add(id);
				savedIn.put(id, group);
				dirtyGroups.add(group);
			}
		}
		long bytes = 0;
		for(String group : dirtyGroups)
		{
			Path file = dir.resolve(group+".dat");
			Set<UUID> ids = members.get(group);
			if(ids==null||ids.isEmpty())
			{
				members.remove(group);
				Files.deleteIfExists(file);
				continue;
			}
			Map<UUID, CellContents> cells = new LinkedHashMap<>();
			ids.forEach(id -> cells.put(id, live.get(id)));
			NbtIo.writeCompressed(records.encodeAll(cells), file);
			bytes += Files.size(file);
		}
		return new SaveResult(dirtyGroups.size(), bytes);
	}

	@Override
	public Map<UUID, CellContents> load() throws IOException
	{
		Map<UUID, CellContents> loaded = new HashMap<>();
		for(Path file : files())
			records.decodeAll(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), loaded);
		return loaded;
	}

	@Override
	public DiskUsage usage() throws IOException
	{
		long bytes = 0;
		List<Path> files = files();
		for(Path file : files)
			bytes += Files.size(file);
		return new DiskUsage(files.size(), bytes);
	}

	private List<Path> files() throws IOException
	{
		try(Stream<Path> stream = Files.list(dir))
		{
			return stream.filter(file -> file.toString().endsWith(".dat")).toList();
		}
	}

	@Override
	public void close()
	{
	}
}
