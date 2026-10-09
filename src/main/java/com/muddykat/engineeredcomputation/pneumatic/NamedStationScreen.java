package com.muddykat.engineeredcomputation.pneumatic;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.ECPayloads;

public abstract class NamedStationScreen<M extends AbstractContainerMenu&NamedStationMenu>
		extends AbstractContainerScreen<M>
{
	protected static final int NAME_Y = 18;
	private static final int NAME_HEIGHT = 13;
	private static final int SWITCH_WIDTH = 24;
	private static final int SWITCH_HEIGHT = 14;

	private EditBox nameField;

	protected NamedStationScreen(M menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
	}

	protected abstract Component unnamed();

	@Override
	protected void init()
	{
		super.init();
		nameField = new EditBox(font, leftPos+11, topPos+NAME_Y+3, imageWidth-22, 10,
				Component.translatable("gui.engineered_computation.station.name"));
		nameField.setBordered(false);
		nameField.setMaxLength(TubeStationBlockEntity.MAX_NAME);
		nameField.setTextColor(0xFFFFFF);
		nameField.setHint(unnamed().copy().withStyle(style -> style.withColor(0x707070)));
		nameField.setValue(menu.getName());
		nameField.setResponder(name -> ECPayloads.sendToServer(new StationNamePayload(menu.containerId, name)));
		addRenderableWidget(nameField);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		int x = leftPos+8;
		graphics.fill(x, topPos+NAME_Y, x+imageWidth-16, topPos+NAME_Y+NAME_HEIGHT, 0xFFA0A0A0);
		graphics.fill(x+1, topPos+NAME_Y+1, x+imageWidth-17, topPos+NAME_Y+NAME_HEIGHT-1, 0xFF000000);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
	}

	protected void renderSwitch(GuiGraphics graphics, int rowY, Component label, boolean on, int mouseX, int mouseY)
	{
		graphics.drawString(font, label, leftPos+8, topPos+rowY+3, ECGui.LABEL, false);
		int x = leftPos+switchX();
		ECGui.button(graphics, x, topPos+rowY, SWITCH_WIDTH, SWITCH_HEIGHT, isOverSwitch(rowY, mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable(on? "gui.engineered_computation.adapter.on"
				: "gui.engineered_computation.adapter.off"), x+SWITCH_WIDTH/2+1, topPos+rowY+3, on? 0x6CFF7C: 0xFF6A5A);
	}

	private int switchX()
	{
		return imageWidth-8-SWITCH_WIDTH;
	}

	protected boolean isOverSwitch(int rowY, double mouseX, double mouseY)
	{
		double x = mouseX-leftPos-switchX(), y = mouseY-topPos-rowY;
		return x>=0&&x<SWITCH_WIDTH&&y>=0&&y<SWITCH_HEIGHT;
	}

	protected boolean clickSwitch(int rowY, double mouseX, double mouseY, int button)
	{
		if(!isOverSwitch(rowY, mouseX, mouseY))
			return false;
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(nameField.isFocused()&&keyCode!=GLFW.GLFW_KEY_ESCAPE)
		{
			if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER)
				nameField.setFocused(false);
			else
				nameField.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(!nameField.isMouseOver(mouseX, mouseY))
			nameField.setFocused(false);
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
