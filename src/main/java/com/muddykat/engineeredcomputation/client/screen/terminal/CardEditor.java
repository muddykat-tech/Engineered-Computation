package com.muddykat.engineeredcomputation.client.screen.terminal;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.FilterTextField;
import com.muddykat.engineeredcomputation.client.gui.GuiIcon;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeMenu;

final class CardEditor
{
	static final int WIDTH = 175;
	private static final int TITLE_HEIGHT = 14;
	static final int HEIGHT = TITLE_HEIGHT+26;
	private static final int ENTRY_Y = TITLE_HEIGHT+3;
	private static final int CLOSE_SIZE = 11;


	private final TeletypeMenu menu;
	private final Font font;
	private final FilterTextField field;
	private int x;
	private int y;
	private boolean dragging;
	private int dragX;
	private int dragY;
	private boolean closed;
	private ProgramEntry shown;

	CardEditor(TeletypeMenu menu, Font font, int x, int y)
	{
		this.menu = menu;
		this.font = font;
		field = new FilterTextField(font, 0, 0, WIDTH-32, this::send);
		moveTo(x, y);
		shown = menu.cardFilter();
		field.show(shown);
	}

	boolean isClosed()
	{
		return closed;
	}

	private ItemStack card()
	{
		return menu.getSlot(TeletypeMenu.CARD_SLOT).getItem();
	}

	void tick()
	{
		if(!(card().getItem() instanceof FilterCardItem))
			closed = true;
		else if(!field.isFocused()&&!menu.cardFilter().equals(shown))
		{
			shown = menu.cardFilter();
			field.show(shown);
		}
	}

	private void moveTo(int newX, int newY)
	{
		Minecraft minecraft = Minecraft.getInstance();
		x = Math.clamp(newX, 0, Math.max(0, minecraft.getWindow().getGuiScaledWidth()-WIDTH));
		y = Math.clamp(newY, 0, Math.max(0, minecraft.getWindow().getGuiScaledHeight()-HEIGHT));
		field.setPosition(x+27, y+ENTRY_Y+(18-FilterTextField.HEIGHT)/2);
	}

	private void send(ProgramEditPayload.Action action, String text)
	{
		PacketDistributor.sendToServer(new ProgramEditPayload(menu.containerId, 0, action, text));
	}

	boolean isOver(double mouseX, double mouseY)
	{
		return mouseX>=x&&mouseX<x+WIDTH&&mouseY>=y&&mouseY<y+HEIGHT||field.isOver(mouseX, mouseY);
	}

	Rect2i area()
	{
		return new Rect2i(x, y, WIDTH, HEIGHT);
	}

	private boolean isOverClose(double mouseX, double mouseY)
	{
		int closeX = x+WIDTH-CLOSE_SIZE-4;
		return mouseX>=closeX&&mouseX<closeX+CLOSE_SIZE&&mouseY>=y+3&&mouseY<y+3+CLOSE_SIZE;
	}

	private boolean isOverTitle(double mouseX, double mouseY)
	{
		return mouseX>=x&&mouseX<x+WIDTH&&mouseY>=y&&mouseY<y+TITLE_HEIGHT;
	}

	private boolean isOverEntry(double mouseX, double mouseY)
	{
		return mouseX>=x+5&&mouseX<x+23&&mouseY>=y+ENTRY_Y&&mouseY<y+ENTRY_Y+18;
	}

	void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		ECGui.panel(graphics, x, y, WIDTH, HEIGHT);
		graphics.drawString(font, ECGui.fit(font, card().getHoverName(), WIDTH-CLOSE_SIZE-14), x+6, y+4, ECGui.LABEL,
				false);
		ECGui.button(graphics, x+WIDTH-CLOSE_SIZE-4, y+3, CLOSE_SIZE, CLOSE_SIZE, isOverClose(mouseX, mouseY));
		GuiIcon.CROSS.draw(graphics, x+WIDTH-CLOSE_SIZE-1, y+6);
		int entryX = x+5, entryY = y+ENTRY_Y;
		ECGui.slot(graphics, entryX, entryY);
		graphics.renderItem(ECGui.entryIcon(menu.cardFilter()), entryX+1, entryY+1);
		if(isOverEntry(mouseX, mouseY))
			graphics.fill(entryX+1, entryY+1, entryX+17, entryY+17, 0x80FFFFFF);
		field.render(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popPose();
	}

	List<Component> tooltip(double mouseX, double mouseY)
	{
		if(isOverEntry(mouseX, mouseY)&&menu.getCarried().isEmpty())
			return FilterTextField.entryTooltip(menu.cardFilter());
		if(isOverClose(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.card_editor.close"));
		return field.tooltip(mouseX, mouseY);
	}

	boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(field.mouseClicked(mouseX, mouseY, button))
			return true;
		if(!isOver(mouseX, mouseY))
			return false;
		if(isOverClose(mouseX, mouseY)&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			closed = true;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
		}
		else if(isOverTitle(mouseX, mouseY)&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			dragging = true;
			dragX = (int)mouseX-x;
			dragY = (int)mouseY-y;
		}
		else if(isOverEntry(mouseX, mouseY))
		{
			if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
			{
				send(ProgramEditPayload.Action.CLEAR, "");
				field.clear();
			}
			else if(!menu.getCarried().isEmpty())
				send(ProgramEditPayload.Action.SET_ITEM, "");
			else
				field.setFocused(true);
		}
		return true;
	}

	boolean mouseDragged(double mouseX, double mouseY)
	{
		if(!dragging)
			return false;
		moveTo((int)mouseX-dragX, (int)mouseY-dragY);
		return true;
	}

	boolean mouseReleased()
	{
		boolean was = dragging;
		dragging = false;
		return was;
	}

	boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(field.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if(keyCode==GLFW.GLFW_KEY_ESCAPE)
		{
			closed = true;
			return true;
		}
		return false;
	}

	boolean charTyped(char codePoint, int modifiers)
	{
		return field.charTyped(codePoint, modifiers);
	}
}
