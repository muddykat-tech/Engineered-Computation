package com.muddykat.engineeredcomputation.pneumatic;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;

public class TubeStationScreen extends NamedStationScreen<TubeStationMenu>
{
	private static final int NEW_ITEMS_Y = 38;
	private static final int STATUS_Y = 60;
	private static final int FILTER_STATUS_Y = 80;
	private static final int MODE_X = TubeStationMenu.FILTER_X-ECGui.FILTER_MODE_SIZE-3;
	private static final int MODE_Y = TubeStationMenu.FILTER_Y+(18-ECGui.FILTER_MODE_SIZE)/2;
	private static final int TOOLTIP_WIDTH = 200;

	public TubeStationScreen(TubeStationMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageHeight = menu.filters()? TubeStationMenu.INVENTORY_Y+58+18+6: 76;
		this.inventoryLabelY = TubeStationMenu.INVENTORY_Y-11;
	}

	@Override
	protected Component unnamed()
	{
		return menu.getInventoryName();
	}

	/** Whether a card decides what's deposited here, in place of the new items switch. */
	private boolean filtered()
	{
		return menu.filters()&&StationFilter.isCard(menu.getFilterCard());
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		super.renderBg(pose, partialTick, mouseX, mouseY);
		Component newItems = new TranslatableComponent("gui.engineered_computation.station.new_items");
		if(filtered())
			font.draw(pose, ECGui.fit(font, new TranslatableComponent("gui.engineered_computation.station.by_card"),
					imageWidth-16), leftPos+8, topPos+NEW_ITEMS_Y+3, 0x707070);
		else
			renderSwitch(pose, NEW_ITEMS_Y, newItems, menu.takesNewItems(), mouseX, mouseY);
		if(menu.filters())
		{
			for(Slot slot : menu.slots)
				ECGui.slot(pose, leftPos+slot.x-1, topPos+slot.y-1);
			font.draw(pose, new TranslatableComponent("gui.engineered_computation.station.filter"), leftPos+8,
					topPos+TubeStationMenu.FILTER_Y+5, ECGui.LABEL);
			ECGui.filterMode(pose, leftPos+MODE_X, topPos+MODE_Y, menu.isBlocking(), isOverMode(mouseX, mouseY));
		}
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.station.waiting",
				menu.getPendingRequests()), leftPos+8, topPos+(menu.filters()? FILTER_STATUS_Y: STATUS_Y), 0x707070);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY)
	{
		super.renderLabels(pose, mouseX, mouseY);
		if(menu.filters())
			font.draw(pose, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL);
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		super.render(pose, mouseX, mouseY, partialTick);
		List<Component> lines = new ArrayList<>();
		if(filtered()&&isOver(mouseX, mouseY, 8, NEW_ITEMS_Y, imageWidth-16, 14))
			lines.add(new TranslatableComponent("gui.engineered_computation.station.by_card.detail")
					.withStyle(ChatFormatting.GRAY));
		else if(!filtered()&&isOverSwitch(NEW_ITEMS_Y, mouseX, mouseY))
			lines.add(new TranslatableComponent("gui.engineered_computation.station.new_items.detail")
					.withStyle(ChatFormatting.GRAY));
		else if(menu.filters()&&isOverMode(mouseX, mouseY))
		{
			String mode = menu.isBlocking()? "block": "allow";
			lines.add(new TranslatableComponent("gui.engineered_computation.adapter.mode."+mode)
					.withStyle(menu.isBlocking()? ChatFormatting.RED: ChatFormatting.GREEN));
			lines.add(new TranslatableComponent("gui.engineered_computation.station.mode."+mode)
					.withStyle(ChatFormatting.GRAY));
			lines.add(new TranslatableComponent(menu.isBlocking()? "gui.engineered_computation.adapter.mode.to_allow"
					: "gui.engineered_computation.adapter.mode.to_block").withStyle(ChatFormatting.DARK_GRAY));
		}
		else if(menu.filters()&&menu.getFilterCard().isEmpty()&&menu.getCarried().isEmpty()
				&&isOver(mouseX, mouseY, TubeStationMenu.FILTER_X, TubeStationMenu.FILTER_Y, 18, 18))
			lines.add(new TranslatableComponent("gui.engineered_computation.station.filter.empty")
					.withStyle(ChatFormatting.GRAY));
		if(lines.isEmpty())
			return;
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for(Component line : lines)
			wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
		renderTooltip(pose, wrapped, mouseX, mouseY);
	}

	private boolean isOverMode(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, MODE_X, MODE_Y, ECGui.FILTER_MODE_SIZE, ECGui.FILTER_MODE_SIZE);
	}

	private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height)
	{
		double relX = mouseX-leftPos-x, relY = mouseY-topPos-y;
		return relX>=0&&relY>=0&&relX<width&&relY<height;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&!filtered()
				&&clickSwitch(NEW_ITEMS_Y, mouseX, mouseY, TubeStationMenu.TOGGLE_NEW_ITEMS))
			return true;
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&menu.filters()&&isOverMode(mouseX, mouseY))
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TubeStationMenu.TOGGLE_FILTER_MODE);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
