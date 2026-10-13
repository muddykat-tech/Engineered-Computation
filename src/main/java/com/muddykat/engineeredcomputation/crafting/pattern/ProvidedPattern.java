package com.muddykat.engineeredcomputation.crafting.pattern;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

public record ProvidedPattern(Pattern pattern, @Nullable BlockPos processor, int slot, Map<Integer, Item> choices)
{
	public static ProvidedPattern direct(Pattern pattern)
	{
		return new ProvidedPattern(pattern, null, -1, Map.of());
	}

	public static ProvidedPattern abstractAt(Pattern pattern, BlockPos processor, int slot)
	{
		return new ProvidedPattern(pattern, processor, slot, Map.of());
	}

	public boolean isDirect()
	{
		return processor==null;
	}

	public ProvidedPattern choosing(Map<Integer, Item> choices)
	{
		return new ProvidedPattern(pattern, processor, slot, Map.copyOf(choices));
	}

	public Pattern resolved()
	{
		return pattern.inputTags().isEmpty()? pattern: pattern.resolve(choices).orElse(pattern);
	}

	public boolean isResolved()
	{
		return pattern.inputTags().isEmpty()||pattern.resolve(choices).isPresent();
	}
}
