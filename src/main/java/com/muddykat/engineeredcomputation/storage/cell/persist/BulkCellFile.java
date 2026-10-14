package com.muddykat.engineeredcomputation.storage.cell.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

/** Every cell in one gzip NBT file, rewritten whole whenever any cell changed. */
public final class BulkCellFile implements CellSaveStrategy
{
	private final Path file;
	private final CellRecords records;

	public BulkCellFile(Path file, CellRecords records)
	{
		this.file = file;
		this.records = records;
	}

	public static Map<UUID, CellContents> read(Path file, CellRecords records) throws IOException
	{
		Map<UUID, CellContents> loaded = new HashMap<>();
		if(Files.exists(file))
			records.decodeAll(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), loaded);
		return loaded;
	}

	@Override
	public String name()
	{
		return "bulk file";
	}

	@Override
	public SaveResult save(Map<UUID, CellContents> live, Set<UUID> dirty) throws IOException
	{
		if(dirty.isEmpty())
			return new SaveResult(0, 0);
		Files.createDirectories(file.getParent());
		Path temp = file.resolveSibling(file.getFileName()+".tmp");
		NbtIo.writeCompressed(records.encodeAll(live), temp);
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		return new SaveResult(1, Files.size(file));
	}

	@Override
	public Map<UUID, CellContents> load() throws IOException
	{
		return read(file, records);
	}

	@Override
	public DiskUsage usage() throws IOException
	{
		return Files.exists(file)? new DiskUsage(1, Files.size(file)): new DiskUsage(0, 0);
	}

	@Override
	public void close()
	{
	}
}
