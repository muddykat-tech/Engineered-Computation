package com.muddykat.engineeredcomputation.client.gui;

import java.util.Optional;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.muddykat.engineeredcomputation.adapter.ProgramEntry;

public final class ECGui
{
	private static final int CYCLE_MILLIS = 1000;
	// Vanilla's slot, creative inventory scroller and button art, drawn from the textures they live in.
	private static final ResourceLocation CONTAINER = ResourceLocation.withDefaultNamespace(
			"textures/gui/container/generic_54.png");
	private static final ResourceLocation CREATIVE_TABS = ResourceLocation.withDefaultNamespace(
			"textures/gui/container/creative_inventory/tabs.png");
	private static final ResourceLocation WIDGETS = ResourceLocation.withDefaultNamespace("textures/gui/widgets.png");
	public static final int SCROLLER_HEIGHT = 15;

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
		graphics.blitNineSliced(WIDGETS, x, y, width, height, 20, 4, 200, 20, 0, hovered? 86: 66);
	}

	/** The creative inventory's scroll thumb, 12 by {@link #SCROLLER_HEIGHT}. */
	public static void scroller(GuiGraphics graphics, int x, int y, boolean enabled)
	{
		graphics.blit(CREATIVE_TABS, x, y, enabled? 232: 244, 0, 12, SCROLLER_HEIGHT);
	}

	/** The size of {@link #filterMode}'s button. */
	public static final int FILTER_MODE_SIZE = 11;
	private static final String[] ALLOW_ICON = {
			"....#",
			"...##",
			"#.##.",
			"###..",
			".#..."};
	private static final String[] BLOCK_ICON = {
			".###.",
			"#..##",
			"#.#.#",
			"##..#",
			".###."};

	/** A filter's allow / block list button: a green tick for an allow list, a red no-entry sign for a block list. */
	public static void filterMode(GuiGraphics graphics, int x, int y, boolean blocking, boolean hovered)
	{
		button(graphics, x, y, FILTER_MODE_SIZE, FILTER_MODE_SIZE, hovered);
		icon(graphics, x+3, y+3, blocking? BLOCK_ICON: ALLOW_ICON, blocking? 0xFFFF6A5A: 0xFF6CFF7C);
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

	/** What a program entry shows as: its item, or for a tag each of its items in turn (a barrier if it has none). */
	public static ItemStack entryIcon(ProgramEntry entry)
	{
		if(entry.item().isPresent())
			return new ItemStack(entry.item().get());
		Optional<HolderSet.Named<Item>> items = entry.tag().flatMap(BuiltInRegistries.ITEM::getTag);
		if(items.isEmpty()||items.get().size()==0)
			return entry.tag().isPresent()? new ItemStack(Items.BARRIER): ItemStack.EMPTY;
		long cycle = Util.getMillis()/CYCLE_MILLIS;
		return new ItemStack(items.get().get((int)(cycle%items.get().size())));
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
		graphics.blit(CONTAINER, x, y, 7, 17, 18, 18);
	}

	/** A gauge in one colour: for something it's good to have full, like stored RF. */
	public static void gauge(GuiGraphics graphics, int x, int y, int width, int height, double fraction, int colour)
	{
		sunken(graphics, x, y, width, height);
		int filled = (int)Math.round((width-2)*Math.max(0, Math.min(1, fraction)));
		if(filled>0)
			graphics.fill(x+1, y+1, x+1+filled, y+height-1, colour);
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
