package com.muddykat.engineeredcomputation.storage.cell.persist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

/** Indexed is normally the best choice, but if you need to save space, bulk has a smaller save footprint. */
public enum CellSaveMode
{
	INDEXED("cells.ecc"),
	BULK("cells.dat");

	private final String fileName;

	CellSaveMode(String fileName)
	{
		this.fileName = fileName;
	}

	public String fileName()
	{
		return fileName;
	}

	public CellSaveStrategy open(Path file, int sectorSize, CellRecords records) throws IOException
	{
		return this==INDEXED? IndexedCellFile.open(file, sectorSize, records): new BulkCellFile(file, records);
	}

	public Map<UUID, CellContents> read(Path file, CellRecords records) throws IOException
	{
		return this==INDEXED? IndexedCellFile.read(file, records): BulkCellFile.read(file, records);
	}
}
