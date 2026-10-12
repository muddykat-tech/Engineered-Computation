package com.muddykat.engineeredcomputation.terminal;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.network.FriendlyByteBuf;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.NetworkStorage;

/**
 * What a terminal shows about its network: storage use, why it might be locked, and (while power is enabled) its cost
 * in RF per tick and the RF its Energy Accepters hold.
 */
public record TerminalStats(long usedBytes, long totalBytes, long usedTypes, long totalTypes, int cells, int lockBits,
		long powerCost, long powerStored, long powerCapacity)
{
	private static final int TOO_LARGE = 1<<30;
	private static final int PROCESSING = 1<<29;
	private static final int NO_OUTLET = 1<<28;
	private static final int POWER_ON = 1<<27;
	private static final int UNPOWERED = 1<<26;
	private static final int NO_PROCESSOR = 8;

	public static final TerminalStats EMPTY = new TerminalStats(0, 0, 0, 0, 0, 0, 0, 0, 0);

	public static TerminalStats read(FriendlyByteBuf buf)
	{
		return new TerminalStats(buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(),
				buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeVarLong(usedBytes);
		buf.writeVarLong(totalBytes);
		buf.writeVarLong(usedTypes);
		buf.writeVarLong(totalTypes);
		buf.writeVarInt(cells);
		buf.writeVarInt(lockBits);
		buf.writeVarLong(powerCost);
		buf.writeVarLong(powerStored);
		buf.writeVarLong(powerCapacity);
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
		return new TerminalStats(usedBytes, totalBytes, usedTypes, totalTypes, cells, lockBits, 0, 0, 0);
	}

	/**
	 * These stats with the network's power: whether power is enabled and paid, its cost, and its accepters' RF (all 0
	 * while power is disabled).
	 */
	public TerminalStats withPower(boolean enabled, boolean powered, long cost, long stored, long capacity)
	{
		if(!enabled)
			return new TerminalStats(usedBytes, totalBytes, usedTypes, totalTypes, cells,
					lockBits&~(POWER_ON|UNPOWERED), 0, 0, 0);
		int bits = lockBits&~UNPOWERED|POWER_ON|(powered? 0: UNPOWERED);
		return new TerminalStats(usedBytes, totalBytes, usedTypes, totalTypes, cells, bits, cost, stored, capacity);
	}

	public boolean powerEnabled()
	{
		return (lockBits&POWER_ON)!=0;
	}

	/** Whether power is enabled and the network can't pay for its machines (402). */
	public boolean unpowered()
	{
		return (lockBits&UNPOWERED)!=0;
	}

	public static TerminalStats ofCatalogue(int cards, int capacity, int kinds, int catalogues, int reachable,
			boolean tooLarge, boolean noOutlet, boolean carriersComing)
	{
		int lockBits = (tooLarge? TOO_LARGE: 0)|(noOutlet? NO_OUTLET: 0)|(carriersComing? PROCESSING: 0);
		return new TerminalStats(cards, capacity, kinds, reachable, catalogues, lockBits, 0, 0, 0);
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
