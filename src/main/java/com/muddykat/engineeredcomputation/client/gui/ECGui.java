package com.muddykat.engineeredcomputation.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class ECGui
{
	public static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");
	public static final ResourceLocation SCROLLER =
			ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
	public static final ResourceLocation SCROLLER_DISABLED =
			ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
	public static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
	public static final ResourceLocation BUTTON_HIGHLIGHTED =
			ResourceLocation.withDefaultNamespace("widget/button_highlighted");

	public static final int LABEL = 0x404040;

	private static final int BLACK = 0xFF000000;
	private static final int FACE = 0xFFC6C6C6;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int SUNKEN_SHADOW = 0xFF373737;
	private static final int SUNKEN_FACE = 0xFF8B8B8B;

	private ECGui()
	{
	}

	public static void panel(GuiGraphics graphics, int x, int y, int width, int height)
	{
		int right = x+width;
		int bottom = y+height;
		graphics.fill(x+1, y+2, right-1, bottom-2, FACE);
		graphics.fill(x+2, y+1, right-2, bottom-1, FACE);
		graphics.fill(x+2, y+1, right-3, y+3, HIGHLIGHT);
		graphics.fill(x+1, y+2, x+3, bottom-3, HIGHLIGHT);
		graphics.fill(x+3, bottom-3, right-2, bottom-1, SHADOW);
		graphics.fill(right-3, y+3, right-1, bottom-2, SHADOW);

		graphics.fill(x+2, y, right-3, y+1, BLACK);
		graphics.fill(x, y+2, x+1, bottom-3, BLACK);
		graphics.fill(x+3, bottom-1, right-2, bottom, BLACK);
		graphics.fill(right-1, y+3, right, bottom-2, BLACK);
		graphics.fill(x+1, y+1, x+2, y+2, BLACK);
		graphics.fill(right-3, y+1, right-2, y+2, BLACK);
		graphics.fill(right-2, y+2, right-1, y+3, BLACK);
		graphics.fill(x+1, bottom-3, x+2, bottom-2, BLACK);
		graphics.fill(x+2, bottom-2, x+3, bottom-1, BLACK);
		graphics.fill(right-2, bottom-2, right-1, bottom-1, BLACK);
	}

	public static void sunken(GuiGraphics graphics, int x, int y, int width, int height)
	{
		graphics.fill(x, y, x+width, y+height, SUNKEN_FACE);
		graphics.fill(x, y, x+width-1, y+1, SUNKEN_SHADOW);
		graphics.fill(x, y, x+1, y+height-1, SUNKEN_SHADOW);
		graphics.fill(x+1, y+height-1, x+width, y+height, HIGHLIGHT);
		graphics.fill(x+width-1, y+1, x+width, y+height, HIGHLIGHT);
	}

	public static void button(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered)
	{
		graphics.blitSprite(hovered? BUTTON_HIGHLIGHTED: BUTTON, x, y, width, height);
	}

	public static void icon(GuiGraphics graphics, int x, int y, String[] rows, int colour)
	{
		for(int pass = 0; pass<2; pass++)
			for(int row = 0; row<rows.length; row++)
				for(int col = 0; col<rows[row].length(); col++)
					if(rows[row].charAt(col)=='#')
					{
						int px = x+col+(pass==0? 1: 0);
						int py = y+row+(pass==0? 1: 0);
						graphics.fill(px, py, px+1, py+1, pass==0? 0xFF3F3F3F: colour);
					}
	}

	public static FormattedCharSequence fit(Font font, Component text, int width)
	{
		if(font.width(text)<=width)
			return text.getVisualOrderText();
		FormattedText cut = font.substrByWidth(text, width-font.width("…"));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
	}

	public static void tagBadge(GuiGraphics graphics, Font font, int x, int y)
	{
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 200);
		graphics.drawString(font, "#", x+1, y, 0x55FFFF, true);
		graphics.pose().popPose();
	}

	public static void slot(GuiGraphics graphics, int x, int y)
	{
		graphics.blitSprite(SLOT, x, y, 18, 18);
	}

	public static void gauge(GuiGraphics graphics, int x, int y, int width, int height, double fraction)
	{
		sunken(graphics, x, y, width, height);
		double clamped = Math.max(0, Math.min(1, fraction));
		int filled = (int)Math.round((width-2)*clamped);
		if(filled>0)
			graphics.fill(x+1, y+1, x+1+filled, y+height-1, gaugeColour(clamped));
	}

	private static int gaugeColour(double fraction)
	{
		int red = fraction<0.7? (int)(255*fraction/0.7): 255;
		int green = fraction<0.7? 200: (int)(200*(1-(fraction-0.7)/0.3));
		return 0xFF000000|red<<16|Math.max(0, green)<<8|40;
	}
}
