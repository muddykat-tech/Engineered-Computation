package com.muddykat.engineeredcomputation.client.guide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.registration.ECContent;

public abstract class GuideScreen extends Screen
{
	protected final ComputerEra era;
	protected Guide guide;
	protected int page;
	private final Deque<Integer> history = new ArrayDeque<>();
	private ItemStack hoveredStack = ItemStack.EMPTY;
	private final Map<Item, Optional<ComputerEra>> elsewhere = new HashMap<>();

	protected GuideScreen(ComputerEra era)
	{
		super(ECContent.guide(era).get().getDescription());
		this.era = era;
	}

	public static GuideScreen open(ComputerEra era)
	{
		return era==ComputerEra.FLOPPY? new PalmGuideScreen(era): new BookGuideScreen(era);
	}

	protected abstract int pagesShown();

	protected abstract int contentX(int index);

	protected abstract int contentY();

	abstract int textColour();

	abstract int linkColour(boolean hovered);

	void drawSlot(PoseStack pose, int x, int y, int size)
	{
		if(size==18)
			ECGui.slot(pose, x, y);
		else
			ECGui.sunken(pose, x, y, size, size);
	}

	void drawArrow(PoseStack pose, int x, int y)
	{
		int colour = 0xFF000000|textColour();
		GuiComponent.fill(pose, x, y-1, x+11, y+1, colour);
		for(int step = 0; step<5; step++)
			GuiComponent.fill(pose, x+11+step, y-5+step, x+12+step, y+5-step, colour);
	}

	@Override
	protected void init()
	{
		guide = GuideBook.build(font, era);
		page = viewStart(page);
	}

	Font font()
	{
		return font;
	}

	void goTo(int target)
	{
		int start = viewStart(target);
		if(start!=page)
		{
			history.push(page);
			page = start;
		}
	}

	void turn(int direction)
	{
		page = viewStart(page+direction*pagesShown());
	}

	void back()
	{
		if(!history.isEmpty())
			page = history.pop();
	}

	void contents()
	{
		goTo(0);
	}

	boolean canGoBack()
	{
		return !history.isEmpty();
	}

	boolean canTurn(int direction)
	{
		return viewStart(page+direction*pagesShown())!=page;
	}

	boolean isShown(int target)
	{
		return target>=page&&target<page+pagesShown();
	}

	private int viewStart(int target)
	{
		int clamped = Math.max(0, Math.min(guide==null? 0: guide.pages().size()-1, target));
		return clamped-clamped%pagesShown();
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		hoveredStack = ItemStack.EMPTY;
		for(int i = 0; i<pagesShown()&&page+i<guide.pages().size(); i++)
			guide.pages().get(page+i).render(this, pose, contentX(i), contentY(), mouseX, mouseY, partialTick);
		renderControls(pose, mouseX, mouseY);
		if(!hoveredStack.isEmpty())
			renderTooltip(pose, tooltip(hoveredStack), hoveredStack.getTooltipImage(), mouseX, mouseY, hoveredStack);
		else
			renderControlTooltip(pose, mouseX, mouseY);
	}

	protected abstract void renderControls(PoseStack pose, int mouseX, int mouseY);

	protected void renderControlTooltip(PoseStack pose, int mouseX, int mouseY)
	{
	}

	void renderStack(PoseStack pose, ItemStack stack, int x, int y, int mouseX, int mouseY)
	{
		if(stack.isEmpty())
			return;
		ECGui.renderItem(pose, stack, x, y);
		ECGui.renderItemDecorations(pose, font, stack, x, y, null);
		if(mouseX>=x-1&&mouseX<x+17&&mouseY>=y-1&&mouseY<y+17)
			hoveredStack = stack;
	}

	private List<Component> tooltip(ItemStack stack)
	{
		List<Component> lines = new ArrayList<>(getTooltipFromItem(stack));
		Integer target = guide.recipePages().get(stack.getItem());
		if(target!=null)
		{
			if(!isShown(target))
				lines.add(new TranslatableComponent("guide.engineered_computation.link.recipe")
						.withStyle(ChatFormatting.DARK_AQUA));
		}
		else
			coveringEra(stack.getItem()).ifPresent(other -> lines.add(new TranslatableComponent(
					"guide.engineered_computation.link.elsewhere", ECContent.guide(other).get().getDescription())
					.withStyle(ChatFormatting.GRAY)));
		return lines;
	}

	private Optional<ComputerEra> coveringEra(Item item)
	{
		return elsewhere.computeIfAbsent(item, i -> {
			for(ComputerEra other : ComputerEra.values())
				if(other!=era&&GuideBook.build(font, other).recipePages().containsKey(i))
					return Optional.of(other);
			return Optional.empty();
		});
	}

	void drawTitle(PoseStack pose, Component title, int x, int y)
	{
		Component bold = title.copy().withStyle(ChatFormatting.BOLD);
		int width = Math.min(font.width(bold), GuidePage.WIDTH);
		drawFitted(pose, bold, x+(GuidePage.WIDTH-width)/2, y, GuidePage.WIDTH, textColour());
	}

	void drawFitted(PoseStack pose, Component text, int x, int y, int maxWidth, int colour)
	{
		int width = font.width(text);
		if(width<=maxWidth)
		{
			font.draw(pose, text, x, y, colour);
			return;
		}
		float scale = (float)maxWidth/width;
		pose.pushPose();
		pose.translate(x, y+(font.lineHeight-1)*(1-scale)/2, 0);
		pose.scale(scale, scale, 1);
		font.draw(pose, text, 0, 0, colour);
		pose.popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(super.mouseClicked(mouseX, mouseY, button))
			return true;
		if(button!=GLFW.GLFW_MOUSE_BUTTON_LEFT)
			return false;
		if(controlClicked(mouseX, mouseY))
			return true;
		for(int i = 0; i<pagesShown()&&page+i<guide.pages().size(); i++)
			if(guide.pages().get(page+i).mouseClicked(this, contentX(i), contentY(), mouseX, mouseY))
				return true;
		Integer target = hoveredStack.isEmpty()? null: guide.recipePages().get(hoveredStack.getItem());
		if(target!=null&&!isShown(target))
		{
			goTo(target);
			return true;
		}
		return false;
	}

	protected abstract boolean controlClicked(double mouseX, double mouseY);

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		for(int i = 0; i<pagesShown()&&page+i<guide.pages().size(); i++)
			if(mouseX>=contentX(i)&&mouseX<contentX(i)+GuidePage.WIDTH)
				guide.pages().get(page+i).mouseDragged(dragX, dragY);
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
	{
		if(scrollY!=0)
		{
			turn(scrollY<0? 1: -1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_LEFT -> turn(-1);
			case GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_RIGHT -> turn(1);
			case GLFW.GLFW_KEY_BACKSPACE -> back();
			case GLFW.GLFW_KEY_HOME -> contents();
			default ->
			{
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

}
