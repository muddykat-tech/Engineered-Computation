package com.muddykat.engineeredcomputation.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public enum GuiIcon
{
	LINK_ON(9, 9),
	LINK_OFF(9, 9),
	CROSS(6, 6),
	CROSS_RED(6, 6),
	JOBS(6, 6),
	JOBS_ACTIVE(6, 6),
	SEND(6, 6),
	SEND_DISABLED(6, 6),
	LOCKED_SHOWN(6, 6),
	LOCKED_HIDDEN(6, 6),
	LOCKED(4, 5),
	ALLOW(6, 6),
	BLOCK(6, 6);

	private static final ResourceLocation SHEET =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "textures/gui/common_gui.png");
	private static final int SHEET_SIZE = 64;
	private static final int CELL = 16;

	private final int width;
	private final int height;

	GuiIcon(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public void draw(GuiGraphics graphics, int x, int y)
	{
		graphics.blit(SHEET, x, y, ordinal()%4*CELL, ordinal()/4*CELL, width, height, SHEET_SIZE, SHEET_SIZE);
	}
}
