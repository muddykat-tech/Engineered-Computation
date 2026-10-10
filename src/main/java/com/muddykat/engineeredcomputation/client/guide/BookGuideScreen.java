package com.muddykat.engineeredcomputation.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
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
		addRenderableWidget(new Button(width/2-100, TOP+PAGE_HEIGHT+4, 200, 20, CommonComponents.GUI_DONE,
				button -> onClose()));
		backward = addRenderableWidget(new PageButton(left()+24, TOP+157, false, button -> turn(-1), true));
		forward = addRenderableWidget(new PageButton(left()+SPREAD_WIDTH-24-23, TOP+157, true, button -> turn(1),
				true));
	}

	@Override
	public void renderBackground(PoseStack pose)
	{
		super.renderBackground(pose);
		RenderSystem.disableCull();
		pose.pushPose();
		pose.translate(left()+PAGE_WIDTH, TOP, 0);
		pose.scale(-1, 1, 1);
		ECGui.blit(pose, BookViewScreen.BOOK_LOCATION, 0, 0, ART_U, 0, PAGE_WIDTH, PAGE_HEIGHT);
		pose.popPose();
		RenderSystem.enableCull();
		ECGui.blit(pose, BookViewScreen.BOOK_LOCATION, left()+PAGE_WIDTH, TOP, ART_U, 0, PAGE_WIDTH, PAGE_HEIGHT);
	}

	@Override
	protected void renderControls(PoseStack pose, int mouseX, int mouseY)
	{
		backward.visible = canTurn(-1);
		forward.visible = canTurn(1);
		int y = TOP+HEADER_Y;
		int x = contentX(0);
		font.draw(pose, backLabel(), x, y, !canGoBack()? DISABLED: linkColour(isOverBack(mouseX, mouseY)));
		Component contents = contentsLabel();
		font.draw(pose, contents, x+GuidePage.WIDTH-font.width(contents), y,
				linkColour(isOverContents(mouseX, mouseY)));

		int last = Math.min(page+2, guide.pages().size());
		Component pages = page+1==last
				? new TranslatableComponent("book.pageIndicator", last, guide.pages().size())
				: new TranslatableComponent("guide.engineered_computation.nav.pages", page+1, last,
						guide.pages().size());
		font.draw(pose, pages, contentX(1)+GuidePage.WIDTH-font.width(pages), y, TEXT);
	}

	private Component backLabel()
	{
		return new TranslatableComponent("guide.engineered_computation.nav.back");
	}

	private Component contentsLabel()
	{
		return new TranslatableComponent("guide.engineered_computation.nav.contents");
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
