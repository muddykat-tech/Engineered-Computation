package com.muddykat.engineeredcomputation.adapter;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import com.muddykat.engineeredcomputation.network.StatusCode;

public enum AdapterStatus
{
	OK(StatusCode.OK),
	NO_PROCESSOR(StatusCode.FAILED_DEPENDENCY),
	NOT_FOUND(StatusCode.NOT_FOUND),
	INSUFFICIENT_STORAGE(StatusCode.INSUFFICIENT_STORAGE),
	SERVICE_UNAVAILABLE(StatusCode.SERVICE_UNAVAILABLE),
	CONFLICT(StatusCode.CONFLICT),
	CONTENT_TOO_LARGE(StatusCode.CONTENT_TOO_LARGE),
	OFF(null);

	private static final int OFF_COLOUR = 0x9AA8A0;

	@Nullable
	private final StatusCode code;

	AdapterStatus(@Nullable StatusCode code)
	{
		this.code = code;
	}

	public Component title()
	{
		return code==null? Component.translatable(key()+".title"): code.title();
	}

	public int colour()
	{
		return code==null? OFF_COLOUR: code.colour();
	}

	public Component detail()
	{
		return Component.translatable(key()+".detail");
	}

	private String key()
	{
		return "gui.engineered_computation.adapter.status."+name().toLowerCase(Locale.ROOT);
	}
}
