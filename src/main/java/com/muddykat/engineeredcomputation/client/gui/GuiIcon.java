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
	BLOCK(6, 6),
	SORT_NAME(6, 6),
	SORT_COUNT(6, 6),
	ASCENDING(6, 6),
	DESCENDING(6, 6),
	CRAFTS_BOTH(6, 6),
	CRAFTS_ONLY(6, 6),
	CRAFTS_HIDDEN(6, 6),
	REMOTE_SHOWN(6, 6),
	REMOTE_HIDDEN(6, 6),
	REMOTE(5, 5),
	REMOTE_TOP(6, 6),
	REMOTE_BOTTOM(6, 6);

	private static final ResourceLocation SHEET =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "textures/gui/common_gui.png");
	private static final int SHEET_WIDTH = 64;
	private static final int SHEET_HEIGHT = 128;
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
		graphics.blit(SHEET, x, y, ordinal()%4*CELL, ordinal()/4*CELL, width, height, SHEET_WIDTH, SHEET_HEIGHT);
	}
}
