package com.muddykat.engineeredcomputation.terminal;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

public final class ItemSearch
{
	private ItemSearch()
	{
	}

	public static String normalise(String query)
	{
		return query.trim().toLowerCase(Locale.ROOT);
	}

	public static boolean matches(String name, ResourceLocation id, String query)
	{
		String normalised = normalise(query);
		if(normalised.isEmpty())
			return true;
		if(normalised.startsWith("@"))
			return id.getNamespace().startsWith(normalised.substring(1));
		return name.toLowerCase(Locale.ROOT).contains(normalised);
	}
}
