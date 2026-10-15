package com.muddykat.engineeredcomputation.client.screen.remote;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerMenu;

public class StorageAnalyzerScreen extends AbstractContainerScreen<StorageAnalyzerMenu>
{
	private static final int ROW_Y = 20;
	private static final int BUTTON_WIDTH = 92;
	private static final int BUTTON_HEIGHT = 14;
	private static final int STATUS_Y = 40;
	private static final int TOOLTIP_WIDTH = 200;

	public StorageAnalyzerScreen(StorageAnalyzerMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 196;
		this.imageHeight = 54;
	}

	private static String key(StorageAnalyzerBlockEntity.Priority priority)
	{
		return "gui.engineered_computation.analyzer.priority."+priority.name().toLowerCase(Locale.ROOT);
	}

	private int buttonX()
	{
		return leftPos+imageWidth-8-BUTTON_WIDTH;
	}

	private boolean isOverButton(double mouseX, double mouseY)
	{
		double x = mouseX-buttonX(), y = mouseY-topPos-ROW_Y;
		return x>=0&&x<BUTTON_WIDTH&&y>=0&&y<BUTTON_HEIGHT;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		graphics.drawString(font, Component.translatable("gui.engineered_computation.analyzer.priority"), leftPos+8,
				topPos+ROW_Y+3, ECGui.LABEL, false);
		ECGui.button(graphics, buttonX(), topPos+ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT, isOverButton(mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable(key(menu.getPriority())), buttonX()+BUTTON_WIDTH/2+1,
				topPos+ROW_Y+3, 0xFFFFFF);
		int facing = menu.facing();
		graphics.drawString(font, Component.translatable(facing==StorageAnalyzerMenu.PNEUMATIC
				? "gui.engineered_computation.analyzer.pneumatic": facing==StorageAnalyzerMenu.INVENTORY
				? "gui.engineered_computation.analyzer.inventory": "gui.engineered_computation.analyzer.nothing"),
				leftPos+8, topPos+STATUS_Y, facing==StorageAnalyzerMenu.NOTHING? 0xC03A2C: ECGui.LABEL, false);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		if(isOverButton(mouseX, mouseY))
			graphics.renderTooltip(font, font.split(Component.translatable(key(menu.getPriority())+".detail")
					.withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH), mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverButton(mouseX, mouseY))
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StorageAnalyzerMenu.CYCLE_PRIORITY);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
