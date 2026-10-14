package com.muddykat.engineeredcomputation.storage.cell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Every cell id in storage, when it was given out and which cell item holds it. The time keeps a world sweep from
 * removing a cell made after it began; the item lets a backed up cell be restored as the right type.
 */
final class CellUuidList
{
	static final String FILE = "ec_uuid_list.dat";
	private static final String IDS = "ids";
	private static final String ID = "id";
	private static final String ASSIGNED = "assigned";
	private static final String CELL = "cell";

	private final Path file;
	private final Map<UUID, Entry> entries = new HashMap<>();
	private boolean dirty;

	private CellUuidList(Path file)
	{
		this.file = file;
	}

	static CellUuidList load(Path file) throws IOException
	{
		CellUuidList list = new CellUuidList(file);
		if(Files.exists(file))
			for(Tag entry : NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getList(IDS, Tag.TAG_COMPOUND))
			{
				CompoundTag tag = (CompoundTag)entry;
				list.entries.put(UUIDUtil.uuidFromIntArray(tag.getIntArray(ID)), new Entry(tag.getLong(ASSIGNED),
						tag.contains(CELL)? ResourceLocation.tryParse(tag.getString(CELL)): null));
			}
		return list;
	}

	/** Ids missing from the list, from cells saved before it existed, count as assigned at time zero. */
	void reconcile(Set<UUID> ids)
	{
		dirty |= entries.keySet().retainAll(ids);
		for(UUID id : ids)
			if(entries.putIfAbsent(id, new Entry(0, null))==null)
				dirty = true;
	}

	/** Also fills in the cell item for ids listed before it was known. */
	void add(UUID id, @Nullable Item cell)
	{
		ResourceLocation item = cell==null? null: BuiltInRegistries.ITEM.getKey(cell);
		Entry entry = entries.get(id);
		if(entry==null)
			entries.put(id, new Entry(System.currentTimeMillis(), item));
		else if(entry.cell==null&&item!=null)
			entries.put(id, new Entry(entry.assigned, item));
		else
			return;
		dirty = true;
	}

	void remove(UUID id)
	{
		if(entries.remove(id)!=null)
			dirty = true;
	}

	Set<UUID> assignedBy(long time)
	{
		Set<UUID> ids = new HashSet<>();
		entries.forEach((id, entry) -> {
			if(entry.assigned<=time)
				ids.add(id);
		});
		return ids;
	}

	@Nullable
	ResourceLocation cellOf(UUID id)
	{
		Entry entry = entries.get(id);
		return entry==null? null: entry.cell;
	}

	int size()
	{
		return entries.size();
	}

	boolean isDirty()
	{
		return dirty;
	}

	void save() throws IOException
	{
		ListTag list = new ListTag();
		entries.forEach((id, entry) -> {
			CompoundTag tag = new CompoundTag();
			tag.put(ID, new IntArrayTag(UUIDUtil.uuidToIntArray(id)));
			tag.putLong(ASSIGNED, entry.assigned);
			if(entry.cell!=null)
				tag.putString(CELL, entry.cell.toString());
			list.add(tag);
		});
		CompoundTag root = new CompoundTag();
		root.put(IDS, list);
		Path temp = file.resolveSibling(file.getFileName()+".tmp");
		NbtIo.writeCompressed(root, temp);
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		dirty = false;
	}

	private record Entry(long assigned, @Nullable ResourceLocation cell)
	{
	}
}
