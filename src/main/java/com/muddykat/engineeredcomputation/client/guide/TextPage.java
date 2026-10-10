package com.muddykat.engineeredcomputation.client.guide;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class TextPage implements GuidePage
{
	static final int LINE_HEIGHT = 9;
	static final int TITLE_LINES = 2;
	static final int LINES = HEIGHT/LINE_HEIGHT;

	@Nullable
	private final Component title;
	private final List<FormattedCharSequence> lines;

	TextPage(@Nullable Component title, List<FormattedCharSequence> lines)
	{
		this.title = title;
		this.lines = lines;
	}

	@Override
	public void render(GuideScreen screen, PoseStack pose, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		int lineY = y;
		if(title!=null)
		{
			screen.drawTitle(pose, title, x, lineY);
			lineY += TITLE_LINES*LINE_HEIGHT;
		}
		for(FormattedCharSequence line : lines)
		{
			screen.font().draw(pose, line, x, lineY, screen.textColour());
			lineY += LINE_HEIGHT;
		}
	}
}
