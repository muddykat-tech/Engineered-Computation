package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.muddykat.engineeredcomputation.storage.ItemKey;

public final class RequestSlip
{
	public static final int SIZE = 9;

	public record Line(ItemKey key, int amount)
	{
		public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemKey.CODEC.fieldOf("item").forGetter(Line::key),
				Codec.INT.fieldOf("amount").forGetter(Line::amount)
		).apply(instance, Line::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
				ItemKey.STREAM_CODEC, Line::key,
				ByteBufCodecs.VAR_INT, Line::amount,
				Line::new);
	}

	public static final Codec<List<Line>> CODEC = Line.CODEC.listOf();

	private final List<Line> lines = new ArrayList<>();
	private final Runnable onChanged;

	public RequestSlip(Runnable onChanged)
	{
		this.onChanged = onChanged;
	}

	public List<Line> lines()
	{
		return Collections.unmodifiableList(lines);
	}

	public boolean isEmpty()
	{
		return lines.isEmpty();
	}

	public boolean add(ItemKey key, int amount, int most)
	{
		for(int i = 0; i<lines.size(); i++)
			if(lines.get(i).key().equals(key))
			{
				int total = Math.min(most, lines.get(i).amount()+amount);
				if(total==lines.get(i).amount())
					return false;
				lines.set(i, new Line(key, total));
				onChanged.run();
				return true;
			}
		if(lines.size()>=SIZE||Math.min(amount, most)<=0)
			return false;
		lines.add(new Line(key, Math.min(amount, most)));
		onChanged.run();
		return true;
	}

	public void remove(int index)
	{
		if(index<0||index>=lines.size())
			return;
		lines.remove(index);
		onChanged.run();
	}

	public void lower(int index, int by)
	{
		if(index<0||index>=lines.size())
			return;
		Line line = lines.get(index);
		if(line.amount()<=by)
			lines.remove(index);
		else
			lines.set(index, new Line(line.key(), line.amount()-by));
		onChanged.run();
	}

	public void set(List<Line> lines)
	{
		this.lines.clear();
		this.lines.addAll(lines.subList(0, Math.min(SIZE, lines.size())));
		onChanged.run();
	}
}
