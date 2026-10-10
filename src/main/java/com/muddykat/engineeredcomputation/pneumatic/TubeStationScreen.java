package com.muddykat.engineeredcomputation.pneumatic;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;

public class TubeStationScreen extends NamedStationScreen<TubeStationMenu>
{
	private static final int NEW_ITEMS_Y = 38;
	private static final int STATUS_Y = 60;
	private static final int TOOLTIP_WIDTH = 200;

	public TubeStationScreen(TubeStationMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageHeight = 76;
	}

	@Override
	protected Component unnamed()
	{
		return menu.getInventoryName();
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		super.renderBg(pose, partialTick, mouseX, mouseY);
		renderSwitch(pose, NEW_ITEMS_Y, new TranslatableComponent("gui.engineered_computation.station.new_items"),
				menu.takesNewItems(), mouseX, mouseY);
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.station.waiting",
				menu.getPendingRequests()), leftPos+8, topPos+STATUS_Y, 0x707070);
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		super.render(pose, mouseX, mouseY, partialTick);
		if(isOverSwitch(NEW_ITEMS_Y, mouseX, mouseY))
			renderTooltip(pose, font.split(new TranslatableComponent(
					"gui.engineered_computation.station.new_items.detail").withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH),
					mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&clickSwitch(NEW_ITEMS_Y, mouseX, mouseY, TubeStationMenu.TOGGLE_NEW_ITEMS))
			return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
