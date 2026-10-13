package com.muddykat.engineeredcomputation.storage.persist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

import com.muddykat.engineeredcomputation.storage.cell.CellContents;

final class CellRecords
{
	private static final String CELLS = "cells";
	private static final String ID = "id";
	private static final String ITEMS = "items";

	private final RegistryOps<Tag> ops;

	CellRecords(HolderLookup.Provider registries)
	{
		this.ops = registries.createSerializationContext(NbtOps.INSTANCE);
	}

	CompoundTag encodeAll(Map<UUID, CellContents> cells)
	{
		ListTag list = new ListTag();
		cells.forEach((id, contents) -> {
			CompoundTag cell = encode(contents);
			cell.put(ID, UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, id).getOrThrow());
			list.add(cell);
		});
		CompoundTag tag = new CompoundTag();
		tag.put(CELLS, list);
		return tag;
	}

	void decodeAll(CompoundTag tag, Map<UUID, CellContents> into)
	{
		for(Tag entry : tag.getList(CELLS, Tag.TAG_COMPOUND))
		{
			CompoundTag cell = (CompoundTag)entry;
			into.put(UUIDUtil.CODEC.parse(NbtOps.INSTANCE, cell.get(ID)).getOrThrow(), decode(cell));
		}
	}

	CompoundTag encode(CellContents contents)
	{
		CompoundTag tag = new CompoundTag();
		tag.put(ITEMS, CellContents.CODEC.encodeStart(ops, contents).getOrThrow());
		return tag;
	}

	CellContents decode(CompoundTag tag)
	{
		return CellContents.CODEC.parse(ops, tag.get(ITEMS)).getOrThrow();
	}

	static byte[] compress(CompoundTag tag) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		NbtIo.writeCompressed(tag, out);
		return out.toByteArray();
	}

	static CompoundTag decompress(byte[] data) throws IOException
	{
		return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
	}
}
