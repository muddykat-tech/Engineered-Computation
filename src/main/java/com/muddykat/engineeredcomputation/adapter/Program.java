package com.muddykat.engineeredcomputation.adapter;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;


public record Program(List<ProgramEntry> entries)
{
	public static final int SIZE = 9;
	public static final Program EMPTY = new Program(List.of());

	public static final Codec<Program> CODEC = ExtraCodecs.validate(ProgramEntry.CODEC.listOf(),
			entries -> entries.size()<=SIZE? DataResult.success(entries)
					: DataResult.error(() -> "A program has at most "+SIZE+" entries, not "+entries.size()))
			.xmap(Program::new, Program::entries);

	public Program
	{
		List<ProgramEntry> padded = new ArrayList<>(entries.subList(0, Math.min(SIZE, entries.size())));
		while(padded.size()<SIZE)
			padded.add(ProgramEntry.EMPTY);
		entries = List.copyOf(padded);
	}

	public static Program read(FriendlyByteBuf buf)
	{
		return new Program(buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, SIZE), ProgramEntry::read));
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeCollection(entries, (out, entry) -> entry.write(out));
	}

	public ProgramEntry get(int index)
	{
		return entries.get(index);
	}

	public Program with(int index, ProgramEntry entry)
	{
		List<ProgramEntry> changed = new ArrayList<>(entries);
		changed.set(index, entry);
		return new Program(changed);
	}

	public boolean isEmpty()
	{
		return entries.stream().allMatch(ProgramEntry::isEmpty);
	}

	public boolean matches(ItemStack stack)
	{
		return entries.stream().anyMatch(entry -> entry.matches(stack));
	}
}
