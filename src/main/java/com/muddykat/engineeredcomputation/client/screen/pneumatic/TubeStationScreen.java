package com.muddykat.engineeredcomputation.client.screen.pneumatic;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.pneumatic.station.StationFilter;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationMenu;

public class TubeStationScreen extends NamedStationScreen<TubeStationMenu>
{
	private static final int NEW_ITEMS_Y = 38;
	private static final int STATUS_Y = 60;
	private static final int INSERT_ONLY_Y = 78;
	private static final int FILTER_STATUS_Y = 98;
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

	private boolean filtered()
	{
		return menu.filters()&&StationFilter.isCard(menu.getFilterCard());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		super.renderBg(graphics, partialTick, mouseX, mouseY);
		Component newItems = Component.translatable("gui.engineered_computation.station.new_items");
		if(filtered())
			graphics.drawString(font, ECGui.fit(font,
					Component.translatable("gui.engineered_computation.station.by_card"), imageWidth-16), leftPos+8,
					topPos+NEW_ITEMS_Y+3, 0x707070, false);
		else
			renderSwitch(graphics, NEW_ITEMS_Y, newItems, menu.takesNewItems(), mouseX, mouseY);
		if(menu.filters())
		{
			for(Slot slot : menu.slots)
				ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
			graphics.drawString(font, Component.translatable("gui.engineered_computation.station.filter"), leftPos+8,
					topPos+TubeStationMenu.FILTER_Y+5, ECGui.LABEL, false);
			ECGui.filterMode(graphics, leftPos+MODE_X, topPos+MODE_Y, menu.isBlocking(), isOverMode(mouseX, mouseY));
			renderSwitch(graphics, INSERT_ONLY_Y, Component.translatable("gui.engineered_computation.station.insert_only"),
					menu.isInsertOnly(), mouseX, mouseY);
		}
		graphics.drawString(font, Component.translatable("gui.engineered_computation.station.waiting",
				menu.getPendingRequests()), leftPos+8, topPos+(menu.filters()? FILTER_STATUS_Y: STATUS_Y), 0x707070,
				false);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		super.renderLabels(graphics, mouseX, mouseY);
		if(menu.filters())
			graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		List<Component> lines = new ArrayList<>();
		if(filtered()&&isOver(mouseX, mouseY, 8, NEW_ITEMS_Y, imageWidth-16, 14))
			lines.add(Component.translatable("gui.engineered_computation.station.by_card.detail")
					.withStyle(ChatFormatting.GRAY));
		else if(!filtered()&&isOverSwitch(NEW_ITEMS_Y, mouseX, mouseY))
			lines.add(Component.translatable("gui.engineered_computation.station.new_items.detail")
					.withStyle(ChatFormatting.GRAY));
		else if(menu.filters()&&isOverSwitch(INSERT_ONLY_Y, mouseX, mouseY))
			lines.add(Component.translatable("gui.engineered_computation.station.insert_only.detail")
					.withStyle(ChatFormatting.GRAY));
		else if(menu.filters()&&isOverMode(mouseX, mouseY))
		{
			String mode = menu.isBlocking()? "block": "allow";
			lines.add(Component.translatable("gui.engineered_computation.adapter.mode."+mode)
					.withStyle(menu.isBlocking()? ChatFormatting.RED: ChatFormatting.GREEN));
			lines.add(Component.translatable("gui.engineered_computation.station.mode."+mode)
					.withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable(menu.isBlocking()? "gui.engineered_computation.adapter.mode.to_allow"
					: "gui.engineered_computation.adapter.mode.to_block").withStyle(ChatFormatting.DARK_GRAY));
		}
		else if(menu.filters()&&menu.getFilterCard().isEmpty()&&menu.getCarried().isEmpty()
				&&isOver(mouseX, mouseY, TubeStationMenu.FILTER_X, TubeStationMenu.FILTER_Y, 18, 18))
			lines.add(Component.translatable("gui.engineered_computation.station.filter.empty")
					.withStyle(ChatFormatting.GRAY));
		if(lines.isEmpty())
			return;
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for(Component line : lines)
			wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
		graphics.renderTooltip(font, wrapped, mouseX, mouseY);
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
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&menu.filters()
				&&clickSwitch(INSERT_ONLY_Y, mouseX, mouseY, TubeStationMenu.TOGGLE_INSERT_ONLY))
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
