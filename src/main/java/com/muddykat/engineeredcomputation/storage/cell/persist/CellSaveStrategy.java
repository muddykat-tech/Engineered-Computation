package com.muddykat.engineeredcomputation.storage.cell.persist;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

public interface CellSaveStrategy extends Closeable
{
	String name();

	/** Cells in {@code dirty} that are missing from {@code live} have been destroyed. */
	SaveResult save(Map<UUID, CellContents> live, Set<UUID> dirty) throws IOException;

	/** Reads everything back from disk, ignoring what this instance holds in memory. */
	Map<UUID, CellContents> load() throws IOException;

	DiskUsage usage() throws IOException;

	record SaveResult(int filesWritten, long bytesWritten)
	{
	}

	record DiskUsage(int files, long bytes)
	{
	}
}
