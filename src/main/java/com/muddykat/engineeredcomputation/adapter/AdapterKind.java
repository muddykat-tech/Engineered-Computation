package com.muddykat.engineeredcomputation.adapter;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum AdapterKind implements StringRepresentable
{
	IMPORTER("data_importer"),
	EXPORTER("data_exporter");

	public static final Codec<AdapterKind> CODEC = StringRepresentable.fromEnum(AdapterKind::values,
			AdapterKind::byName);

	@Nullable
	public static AdapterKind byName(String name)
	{
		for(AdapterKind value : values())
			if(value.getSerializedName().equals(name))
				return value;
		return null;
	}

	private final String name;

	AdapterKind(String name)
	{
		this.name = name;
	}

	@Override
	public String getSerializedName()
	{
		return name;
	}
}
