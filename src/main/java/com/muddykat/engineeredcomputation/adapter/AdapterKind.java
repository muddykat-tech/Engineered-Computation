package com.muddykat.engineeredcomputation.adapter;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum AdapterKind implements StringRepresentable
{
	IMPORTER("data_importer"),
	EXPORTER("data_exporter");

	public static final Codec<AdapterKind> CODEC = StringRepresentable.fromEnum(AdapterKind::values);

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
