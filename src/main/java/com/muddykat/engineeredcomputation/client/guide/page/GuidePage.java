package com.muddykat.engineeredcomputation.client.guide.page;

import net.minecraft.client.gui.GuiGraphics;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;

public interface GuidePage
{
	int WIDTH = 114;
	int HEIGHT = 117;

	void render(GuideScreen screen, GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick);

	default boolean mouseClicked(GuideScreen screen, int x, int y, double mouseX, double mouseY)
	{
		return false;
	}

	default void mouseDragged(double dragX, double dragY)
	{
	}
}
