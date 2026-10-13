package com.muddykat.engineeredcomputation.client.screen.catalogue;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.catalogue.deck.CardDeckMenu;
import com.muddykat.engineeredcomputation.client.gui.ECGui;

public class CardDeckScreen extends AbstractContainerScreen<CardDeckMenu>
{
	public CardDeckScreen(CardDeckMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = CardDeckMenu.inventoryY(menu.getCapacity())+83;
		this.inventoryLabelY = CardDeckMenu.inventoryY(menu.getCapacity())-11;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
	}
}
