package com.muddykat.engineeredcomputation.network;

import net.minecraft.network.chat.Component;

public enum StatusCode
{
	PROCESSING(102, 0xFFD24A),
	OK(200, 0x55FF77),
	NO_CONTENT(204, 0x8CC8FF),
	NOT_FOUND(404, 0xFFB830),
	CONFLICT(409, 0xFF5A4A),
	CONTENT_TOO_LARGE(413, 0xFF5A4A),
	PAYMENT_REQUIRED(402, 0xFF8A3A),
	FAILED_DEPENDENCY(424, 0xFF8A3A),
	SERVICE_UNAVAILABLE(503, 0xFF5A4A),
	INSUFFICIENT_STORAGE(507, 0xFFB830);

	private final int code;
	private final int colour;

	StatusCode(int code, int colour)
	{
		this.code = code;
		this.colour = colour;
	}

	public int code()
	{
		return code;
	}

	public int colour()
	{
		return colour;
	}

	public Component label()
	{
		return Component.translatable("gui.engineered_computation.code."+code);
	}

	public Component title()
	{
		return Component.literal(code+" ").append(label());
	}
}
