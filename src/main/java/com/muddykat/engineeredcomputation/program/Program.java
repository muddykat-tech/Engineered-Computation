package com.muddykat.engineeredcomputation.program;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record Program(List<ProgramEntry> entries)
{
	public static final int SIZE = 9;
	public static final Program EMPTY = new Program(List.of());

	public static final Codec<Program> CODEC = ProgramEntry.CODEC.listOf(0, SIZE).xmap(Program::new, Program::entries);
	public static final StreamCodec<RegistryFriendlyByteBuf, Program> STREAM_CODEC =
			ProgramEntry.STREAM_CODEC.apply(ByteBufCodecs.list(SIZE)).map(Program::new, Program::entries);

	public Program
	{
		List<ProgramEntry> padded = new ArrayList<>(entries.subList(0, Math.min(SIZE, entries.size())));
		while(padded.size()<SIZE)
			padded.add(ProgramEntry.EMPTY);
		entries = List.copyOf(padded);
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
