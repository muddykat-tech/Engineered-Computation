package com.muddykat.engineeredcomputation.client.guide.page;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import com.muddykat.engineeredcomputation.client.guide.GuideScreen;

public final class ContentsPage implements GuidePage
{
	static final int ENTRY_HEIGHT = 11;
	private static final int FIRST_ENTRY_Y = TextPage.TITLE_LINES*TextPage.LINE_HEIGHT;
	public static final int ENTRIES = (HEIGHT-FIRST_ENTRY_Y)/ENTRY_HEIGHT;
	private static final int INDENT = 8;

	private final Component title;
	private final List<Entry> entries;

	public ContentsPage(Component title, List<Entry> entries)
	{
		this.title = title;
		this.entries = List.copyOf(entries);
	}

	@Override
	public void render(GuideScreen screen, GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick)
	{
		screen.drawTitle(graphics, title, x, y);
		for(int i = 0; i<entries.size(); i++)
		{
			Entry entry = entries.get(i);
			int indent = entry.section()? INDENT: 0;
			boolean hovered = isOver(i, x, y, mouseX, mouseY);
			Component label = Component.literal(entry.section()? "· ": "› ").append(entry.label());
			if(hovered)
				label = label.copy().withStyle(style -> style.withUnderlined(true));
			screen.drawFitted(graphics, label, x+indent, y+FIRST_ENTRY_Y+i*ENTRY_HEIGHT, WIDTH-indent,
					screen.linkColour(hovered));
		}
	}

	@Override
	public boolean mouseClicked(GuideScreen screen, int x, int y, double mouseX, double mouseY)
	{
		for(int i = 0; i<entries.size(); i++)
			if(isOver(i, x, y, mouseX, mouseY))
			{
				screen.goTo(entries.get(i).page());
				return true;
			}
		return false;
	}

	private boolean isOver(int index, int x, int y, double mouseX, double mouseY)
	{
		int entryY = y+FIRST_ENTRY_Y+index*ENTRY_HEIGHT;
		return mouseX>=x&&mouseX<x+WIDTH&&mouseY>=entryY-1&&mouseY<entryY+ENTRY_HEIGHT-1;
	}

	public record Entry(Component label, int page, boolean section)
	{
	}
}
