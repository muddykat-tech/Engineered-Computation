package com.muddykat.engineeredcomputation.client.guide.page;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;

public final class TextPage implements GuidePage
{
	static final int LINE_HEIGHT = 9;
	public static final int TITLE_LINES = 2;
	public static final int LINES = HEIGHT/LINE_HEIGHT;

	@Nullable
	private final Component title;
	private final List<FormattedCharSequence> lines;

	public TextPage(@Nullable Component title, List<FormattedCharSequence> lines)
	{
		this.title = title;
		this.lines = lines;
	}

	@Override
	public void render(GuideScreen screen, GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		int lineY = y;
		if(title!=null)
		{
			screen.drawTitle(graphics, title, x, lineY);
			lineY += TITLE_LINES*LINE_HEIGHT;
		}
		for(FormattedCharSequence line : lines)
		{
			graphics.drawString(screen.font(), line, x, lineY, screen.textColour(), false);
			lineY += LINE_HEIGHT;
		}
	}
}
