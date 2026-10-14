package com.muddykat.engineeredcomputation.storage.persist;

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

import net.minecraft.core.HolderLookup;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;
import com.muddykat.engineeredcomputation.storage.cell.persist.CellSaveStrategy;

/**
 * One file laid out like Minecraft's region files: a header of fixed-size slots (cell id, first sector, sector count,
 * length) followed by sectors of gzip NBT, so a save only rewrites the changed cells' sectors and their slots.
 * Each record is written before its slot, so a crash mid-save leaves the slot pointing at the old record. The
 * header grows by moving the records that it'd overwrite to the end of the file; freed sectors are reused but
 * the file will never shrink.
 */
public final class RegionFileStrategy implements CellSaveStrategy
{
	public static final int DEFAULT_SECTOR = 512;
	private static final int MAGIC = 0x45434346;
	private static final int VERSION = 2;
	private static final int FILE_HEADER = 32;
	private static final int SLOT = 32;

	private final Path file;
	private final Options options;
	private final CellRecords records;
	private final FileChannel channel;
	private final Map<UUID, Slot> slots = new HashMap<>();
	private final BitSet usedSectors = new BitSet();
	private final BitSet usedSlots = new BitSet();
	private int sector;
	private int headerSectors;
	private long written;
	private long moves;

	public RegionFileStrategy(Path file, HolderLookup.Provider registries) throws IOException
	{
		this(file, Options.BEST, new CellRecords(registries));
	}

	public RegionFileStrategy(Path file, Options options, HolderLookup.Provider registries) throws IOException
	{
		this(file, options, new CellRecords(registries));
	}

	private RegionFileStrategy(Path file, Options options, CellRecords records) throws IOException
	{
		Files.createDirectories(file.getParent());
		this.file = file;
		this.options = options;
		this.records = records;
		if(options.compactAbovePercent>0&&Files.exists(file)&&Files.size(file)>0)
			compactIfWasteful(file, options.compactAbovePercent, records);
		this.channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE);
		if(channel.size()==0)
		{
			sector = options.sectorSize;
			headerSectors = 1;
			usedSectors.set(0);
			write(ByteBuffer.allocate(sector), 0);
			writeFileHeader();
		}
		else
			readHeader();
	}

	@Override
	public String name()
	{
		return (options.equals(Options.NONE)? "indexed, original": "indexed, fixed")
				+(sector==DEFAULT_SECTOR? "": ", "+sector+" B");
	}

	public int headerSectors()
	{
		return headerSectors;
	}

	public Growth growth() throws IOException
	{
		long live = 0, allocated = 0;
		for(Slot slot : slots.values())
		{
			live += slot.length;
			allocated += (long)slot.count*sector;
		}
		long fileBytes = Math.max((channel.size()+sector-1)/sector, usedSectors.length())*sector;
		long header = (long)headerSectors*sector;
		return new Growth(fileBytes, header, live, allocated-live, fileBytes-header-allocated, moves);
	}

	/** Each switch is one fix for the file growing past its live data. */
	public record Options(boolean growInPlace, int headroomPercent, boolean headroomOnlyAfterGrowth,
			boolean keepOnShrink, int compactAbovePercent, int sectorSize)
	{
		public static final Options NONE = new Options(false, 0, false, false, 0, DEFAULT_SECTOR);
		public static final Options BEST = new Options(true, 0, false, true, 10, 128);

		/** Only used for a new file; an existing file keeps the sector size in its header. */
		public Options withSectorSize(int size)
		{
			return new Options(growInPlace, headroomPercent, headroomOnlyAfterGrowth, keepOnShrink, compactAbovePercent,
					size);
		}

		int headroom(int sectors)
		{
			return headroomPercent==0? 0: Math.max(1, (sectors*headroomPercent+99)/100);
		}
	}

	/** A {@code gap} inside the file is just space inside the file that isn't used and isn't owned "dead space" essentially;
	 * {@code padding} is assigned to a cell, it's just unused. */
	public record Growth(long fileBytes, long headerBytes, long liveBytes, long paddingBytes, long gapBytes, long moves)
	{
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
		try(RegionFileStrategy reader = new RegionFileStrategy(file, Options.NONE, records))
		{
			Map<UUID, CellContents> loaded = new HashMap<>();
			for(Map.Entry<UUID, Slot> entry : reader.slots.entrySet())
				loaded.put(entry.getKey(), records.decode(CellRecords.decompress(reader.read(entry.getValue()))));
			return loaded;
		}
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

	/** Rewrites the file without gaps or spare room, through a temporary file so a crash keeps the old one. */
	private static void compactIfWasteful(Path file, int percent, CellRecords records) throws IOException
	{
		Map<UUID, byte[]> data = new LinkedHashMap<>();
		int size;
		try(RegionFileStrategy old = new RegionFileStrategy(file, Options.NONE, records))
		{
			size = old.sector;
			long needed = 0;
			for(Slot slot : old.slots.values())
				needed += old.sectorsFor(slot.length);
			long sectors = (old.channel.size()+size-1)/size-old.headerSectors;
			if((sectors-needed)*100<=sectors*percent)
				return;
			for(Map.Entry<UUID, Slot> entry : old.slots.entrySet())
				data.put(entry.getKey(), old.read(entry.getValue()));
		}
		Path temp = file.resolveSibling(file.getFileName()+".compacting");
		Files.deleteIfExists(temp);
		try(RegionFileStrategy fresh = new RegionFileStrategy(temp, Options.NONE.withSectorSize(size), records))
		{
			while(fresh.slotCapacity()<data.size())
				fresh.growHeader();
			for(Map.Entry<UUID, byte[]> entry : data.entrySet())
				fresh.write(entry.getKey(), entry.getValue());
			fresh.channel.force(true);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
		int wanted = needed+(options.headroomOnlyAfterGrowth&&oldCount==0? 0: options.headroom(needed));
		if(oldCount>=needed)
		{
			int kept = options.keepOnShrink? oldCount<=wanted+1? oldCount: wanted: needed;
			write(ByteBuffer.wrap(data), (long)oldFirst*sector);
			slot.count = kept;
			slot.length = data.length;
			writeSlot(id, slot);
			usedSectors.clear(oldFirst+kept, oldFirst+oldCount);
			return;
		}
		if(options.growInPlace&&oldCount>0&&isFree(oldFirst+oldCount, needed-oldCount))
		{
			int grown = isFree(oldFirst+oldCount, wanted-oldCount)? wanted: needed;
			usedSectors.set(oldFirst+oldCount, oldFirst+grown);
			write(ByteBuffer.wrap(data), (long)oldFirst*sector);
			slot.count = grown;
			slot.length = data.length;
			writeSlot(id, slot);
			return;
		}
		if(oldCount>0)
			moves++;
		slot.first = allocate(wanted);
		slot.count = wanted;
		slot.length = data.length;
		write(ByteBuffer.wrap(data), (long)slot.first*sector);
		writeSlot(id, slot);
		if(oldCount>0)
			usedSectors.clear(oldFirst, oldFirst+oldCount);
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
			moves++;
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
			throw new IOException("Not a cell file: "+file);
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
				throw new IOException("Cell file ends early: "+file);
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
