package com.muddykat.engineeredcomputation.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;

interface GuidePage
{
	int WIDTH = 114;
	int HEIGHT = 117;

	void render(GuideScreen screen, PoseStack pose, int x, int y, int mouseX, int mouseY, float partialTick);

	default boolean mouseClicked(GuideScreen screen, int x, int y, double mouseX, double mouseY)
	{
		return false;
	}

	default void mouseDragged(double dragX, double dragY)
	{
	}
}
