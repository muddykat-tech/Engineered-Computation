package com.muddykat.engineeredcomputation.client.gui;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
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

	public static void panel(PoseStack pose, int x, int y, int width, int height)
	{
		int right = x+width;
		int bottom = y+height;
		GuiComponent.fill(pose, x+1, y+2, right-1, bottom-2, FACE);
		GuiComponent.fill(pose, x+2, y+1, right-2, bottom-1, FACE);
		GuiComponent.fill(pose, x+2, y+1, right-3, y+3, HIGHLIGHT);
		GuiComponent.fill(pose, x+1, y+2, x+3, bottom-3, HIGHLIGHT);
		GuiComponent.fill(pose, x+3, bottom-3, right-2, bottom-1, SHADOW);
		GuiComponent.fill(pose, right-3, y+3, right-1, bottom-2, SHADOW);

		GuiComponent.fill(pose, x+2, y, right-3, y+1, BLACK);
		GuiComponent.fill(pose, x, y+2, x+1, bottom-3, BLACK);
		GuiComponent.fill(pose, x+3, bottom-1, right-2, bottom, BLACK);
		GuiComponent.fill(pose, right-1, y+3, right, bottom-2, BLACK);
		GuiComponent.fill(pose, x+1, y+1, x+2, y+2, BLACK);
		GuiComponent.fill(pose, right-3, y+1, right-2, y+2, BLACK);
		GuiComponent.fill(pose, right-2, y+2, right-1, y+3, BLACK);
		GuiComponent.fill(pose, x+1, bottom-3, x+2, bottom-2, BLACK);
		GuiComponent.fill(pose, x+2, bottom-2, x+3, bottom-1, BLACK);
		GuiComponent.fill(pose, right-2, bottom-2, right-1, bottom-1, BLACK);
	}

	public static void sunken(PoseStack pose, int x, int y, int width, int height)
	{
		GuiComponent.fill(pose, x, y, x+width, y+height, SUNKEN_FACE);
		GuiComponent.fill(pose, x, y, x+width-1, y+1, SUNKEN_SHADOW);
		GuiComponent.fill(pose, x, y, x+1, y+height-1, SUNKEN_SHADOW);
		GuiComponent.fill(pose, x+1, y+height-1, x+width, y+height, HIGHLIGHT);
		GuiComponent.fill(pose, x+width-1, y+1, x+width, y+height, HIGHLIGHT);
	}

	public static void button(PoseStack pose, int x, int y, int width, int height, boolean hovered)
	{
		nineSlice(pose, WIDGETS, x, y, width, height, 20, 4, 200, 20, 0, hovered? 86: 66);
	}

	/** The creative inventory's scroll thumb, 12 by {@link #SCROLLER_HEIGHT}. */
	public static void scroller(PoseStack pose, int x, int y, boolean enabled)
	{
		blit(pose, CREATIVE_TABS, x, y, enabled? 232: 244, 0, 12, SCROLLER_HEIGHT);
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
	public static void filterMode(PoseStack pose, int x, int y, boolean blocking, boolean hovered)
	{
		button(pose, x, y, FILTER_MODE_SIZE, FILTER_MODE_SIZE, hovered);
		icon(pose, x+3, y+3, blocking? BLOCK_ICON: ALLOW_ICON, blocking? 0xFFFF6A5A: 0xFF6CFF7C);
	}

	public static void icon(PoseStack pose, int x, int y, String[] rows, int colour)
	{
		for(int pass = 0; pass<2; pass++)
			for(int row = 0; row<rows.length; row++)
				for(int col = 0; col<rows[row].length(); col++)
					if(rows[row].charAt(col)=='#')
					{
						int px = x+col+(pass==0? 1: 0);
						int py = y+row+(pass==0? 1: 0);
						GuiComponent.fill(pose, px, py, px+1, py+1, pass==0? 0xFF3F3F3F: colour);
					}
	}

	/** What a program entry shows as: its item, or for a tag each of its items in turn (a barrier if it has none). */
	public static ItemStack entryIcon(ProgramEntry entry)
	{
		if(entry.item().isPresent())
			return new ItemStack(entry.item().get());
		Optional<HolderSet.Named<Item>> items = entry.tag().flatMap(Registry.ITEM::getTag);
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

	public static void tagBadge(PoseStack pose, Font font, int x, int y)
	{
		pose.pushPose();
		pose.translate(0, 0, 200);
		font.drawShadow(pose, "#", x+1, y, 0x55FFFF);
		pose.popPose();
	}

	public static void slot(PoseStack pose, int x, int y)
	{
		blit(pose, CONTAINER, x, y, 7, 17, 18, 18);
	}

	/** A gauge in one colour: for something it's good to have full, like stored RF. */
	public static void gauge(PoseStack pose, int x, int y, int width, int height, double fraction, int colour)
	{
		sunken(pose, x, y, width, height);
		int filled = (int)Math.round((width-2)*Math.max(0, Math.min(1, fraction)));
		if(filled>0)
			GuiComponent.fill(pose, x+1, y+1, x+1+filled, y+height-1, colour);
	}

	public static void gauge(PoseStack pose, int x, int y, int width, int height, double fraction)
	{
		sunken(pose, x, y, width, height);
		double clamped = Math.max(0, Math.min(1, fraction));
		int filled = (int)Math.round((width-2)*clamped);
		if(filled>0)
			GuiComponent.fill(pose, x+1, y+1, x+1+filled, y+height-1, gaugeColour(clamped));
	}

	private static int gaugeColour(double fraction)
	{
		int red = fraction<0.7? (int)(255*fraction/0.7): 255;
		int green = fraction<0.7? 200: (int)(200*(1-(fraction-0.7)/0.3));
		return 0xFF000000|red<<16|Math.max(0, green)<<8|40;
	}

	/** A region of a 256x256 texture. */
	public static void blit(PoseStack pose, ResourceLocation texture, int x, int y, int u, int v, int width, int height)
	{
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		GuiComponent.blit(pose, x, y, u, v, width, height, 256, 256);
	}

	/**
	 * A texture region stretched to any size the way vanilla sizes its widgets: fixed borders, the edges and middle
	 * tiled. Borders shrink to half the size when it is smaller, and the far ones are then cut from the region's own
	 * edges, as 1.20's blitNineSliced does.
	 */
	public static void nineSlice(PoseStack pose, ResourceLocation texture, int x, int y, int width, int height,
			int border, int borderHeight, int u0Width, int v0Height, int u, int v)
	{
		int left = Math.min(border, width/2), top = Math.min(borderHeight, height/2);
		int innerU = u0Width-2*left, innerV = v0Height-2*top;
		for(int row = 0; row<3; row++)
		{
			int dy = row==0? 0: row==1? top: height-top;
			int rowHeight = row==1? height-2*top: top;
			int sv = row==0? v: row==1? v+top: v+v0Height-top;
			int tileV = row==1? innerV: top;
			for(int col = 0; col<3; col++)
			{
				int dx = col==0? 0: col==1? left: width-left;
				int colWidth = col==1? width-2*left: left;
				int su = col==0? u: col==1? u+left: u+u0Width-left;
				int tileU = col==1? innerU: left;
				for(int ty = 0; ty<rowHeight; ty += tileV)
					for(int tx = 0; tx<colWidth; tx += tileU)
						blit(pose, texture, x+dx+tx, y+dy+ty, su, sv, Math.min(tileU, colWidth-tx),
								Math.min(tileV, rowHeight-ty));
			}
		}
	}

	/** A filled rectangle drawn at depth {@code z}, so it lands over (or under) items drawn at their own depth. */
	public static void fill(PoseStack pose, int x1, int y1, int x2, int y2, int z, int colour)
	{
		pose.pushPose();
		pose.translate(0, 0, z);
		GuiComponent.fill(pose, x1, y1, x2, y2, colour);
		pose.popPose();
	}

	/** A filled rectangle drawn over whatever is already there, items included (1.20's gui overlay layer). */
	public static void overlay(PoseStack pose, int x1, int y1, int x2, int y2, int colour)
	{
		RenderSystem.disableDepthTest();
		GuiComponent.fill(pose, x1, y1, x2, y2, colour);
		RenderSystem.enableDepthTest();
	}

	/** A one-pixel frame. */
	public static void outline(PoseStack pose, int x, int y, int width, int height, int colour)
	{
		GuiComponent.fill(pose, x, y, x+width, y+1, colour);
		GuiComponent.fill(pose, x, y+height-1, x+width, y+height, colour);
		GuiComponent.fill(pose, x, y+1, x+1, y+height-1, colour);
		GuiComponent.fill(pose, x+width-1, y+1, x+width, y+height-1, colour);
	}

	/** An item, moved by {@code pose}: item rendering only follows the model-view stack. */
	public static void renderItem(PoseStack pose, ItemStack stack, int x, int y)
	{
		withPose(pose, () -> Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y));
	}

	/** An item's count and durability, moved by {@code pose}; {@code text} replaces the count (null keeps it). */
	public static void renderItemDecorations(PoseStack pose, Font font, ItemStack stack, int x, int y,
			@Nullable String text)
	{
		withPose(pose, () -> Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y,
				text));
	}

	private static void withPose(PoseStack pose, Runnable draw)
	{
		PoseStack view = RenderSystem.getModelViewStack();
		view.pushPose();
		view.mulPoseMatrix(pose.last().pose());
		RenderSystem.applyModelViewMatrix();
		draw.run();
		view.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	/** Clips drawing to a rectangle given in GUI coordinates. */
	public static void enableScissor(int x1, int y1, int x2, int y2)
	{
		Window window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor((int)(x1*scale), (int)(window.getHeight()-y2*scale), (int)((x2-x1)*scale),
				(int)((y2-y1)*scale));
	}

	public static void disableScissor()
	{
		RenderSystem.disableScissor();
	}

	/** Placeholder text shown in an empty, unfocused text box, where its text would be (styled by {@code hint}). */
	public static void hint(PoseStack pose, Font font, @Nullable EditBox box, Component hint, boolean bordered)
	{
		if(box!=null&&box.isVisible()&&box.getValue().isEmpty()&&!box.isFocused())
			font.drawShadow(pose, fit(font, hint, box.getInnerWidth()), bordered? box.x+4: box.x,
					bordered? box.y+(box.getHeight()-8)/2: box.y, 0xE0E0E0);
	}
}
