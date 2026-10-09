package com.muddykat.engineeredcomputation.terminal;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.network.FriendlyByteBuf;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

public record TerminalStats(long usedBytes, long totalBytes, long usedTypes, long totalTypes, int cells, int lockBits)
{
	private static final int TOO_LARGE = 1<<30;
	private static final int PROCESSING = 1<<29;
	private static final int NO_OUTLET = 1<<28;
	private static final int NO_PROCESSOR = 8;

	public static final TerminalStats EMPTY = new TerminalStats(0, 0, 0, 0, 0, 0);

	public static TerminalStats read(FriendlyByteBuf buf)
	{
		return new TerminalStats(buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(),
				buf.readVarInt(), buf.readVarInt());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeVarLong(usedBytes);
		buf.writeVarLong(totalBytes);
		buf.writeVarLong(usedTypes);
		buf.writeVarLong(totalTypes);
		buf.writeVarInt(cells);
		buf.writeVarInt(lockBits);
	}

	public static TerminalStats of(NetworkStorage storage, Set<ComputerEra> conflictingEras, boolean tooLarge)
	{
		return of(storage, conflictingEras, tooLarge, false, Set.of());
	}

	public static TerminalStats of(NetworkStorage storage, Set<ComputerEra> conflictingEras, boolean tooLarge,
			boolean processing, Set<ComputerEra> erasWithoutProcessor)
	{
		long usedBytes = 0;
		long totalBytes = 0;
		long usedTypes = 0;
		long totalTypes = 0;
		for(CellInventory cell : storage.getCells())
		{
			usedBytes += cell.getUsedBytes();
			totalBytes += cell.getTotalBytes();
			usedTypes += cell.getContents().types();
			totalTypes += cell.getTotalTypes();
		}
		int lockBits = (tooLarge? TOO_LARGE: 0)|(processing? PROCESSING: 0);
		for(ComputerEra era : conflictingEras)
			lockBits |= 1<<era.ordinal();
		for(ComputerEra era : erasWithoutProcessor)
			lockBits |= 1<<NO_PROCESSOR+era.ordinal();
		int cells = storage.getCells().size();
		return new TerminalStats(usedBytes, totalBytes, usedTypes, totalTypes, cells, lockBits);
	}

	public static TerminalStats ofCatalogue(int cards, int capacity, int kinds, int catalogues, int reachable,
			boolean tooLarge, boolean noOutlet, boolean carriersComing)
	{
		int lockBits = (tooLarge? TOO_LARGE: 0)|(noOutlet? NO_OUTLET: 0)|(carriersComing? PROCESSING: 0);
		return new TerminalStats(cards, capacity, kinds, reachable, catalogues, lockBits);
	}

	public boolean noOutlet()
	{
		return (lockBits&NO_OUTLET)!=0;
	}

	public double byteFraction()
	{
		return totalBytes==0? 0: (double)usedBytes/totalBytes;
	}

	public boolean processing()
	{
		return (lockBits&PROCESSING)!=0;
	}

	public boolean tooLarge()
	{
		return (lockBits&TOO_LARGE)!=0;
	}

	public Set<ComputerEra> conflictingEras()
	{
		return eras(0);
	}

	public Set<ComputerEra> erasWithoutProcessor()
	{
		return eras(NO_PROCESSOR);
	}

	private Set<ComputerEra> eras(int firstBit)
	{
		Set<ComputerEra> eras = EnumSet.noneOf(ComputerEra.class);
		for(ComputerEra era : ComputerEra.values())
			if((lockBits&1<<firstBit+era.ordinal())!=0)
				eras.add(era);
		return eras;
	}
}
