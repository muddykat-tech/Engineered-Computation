package com.muddykat.engineeredcomputation.catalogue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

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
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		renderTooltip(pose, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(pose, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(pose, leftPos+slot.x-1, topPos+slot.y-1);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY)
	{
		font.draw(pose, title, titleLabelX, titleLabelY, ECGui.LABEL);
		font.draw(pose, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL);
	}
}
