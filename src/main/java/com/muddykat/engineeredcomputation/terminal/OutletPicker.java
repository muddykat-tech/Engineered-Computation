package com.muddykat.engineeredcomputation.terminal;

import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.ECPayloads;

final class OutletPicker
{
	static final int WIDTH = TerminalMenu.COLUMNS*18+18;
	private static final int LINE_HEIGHT = 12;
	private static final int LINES = 6;
	private static final int SEARCH_HEIGHT = 14;

	private final TerminalMenu menu;
	private final Font font;
	private final int x;
	private final int y;
	private final EditBox search;
	private int scroll;
	private boolean closed;

	OutletPicker(TerminalMenu menu, Font font, int x, int y)
	{
		this.menu = menu;
		this.font = font;
		this.x = x;
		this.y = y;
		search = new EditBox(font, x+4, y+3, WIDTH-8, 10,
				new TranslatableComponent("gui.engineered_computation.teletype.search"));
		search.setBordered(false);
		search.setMaxLength(32);
		search.setTextColor(0xFFFFFF);
		search.setFocus(true);
		search.setResponder(text -> scroll = 0);
	}

	boolean isClosed()
	{
		return closed;
	}

	private List<TeletypeSlipPayload.Outlet> shown()
	{
		String query = search.getValue().strip().toLowerCase(Locale.ROOT);
		return menu.getClientOutlets().stream()
				.filter(outlet -> outlet.label().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
	}

	private int lines()
	{
		return Math.max(1, Math.min(LINES, shown().size()));
	}

	private int height()
	{
		return SEARCH_HEIGHT+lines()*LINE_HEIGHT+2;
	}

	private boolean isOver(double mouseX, double mouseY)
	{
		return mouseX>=x&&mouseX<x+WIDTH&&mouseY>=y&&mouseY<y+height();
	}

	private int lineAt(double mouseX, double mouseY)
	{
		if(!isOver(mouseX, mouseY)||mouseY<y+SEARCH_HEIGHT)
			return -1;
		int index = scroll+(int)((mouseY-y-SEARCH_HEIGHT-1)/LINE_HEIGHT);
		return index<shown().size()? index: -1;
	}

	void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		List<TeletypeSlipPayload.Outlet> shown = shown();
		int lines = lines();
		scroll = Math.max(0, Math.min(scroll, shown.size()-lines));
		pose.pushPose();
		pose.translate(0, 0, 400);
		GuiComponent.fill(pose, x-1, y-1, x+WIDTH+1, y+height()+1, 0xFFA0A0A0);
		GuiComponent.fill(pose, x, y, x+WIDTH, y+height(), 0xF0101010);
		GuiComponent.fill(pose, x, y+SEARCH_HEIGHT-1, x+WIDTH, y+SEARCH_HEIGHT, 0xFF505050);
		search.render(pose, mouseX, mouseY, partialTick);
		ECGui.hint(pose, font, search, new TranslatableComponent("gui.engineered_computation.teletype.search")
				.withStyle(style -> style.withColor(0x707070)), false);
		if(shown.isEmpty())
			font.draw(pose, new TranslatableComponent("gui.engineered_computation.teletype.search.none"), x+4,
					y+SEARCH_HEIGHT+3, 0xA0A0A0);
		int hovered = lineAt(mouseX, mouseY);
		TeletypeSlipPayload.Outlet home = menu.getClientOutlets().isEmpty()? null: menu.getClientOutlets().get(0);
		for(int i = 0; i<Math.min(lines, shown.size()-scroll); i++)
		{
			TeletypeSlipPayload.Outlet outlet = shown.get(scroll+i);
			int lineY = y+SEARCH_HEIGHT+1+i*LINE_HEIGHT;
			if(scroll+i==hovered)
				GuiComponent.fill(pose, x+1, lineY, x+WIDTH-1, lineY+LINE_HEIGHT, 0x40FFFFFF);
			String where = outlet.equals(home)?
					new TranslatableComponent("gui.engineered_computation.teletype.here").getString()
					: new TranslatableComponent("gui.engineered_computation.teletype.distance",
							Math.round(Math.sqrt(outlet.pos().distSqr(menu.getTerminalPos())))).getString();
			font.draw(pose, where, x+WIDTH-4-font.width(where), lineY+2, 0x8CC8FF);
			font.draw(pose, ECGui.fit(font, outlet.label(), WIDTH-12-font.width(where)), x+4, lineY+2,
					outlet.chosen()? 0xFFD24A: 0xFFFFFF);
		}
		pose.popPose();
	}

	boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		int line = lineAt(mouseX, mouseY);
		if(line>=0)
			pick(shown().get(line));
		else if(!isOver(mouseX, mouseY))
			closed = true;
		return true;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
	{
		if(isOver(mouseX, mouseY))
			scroll = Math.max(0, scroll-(int)Math.signum(scrollY));
		return true;
	}

	boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode==GLFW.GLFW_KEY_ESCAPE)
			closed = true;
		else if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER)
		{
			if(!shown().isEmpty())
				pick(shown().get(0));
		}
		else
			search.keyPressed(keyCode, scanCode, modifiers);
		return true;
	}

	boolean charTyped(char codePoint, int modifiers)
	{
		return search.charTyped(codePoint, modifiers);
	}

	private void pick(TeletypeSlipPayload.Outlet outlet)
	{
		ECPayloads.sendToServer(new TeletypeOutletPayload(menu.containerId, outlet.pos()));
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
		closed = true;
	}
}
