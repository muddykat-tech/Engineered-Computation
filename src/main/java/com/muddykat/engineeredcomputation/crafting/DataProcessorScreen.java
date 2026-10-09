package com.muddykat.engineeredcomputation.crafting;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.StatusCode;

public class DataProcessorScreen extends AbstractContainerScreen<DataProcessorMenu>
{
	private static final int STATUS_WIDTH = 120;
	private static final int STATUS_HEIGHT = 14;

	public DataProcessorScreen(DataProcessorMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = DataProcessorMenu.INVENTORY_Y+83;
		this.inventoryLabelY = DataProcessorMenu.INVENTORY_Y-11;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		Slot slot = hoveredSlot;
		StatusCode status = menu.status();
		if(overStatus(mouseX, mouseY)&&status!=null)
			graphics.renderComponentTooltip(font, List.of(status.title(),
					Component.translatable("gui.engineered_computation.processor.status."+status.code())
							.withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.processor.sides")
							.withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
		else if(slot!=null&&!slot.hasItem()&&menu.getCarried().isEmpty()
				&&menu.slots.indexOf(slot)<DataProcessorState.PATTERN_SLOTS)
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.processor.empty_slot"),
					Component.translatable("gui.engineered_computation.processor.accepts")
							.withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
		int x = leftPos+(imageWidth-STATUS_WIDTH)/2, y = topPos+DataProcessorMenu.STATUS_Y;
		graphics.fill(x, y, x+STATUS_WIDTH, y+STATUS_HEIGHT, 0xFF101814);
		StatusCode status = menu.status();
		if(status!=null)
			graphics.drawCenteredString(font, ECGui.fit(font, status.title(), STATUS_WIDTH-6), x+STATUS_WIDTH/2, y+3,
					status.colour());
	}

	private boolean overStatus(double mouseX, double mouseY)
	{
		double x = mouseX-leftPos-(imageWidth-STATUS_WIDTH)/2.0, y = mouseY-topPos-DataProcessorMenu.STATUS_Y;
		return x>=0&&x<STATUS_WIDTH&&y>=0&&y<STATUS_HEIGHT;
	}
}
