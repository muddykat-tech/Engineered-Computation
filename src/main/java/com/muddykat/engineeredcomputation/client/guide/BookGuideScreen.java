package com.muddykat.engineeredcomputation.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import com.muddykat.engineeredcomputation.era.ComputerEra;

final class BookGuideScreen extends GuideScreen
{
	private static final int TEXT = 0x000000;
	private static final int LINK = 0x1E3F8A;
	private static final int LINK_HOVERED = 0x3A6FD8;
	private static final int DISABLED = 0x9A8F7A;

	private static final int ART_U = 20;
	private static final int PAGE_WIDTH = 146;
	private static final int PAGE_HEIGHT = 182;
	private static final int SPREAD_WIDTH = 2*PAGE_WIDTH;
	private static final int TOP = 2;
	private static final int MARGIN = 16;
	private static final int CONTENT_Y = 30;
	private static final int HEADER_Y = 16;

	private PageButton forward;
	private PageButton backward;

	BookGuideScreen(ComputerEra era)
	{
		super(era);
	}

	@Override
	protected int pagesShown()
	{
		return 2;
	}

	@Override
	protected int contentX(int index)
	{
		return left()+index*PAGE_WIDTH+MARGIN;
	}

	@Override
	protected int contentY()
	{
		return TOP+CONTENT_Y;
	}

	@Override
	int textColour()
	{
		return TEXT;
	}

	@Override
	int linkColour(boolean hovered)
	{
		return hovered? LINK_HOVERED: LINK;
	}

	private int left()
	{
		return (width-SPREAD_WIDTH)/2;
	}

	@Override
	protected void init()
	{
		super.init();
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width/2-100, TOP+PAGE_HEIGHT+4, 200, 20).build());
		backward = addRenderableWidget(new PageButton(left()+24, TOP+157, false, button -> turn(-1), true));
		forward = addRenderableWidget(new PageButton(left()+SPREAD_WIDTH-24-23, TOP+157, true, button -> turn(1),
				true));
	}

	@Override
	public void renderBackground(GuiGraphics graphics)
	{
		super.renderBackground(graphics);
		RenderSystem.disableCull();
		graphics.pose().pushPose();
		graphics.pose().translate(left()+PAGE_WIDTH, TOP, 0);
		graphics.pose().scale(-1, 1, 1);
		graphics.blit(BookViewScreen.BOOK_LOCATION, 0, 0, ART_U, 0, PAGE_WIDTH, PAGE_HEIGHT);
		graphics.pose().popPose();
		RenderSystem.enableCull();
		graphics.blit(BookViewScreen.BOOK_LOCATION, left()+PAGE_WIDTH, TOP, ART_U, 0, PAGE_WIDTH, PAGE_HEIGHT);
	}

	@Override
	protected void renderControls(GuiGraphics graphics, int mouseX, int mouseY)
	{
		backward.visible = canTurn(-1);
		forward.visible = canTurn(1);
		int y = TOP+HEADER_Y;
		int x = contentX(0);
		graphics.drawString(font, backLabel(), x, y, !canGoBack()? DISABLED: linkColour(isOverBack(mouseX, mouseY)),
				false);
		Component contents = contentsLabel();
		graphics.drawString(font, contents, x+GuidePage.WIDTH-font.width(contents), y,
				linkColour(isOverContents(mouseX, mouseY)), false);

		int last = Math.min(page+2, guide.pages().size());
		Component pages = page+1==last
				? Component.translatable("book.pageIndicator", last, guide.pages().size())
				: Component.translatable("guide.engineered_computation.nav.pages", page+1, last, guide.pages().size());
		graphics.drawString(font, pages, contentX(1)+GuidePage.WIDTH-font.width(pages), y, TEXT, false);
	}

	private Component backLabel()
	{
		return Component.translatable("guide.engineered_computation.nav.back");
	}

	private Component contentsLabel()
	{
		return Component.translatable("guide.engineered_computation.nav.contents");
	}

	private boolean isOverBack(double mouseX, double mouseY)
	{
		return isOverHeader(mouseX, mouseY, contentX(0), font.width(backLabel()));
	}

	private boolean isOverContents(double mouseX, double mouseY)
	{
		int width = font.width(contentsLabel());
		return isOverHeader(mouseX, mouseY, contentX(0)+GuidePage.WIDTH-width, width);
	}

	private boolean isOverHeader(double mouseX, double mouseY, int x, int width)
	{
		return mouseX>=x&&mouseX<x+width&&mouseY>=TOP+HEADER_Y-1&&mouseY<TOP+HEADER_Y+9;
	}

	@Override
	protected boolean controlClicked(double mouseX, double mouseY)
	{
		if(isOverBack(mouseX, mouseY)&&canGoBack())
		{
			back();
			return true;
		}
		if(isOverContents(mouseX, mouseY))
		{
			contents();
			return true;
		}
		return false;
	}
}
