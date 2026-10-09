package com.muddykat.engineeredcomputation.multiblock.era;

import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.storage.CellInventory;
import com.muddykat.engineeredcomputation.storage.EraDataCell;

public class EraDriveScreen extends AbstractContainerScreen<EraDriveMenu>
{
	private static final int PRIORITY_Y = EraDriveMenu.CELLS_Y+19;
	private static final int PRIORITY_HEIGHT = 10;

	public EraDriveScreen(EraDriveMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = EraDriveMenu.INVENTORY_Y+83;
		this.inventoryLabelY = EraDriveMenu.INVENTORY_Y-11;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		Slot slot = hoveredSlot;
		int priority = priorityAt(mouseX, mouseY);
		if(priority>=0)
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.drive.priority",
							format(CellInventory.priority(menu.slots.get(priority).getItem()))),
					Component.translatable("gui.engineered_computation.drive.priority.detail")
							.withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.drive.priority.help")
							.withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
		else if(slot!=null&&!slot.hasItem()&&menu.getCarried().isEmpty()&&menu.slots.indexOf(slot)<menu.getCellSlotCount())
		{
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.drive.empty_slot"),
					Component.translatable("gui.engineered_computation.drive.accepts", menu.getAcceptedCells())
							.withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		}
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
		for(int i = 0; i<menu.getCellSlotCount(); i++)
		{
			Slot slot = menu.slots.get(i);
			if(!hasPriority(slot))
				continue;
			int x = leftPos+slot.x-1, y = topPos+PRIORITY_Y;
			graphics.fill(x, y, x+18, y+PRIORITY_HEIGHT, priorityAt(mouseX, mouseY)==i? 0xFF2A3A30: 0xFF101814);
			int priority = CellInventory.priority(slot.getItem());
			graphics.drawCenteredString(font, format(priority), x+9, y+1, priority==0? 0xFF8C9890: 0xFFFFD24A);
		}
	}

	private static boolean hasPriority(Slot slot)
	{
		return slot.getItem().getItem() instanceof EraDataCell;
	}

	private static String format(int priority)
	{
		return priority==0? "0": String.format(Locale.ROOT, "%+d", priority);
	}

	private int priorityAt(double mouseX, double mouseY)
	{
		for(int i = 0; i<menu.getCellSlotCount(); i++)
		{
			Slot slot = menu.slots.get(i);
			double x = mouseX-leftPos-(slot.x-1), y = mouseY-topPos-PRIORITY_Y;
			if(hasPriority(slot)&&x>=0&&x<18&&y>=0&&y<PRIORITY_HEIGHT)
				return i;
		}
		return -1;
	}

	private void change(int slot, boolean up)
	{
		int action = hasShiftDown()? up? EraDriveMenu.PRIORITY_UP_TEN: EraDriveMenu.PRIORITY_DOWN_TEN
				: up? EraDriveMenu.PRIORITY_UP: EraDriveMenu.PRIORITY_DOWN;
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, slot*EraDriveMenu.PRIORITY_ACTIONS+action);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		int slot = priorityAt(mouseX, mouseY);
		if(slot>=0&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			change(slot, button==GLFW.GLFW_MOUSE_BUTTON_LEFT);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		int slot = priorityAt(mouseX, mouseY);
		if(slot>=0&&scrollY!=0)
		{
			change(slot, scrollY>0);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
	}
}
