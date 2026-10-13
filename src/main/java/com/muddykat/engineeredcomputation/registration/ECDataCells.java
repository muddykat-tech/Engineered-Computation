package com.muddykat.engineeredcomputation.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.storage.cell.UuidDataCell;

public final class ECDataCells
{
	private static final Map<ComputerEra, List<DeferredItem<EraDataCell>>> CELLS = new EnumMap<>(ComputerEra.class);
	private static final List<DeferredItem<EraDataCell>> TERMINAL_DISKS = new ArrayList<>();
	private static final Map<ComputerEra, DeferredItem<UuidDataCell>> UUID_CELLS = new EnumMap<>(ComputerEra.class);

	static
	{
		// Essentially, I wanted to buff the size of drums... but the ID is tied to it, so we have this "fake" id to
		// keep legacy items still in place.
		tier(ComputerEra.DRUM, "256b", 512, 8, 8);
		tier(ComputerEra.DRUM, "512b", 1024, 8, 12);
		tier(ComputerEra.DRUM, "1k", 2048, 8, 16);
		tier(ComputerEra.DRUM, "2k", 4096, 8, 24);

		tier(ComputerEra.TAPE, 512*1024, 8, 2);
		tier(ComputerEra.TAPE, 1024*1024, 8, 2);
		tier(ComputerEra.TAPE, 2*1024*1024, 8, 2);
		tier(ComputerEra.TAPE, 4*1024*1024, 8, 2);

		tier(ComputerEra.DISK, 8*1024, 32, 32);
		tier(ComputerEra.DISK, 16*1024, 32, 96);
		tier(ComputerEra.DISK, 24*1024, 32, 128);
		tier(ComputerEra.DISK, 32*1024, 32, 256);

		tier(ComputerEra.FLOPPY, 16384, 128, 32);
		tier(ComputerEra.FLOPPY, 32768, 256, 40);
		tier(ComputerEra.FLOPPY, 65536, 512, 50);
		tier(ComputerEra.FLOPPY, 131072, 1024, 63);
		tier(ComputerEra.FLOPPY, 524288, 4096, 63);
	}

	static
	{
		terminalDisk(1024, 8, 4);
		terminalDisk(2048, 16, 6);
		terminalDisk(4096, 32, 8);
		terminalDisk(8192, 64, 10);
	}

	static
	{
		uuidCell(ComputerEra.DRUM, 4096, 8, 24);
		uuidCell(ComputerEra.TAPE, 4*1024*1024, 8, 2);
		uuidCell(ComputerEra.DISK, 32*1024, 32, 256);
		uuidCell(ComputerEra.FLOPPY, 524288, 4096, 63);
	}


	private ECDataCells()
	{
	}

	public static void init()
	{
	}

	public static List<DeferredItem<EraDataCell>> cellsOf(ComputerEra era)
	{
		return Collections.unmodifiableList(CELLS.getOrDefault(era, List.of()));
	}

	public static List<DeferredItem<EraDataCell>> terminalDisks()
	{
		return Collections.unmodifiableList(TERMINAL_DISKS);
	}

	/** List of the Test cells separated so we don't have the guide pick them up. */
	public static Map<ComputerEra, DeferredItem<UuidDataCell>> uuidCells()
	{
		return Collections.unmodifiableMap(UUID_CELLS);
	}

	private static void uuidCell(ComputerEra era, int totalBytes, int bytesPerType, int totalTypes)
	{
		UUID_CELLS.put(era, ECRegistrationHolder.ITEMS.register("uuid_cell_"+era.getSerializedName(),
				() -> new UuidDataCell(new Properties(), era, totalBytes, bytesPerType, totalTypes)));
	}

	private static void terminalDisk(int totalBytes, int bytesPerType, int totalTypes)
	{
		TERMINAL_DISKS.add(ECRegistrationHolder.ITEMS.register("floppy_35_cell_"+(totalBytes/1024)+"k",
				() -> new EraDataCell(new Properties(), ComputerEra.FLOPPY, totalBytes, bytesPerType, totalTypes, true)));
	}

	private static void tier(ComputerEra era, int totalBytes, int bytesPerType, int totalTypes)
	{
		tier(era, totalBytes<1024? totalBytes+"b": totalBytes<1024*1024? (totalBytes/1024)+"k"
				: (totalBytes/(1024*1024))+"m", totalBytes, bytesPerType, totalTypes);
	}

	private static void tier(ComputerEra era, String size, int totalBytes, int bytesPerType, int totalTypes)
	{
		DeferredItem<EraDataCell> item = ECRegistrationHolder.ITEMS.register(
				era.memoryName()+"_cell_"+size,
				() -> new EraDataCell(new Properties(), era, totalBytes, bytesPerType, totalTypes)
		);
		CELLS.computeIfAbsent(era, e -> new ArrayList<>()).add(item);
	}
}
