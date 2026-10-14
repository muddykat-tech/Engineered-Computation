package com.muddykat.engineeredcomputation.storage.cell;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;

public final class CellRestore
{
	private static final String KEY = "commands."+EngineeredComputation.MODID+".cells.restore.";
	private static final DynamicCommandExceptionType NO_BACKUP =
			new DynamicCommandExceptionType(name -> Component.translatable(KEY+"no_backup", name));
	private static final DynamicCommandExceptionType NOT_IN_BACKUP =
			new DynamicCommandExceptionType(id -> Component.translatable(KEY+"missing", id));

	private CellRestore()
	{
	}

	/**
	 * Takes a cell out of a backup and returns it as a new item. It gets a fresh id, so if the original item turns up
	 * after all, the two don't share contents, and it leaves the backup so it can't be restored twice.
	 */
	public static Restored restore(CellStorage storage, String backup, UUID id) throws CommandSyntaxException,
			IOException
	{
		Path file = CellBackup.path(storage, backup);
		if(file==null)
			throw NO_BACKUP.create(backup);
		List<CellBackup.Entry> entries = new ArrayList<>(CellBackup.read(file, storage.records()));
		CellBackup.Entry entry = entries.stream().filter(e -> e.id().equals(id)).findFirst()
				.orElseThrow(() -> NOT_IN_BACKUP.create(id));
		boolean known = entry.cell() instanceof EraDataCell;
		EraDataCell type = known? (EraDataCell)entry.cell(): fittingCell(entry.contents());

		UUID fresh = UUID.randomUUID();
		storage.setContents(fresh, entry.contents(), type);
		// Saved before the backup loses the cell, so a crash in between leaves it stored rather than gone.
		storage.save();
		entries.remove(entry);
		CellBackup.write(file, entries, storage.records());

		ItemStack stack = new ItemStack(type);
		stack.set(ECDataComponents.CELL_UUID.get(), fresh);
		stack.set(ECDataComponents.CELL_SUMMARY.get(),
				new CellSummary(entry.contents().types(), entry.contents().totalCount()));
		return new Restored(stack, !known);
	}

	/** The smallest cell that holds the contents, for cells backed up before their type was recorded. */
	static EraDataCell fittingCell(CellContents contents)
	{
		List<EraDataCell> cells = new ArrayList<>();
		for(ComputerEra era : ComputerEra.values())
			ECDataCells.cellsOf(era).forEach(cell -> cells.add(cell.get()));
		long itemBytes = (contents.totalCount()+CellInventory.ITEMS_PER_BYTE-1)/CellInventory.ITEMS_PER_BYTE;
		return cells.stream()
				.filter(cell -> cell.getTotalTypes()>=contents.types()
						&&cell.getTotalBytes()>=(long)contents.types()*cell.getBytesPerType()+itemBytes)
				.min(Comparator.comparingInt(EraDataCell::getTotalBytes))
				.orElseGet(() -> cells.stream().max(Comparator.comparingInt(EraDataCell::getTotalTypes)
						.thenComparingInt(EraDataCell::getTotalBytes)).orElseThrow());
	}

	public record Restored(ItemStack stack, boolean guessedType)
	{
	}
}
