package com.muddykat.engineeredcomputation.storage.cell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.storage.cell.persist.CellRecords;

/** The cells one sweep removed, in the bulk file format plus each cell's item, so they can be restored. */
public final class CellBackup
{
	static final String PREFIX = "removed-";
	private static final String SUFFIX = ".dat";
	private static final String CELLS = "cells";
	private static final String ID = "id";
	private static final String CELL = "cell";

	private CellBackup()
	{
	}

	/** Newest first. */
	public static List<String> names(CellStorage storage) throws IOException
	{
		try(Stream<Path> files = Files.list(storage.directory()))
		{
			return files.map(file -> file.getFileName().toString())
					.filter(name -> name.startsWith(PREFIX)&&name.endsWith(SUFFIX))
					.map(name -> name.substring(0, name.length()-SUFFIX.length()))
					.sorted(Comparator.reverseOrder())
					.toList();
		}
	}

	/** Null for a name that isn't a backup, including any that tries to reach outside the storage folder. */
	@Nullable
	public static Path path(CellStorage storage, String name)
	{
		if(!name.startsWith(PREFIX)||!name.matches("[A-Za-z0-9_.-]+"))
			return null;
		Path file = storage.directory().resolve(name+SUFFIX);
		return Files.isRegularFile(file)? file: null;
	}

	static String nameFor(String time)
	{
		return PREFIX+time;
	}

	static Path fileFor(CellStorage storage, String name)
	{
		return storage.directory().resolve(name+SUFFIX);
	}

	public static List<Entry> read(Path file, CellRecords records) throws IOException
	{
		List<Entry> entries = new ArrayList<>();
		for(Tag tag : NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getList(CELLS, Tag.TAG_COMPOUND))
		{
			CompoundTag cell = (CompoundTag)tag;
			Item item = cell.contains(CELL)? BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(
					cell.getString(CELL))).orElse(null): null;
			entries.add(new Entry(UUIDUtil.CODEC.parse(NbtOps.INSTANCE, cell.get(ID)).getOrThrow(),
					records.decode(cell), item));
		}
		return entries;
	}

	/** Deletes the file once nothing is left in it. */
	public static void write(Path file, List<Entry> entries, CellRecords records) throws IOException
	{
		if(entries.isEmpty())
		{
			Files.deleteIfExists(file);
			return;
		}
		ListTag list = new ListTag();
		for(Entry entry : entries)
		{
			CompoundTag cell = records.encode(entry.contents);
			cell.put(ID, UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, entry.id).getOrThrow());
			if(entry.cell!=null)
				cell.putString(CELL, BuiltInRegistries.ITEM.getKey(entry.cell).toString());
			list.add(cell);
		}
		CompoundTag root = new CompoundTag();
		root.put(CELLS, list);
		Path temp = file.resolveSibling(file.getFileName()+".tmp");
		NbtIo.writeCompressed(root, temp);
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public record Entry(UUID id, CellContents contents, @Nullable Item cell)
	{
	}
}
