package com.muddykat.engineeredcomputation.client.screen.pneumatic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerMenu;

public class PressurizerScreen extends AbstractContainerScreen<PressurizerMenu>
{
	private static final int GAUGE_X = 70;
	private static final int GAUGE_Y = 38;
	private static final int GAUGE_WIDTH = 98;
	private static final int GAUGE_HEIGHT = 8;
	private static final int GAUGE = 0xFFE07A3A;

	public PressurizerScreen(PressurizerMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageHeight = PressurizerMenu.INVENTORY_Y+58+18+6;
		this.inventoryLabelY = PressurizerMenu.INVENTORY_Y-11;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
		boolean pressure = menu.getCharge()>0||menu.hasFuel();
		graphics.drawString(font, Component.translatable(pressure? "gui.engineered_computation.pressurizer.high"
				: "gui.engineered_computation.pressurizer.low"), leftPos+GAUGE_X, topPos+GAUGE_Y-12,
				pressure? 0x2E7D32: 0xB23A2A, false);
		ECGui.gauge(graphics, leftPos+GAUGE_X, topPos+GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
				(double)menu.getCharge()/menu.getChargePerFuel(), GAUGE);
		graphics.drawString(font, Component.translatable("gui.engineered_computation.pressurizer.charge",
				menu.getCharge()), leftPos+GAUGE_X, topPos+GAUGE_Y+GAUGE_HEIGHT+4, 0x707070, false);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		if(hoveredSlot!=null&&hoveredSlot.index==0&&!hoveredSlot.hasItem())
			graphics.renderTooltip(font, Component.translatable("gui.engineered_computation.pressurizer.fuel"), mouseX,
					mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}
}
