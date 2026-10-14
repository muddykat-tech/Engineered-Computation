package com.muddykat.engineeredcomputation.storage.cell.persist;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

/**
 * One file laid out like Minecraft's region files: a header of fixed-size slots (cell id, first sector, sector count,
 * length) followed by sectors of gzip NBT, so a save only rewrites the changed cells' sectors and their slots.
 * <p>
 * Each record is written before its slot, so a crash mid-save leaves the slot pointing at the old, whole record.
 */
public final class IndexedCellFile implements CellSaveStrategy
{
	public static final int DEFAULT_SECTOR = 128;
	private static final int MAGIC = 0x45434346;
	private static final int VERSION = 1;
	private static final int FILE_HEADER = 32;
	private static final int SLOT = 32;
	private static final int COMPACT_ABOVE_PERCENT = 10;

	private final Path file;
	private final CellRecords records;
	private final FileChannel channel;
	private final Map<UUID, Slot> slots = new HashMap<>();
	private final BitSet usedSectors = new BitSet();
	private final BitSet usedSlots = new BitSet();
	private int sector;
	private int headerSectors;
	private long written;

	private IndexedCellFile(Path file, int sectorSize, CellRecords records, boolean readOnly) throws IOException
	{
		this.file = file;
		this.records = records;
		if(readOnly)
			channel = FileChannel.open(file, StandardOpenOption.READ);
		else
		{
			Files.createDirectories(file.getParent());
			channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
					StandardOpenOption.WRITE);
		}
		if(channel.size()==0)
		{
			sector = sectorSize;
			headerSectors = 1;
			usedSectors.set(0);
			write(ByteBuffer.allocate(sector), 0);
			writeFileHeader();
		}
		else
			readHeader();
	}

	/** Rewrites the file first if it's wasting space or was made with another sector size. */
	public static IndexedCellFile open(Path file, int sectorSize, CellRecords records) throws IOException
	{
		if(Files.exists(file)&&Files.size(file)>0)
		{
			boolean rewrite;
			try(IndexedCellFile old = new IndexedCellFile(file, sectorSize, records, true))
			{
				rewrite = old.sector!=sectorSize||old.reclaimableBytes()*100>old.channel.size()*COMPACT_ABOVE_PERCENT;
			}
			if(rewrite)
				rewrite(file, sectorSize, records);
		}
		return new IndexedCellFile(file, sectorSize, records, false);
	}

	public static Map<UUID, CellContents> read(Path file, CellRecords records) throws IOException
	{
		if(!Files.exists(file)||Files.size(file)==0)
			return new HashMap<>();
		try(IndexedCellFile reader = new IndexedCellFile(file, DEFAULT_SECTOR, records, true))
		{
			Map<UUID, CellContents> loaded = new HashMap<>();
			for(Map.Entry<UUID, Slot> entry : reader.slots.entrySet())
				loaded.put(entry.getKey(), records.decode(CellRecords.decompress(reader.read(entry.getValue()))));
			return loaded;
		}
	}

	/** Rewrites the file without gaps through a temporary file, so a crash keeps the old one. */
	public static void rewrite(Path file, int sectorSize, CellRecords records) throws IOException
	{
		Map<UUID, byte[]> data = new LinkedHashMap<>();
		try(IndexedCellFile old = new IndexedCellFile(file, sectorSize, records, true))
		{
			for(Map.Entry<UUID, Slot> entry : old.slots.entrySet())
				data.put(entry.getKey(), old.read(entry.getValue()));
		}
		Path temp = file.resolveSibling(file.getFileName()+".compacting");
		Files.deleteIfExists(temp);
		try(IndexedCellFile fresh = new IndexedCellFile(temp, sectorSize, records, false))
		{
			while(fresh.slotCapacity()<data.size())
				fresh.growHeader();
			for(Map.Entry<UUID, byte[]> entry : data.entrySet())
				fresh.write(entry.getKey(), entry.getValue());
			fresh.channel.force(true);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	@Override
	public String name()
	{
		return "indexed file";
	}

	public int sectorSize()
	{
		return sector;
	}

	/** Space that a rewrite would give back: gaps between records and sectors past each record's end. */
	public long reclaimableBytes() throws IOException
	{
		long needed = 0;
		for(Slot slot : slots.values())
			needed += sectorsFor(slot.length);
		long sectors = (channel.size()+sector-1)/sector-headerSectors;
		return Math.max(0, sectors-needed)*sector;
	}

	@Override
	public SaveResult save(Map<UUID, CellContents> live, Set<UUID> dirty) throws IOException
	{
		written = 0;
		for(UUID id : dirty)
		{
			CellContents contents = live.get(id);
			if(contents==null)
				delete(id);
			else
				write(id, CellRecords.compress(records.encode(contents)));
		}
		return new SaveResult(written>0? 1: 0, written);
	}

	@Override
	public Map<UUID, CellContents> load() throws IOException
	{
		return read(file, records);
	}

	@Override
	public DiskUsage usage() throws IOException
	{
		return new DiskUsage(1, channel.size());
	}

	@Override
	public void close() throws IOException
	{
		channel.close();
	}

	private int sectorsFor(int length)
	{
		return Math.max(1, (length+sector-1)/sector);
	}

	private void write(UUID id, byte[] data) throws IOException
	{
		int needed = sectorsFor(data.length);
		Slot slot = slots.get(id);
		if(slot==null)
		{
			slot = new Slot(claimSlot());
			slots.put(id, slot);
		}
		int oldFirst = slot.first, oldCount = slot.count;
		if(oldCount>=needed)
		{
			// Keeping one spare sector stops a record that hovers on a sector boundary from moving back and forth.
			int kept = oldCount<=needed+1? oldCount: needed;
			writeRecord(id, slot, data, kept);
			usedSectors.clear(oldFirst+kept, oldFirst+oldCount);
			return;
		}
		if(oldCount>0&&isFree(oldFirst+oldCount, needed-oldCount))
		{
			usedSectors.set(oldFirst+oldCount, oldFirst+needed);
			writeRecord(id, slot, data, needed);
			return;
		}
		slot.first = allocate(needed);
		writeRecord(id, slot, data, needed);
		if(oldCount>0)
			usedSectors.clear(oldFirst, oldFirst+oldCount);
	}

	private void writeRecord(UUID id, Slot slot, byte[] data, int count) throws IOException
	{
		write(ByteBuffer.wrap(data), (long)slot.first*sector);
		slot.count = count;
		slot.length = data.length;
		writeSlot(id, slot);
	}

	private void delete(UUID id) throws IOException
	{
		Slot slot = slots.remove(id);
		if(slot==null)
			return;
		write(ByteBuffer.allocate(SLOT), slotOffset(slot.index));
		usedSectors.clear(slot.first, slot.first+slot.count);
		usedSlots.clear(slot.index);
	}

	private int claimSlot() throws IOException
	{
		int index = usedSlots.nextClearBit(0);
		if(index>=slotCapacity())
			growHeader();
		usedSlots.set(index);
		return index;
	}

	/** Doubles the header, moving the records in its way to the end of the file. */
	private void growHeader() throws IOException
	{
		int grown = headerSectors*2;
		usedSectors.set(headerSectors, grown);
		for(Map.Entry<UUID, Slot> entry : List.copyOf(slots.entrySet()))
		{
			Slot slot = entry.getValue();
			if(slot.count==0||slot.first>=grown)
				continue;
			byte[] data = read(slot);
			int oldEnd = slot.first+slot.count;
			slot.first = allocate(slot.count);
			write(ByteBuffer.wrap(data), (long)slot.first*sector);
			writeSlot(entry.getKey(), slot);
			if(oldEnd>grown)
				usedSectors.clear(grown, oldEnd);
		}
		write(ByteBuffer.allocate((grown-headerSectors)*sector), (long)headerSectors*sector);
		headerSectors = grown;
		writeFileHeader();
	}

	private int allocate(int count)
	{
		int start = usedSectors.nextClearBit(headerSectors);
		while(true)
		{
			int end = usedSectors.nextSetBit(start);
			if(end<0||end-start>=count)
				break;
			start = usedSectors.nextClearBit(end);
		}
		usedSectors.set(start, start+count);
		return start;
	}

	private boolean isFree(int from, int count)
	{
		int next = usedSectors.nextSetBit(from);
		return next<0||next>=from+count;
	}

	private int slotCapacity()
	{
		return (headerSectors*sector-FILE_HEADER)/SLOT;
	}

	private static long slotOffset(int index)
	{
		return FILE_HEADER+(long)index*SLOT;
	}

	private void writeFileHeader() throws IOException
	{
		ByteBuffer header = ByteBuffer.allocate(FILE_HEADER).putInt(MAGIC).putInt(VERSION).putInt(headerSectors)
				.putInt(sector);
		write(header.rewind(), 0);
	}

	private void writeSlot(UUID id, Slot slot) throws IOException
	{
		ByteBuffer entry = ByteBuffer.allocate(SLOT).putLong(id.getMostSignificantBits())
				.putLong(id.getLeastSignificantBits()).putInt(slot.first).putInt(slot.count).putInt(slot.length);
		write(entry.rewind(), slotOffset(slot.index));
	}

	private void readHeader() throws IOException
	{
		ByteBuffer fileHeader = read(0, FILE_HEADER);
		if(fileHeader.getInt()!=MAGIC||fileHeader.getInt()!=VERSION)
			throw new IOException("Not a cell storage file: "+file);
		headerSectors = fileHeader.getInt();
		sector = fileHeader.getInt();
		usedSectors.set(0, headerSectors);
		ByteBuffer table = read(FILE_HEADER, slotCapacity()*SLOT);
		for(int index = 0; index<slotCapacity(); index++)
		{
			UUID id = new UUID(table.getLong(), table.getLong());
			Slot slot = new Slot(index);
			slot.first = table.getInt();
			slot.count = table.getInt();
			slot.length = table.getInt();
			table.getInt();
			if(slot.count==0)
				continue;
			slots.put(id, slot);
			usedSlots.set(index);
			usedSectors.set(slot.first, slot.first+slot.count);
		}
	}

	private byte[] read(Slot slot) throws IOException
	{
		return read((long)slot.first*sector, slot.length).array();
	}

	private ByteBuffer read(long position, int length) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(length);
		while(buffer.hasRemaining())
			if(channel.read(buffer, position+buffer.position())<0)
				throw new IOException("Cell storage file ends early: "+file);
		return buffer.flip();
	}

	private void write(ByteBuffer buffer, long position) throws IOException
	{
		int length = buffer.remaining();
		while(buffer.hasRemaining())
			channel.write(buffer, position+length-buffer.remaining());
		written += length;
	}

	private static final class Slot
	{
		final int index;
		int first, count, length;

		Slot(int index)
		{
			this.index = index;
		}
	}
}
