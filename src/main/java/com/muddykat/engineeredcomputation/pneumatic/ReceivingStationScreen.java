package com.muddykat.engineeredcomputation.pneumatic;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;

public class ReceivingStationScreen extends NamedStationScreen<ReceivingStationMenu>
{
	private static final int EJECT_Y = 36;
	private static final int TOOLTIP_WIDTH = 200;

	public ReceivingStationScreen(ReceivingStationMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageHeight = ReceivingStationMenu.INVENTORY_Y+83;
		this.inventoryLabelY = ReceivingStationMenu.INVENTORY_Y-11;
	}

	@Override
	protected Component unnamed()
	{
		return title;
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		super.renderBg(pose, partialTick, mouseX, mouseY);
		renderSwitch(pose, EJECT_Y, new TranslatableComponent("gui.engineered_computation.receiver.eject"),
				menu.isEjecting(), mouseX, mouseY);
		for(Slot slot : menu.slots)
			ECGui.slot(pose, leftPos+slot.x-1, topPos+slot.y-1);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY)
	{
		super.renderLabels(pose, mouseX, mouseY);
		font.draw(pose, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL);
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		super.render(pose, mouseX, mouseY, partialTick);
		if(isOverSwitch(EJECT_Y, mouseX, mouseY))
			renderTooltip(pose, font.split(new TranslatableComponent(
					"gui.engineered_computation.receiver.eject.detail").withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH),
					mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&clickSwitch(EJECT_Y, mouseX, mouseY, ReceivingStationMenu.TOGGLE_EJECT))
			return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
