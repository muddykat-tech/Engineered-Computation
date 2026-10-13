package com.muddykat.engineeredcomputation.client.screen.program;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.FilterTextField;
import com.muddykat.engineeredcomputation.program.Program;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.program.ProgramFloppyMenu;

public class ProgramFloppyScreen extends AbstractContainerScreen<ProgramFloppyMenu>
{
	private static final int TAG_Y = 44;
	private static final int FIELD_X = 7;

	private FilterTextField field;
	private int selected = 0;
	/** The item's contents can arrive after the screen opens, so this keeps track of what the field last showed. */
	private ProgramEntry shownEntry = ProgramEntry.EMPTY;

	public ProgramFloppyScreen(ProgramFloppyMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = ProgramFloppyMenu.INVENTORY_Y+58+18+6;
		this.inventoryLabelY = ProgramFloppyMenu.INVENTORY_Y-11;
	}

	@Override
	protected void init()
	{
		super.init();
		field = new FilterTextField(font, leftPos+FIELD_X, topPos+TAG_Y, imageWidth-FIELD_X-8, this::send);
		select(selected);
	}

	private void select(int index)
	{
		selected = index;
		shownEntry = menu.program().get(index);
		field.show(shownEntry);
	}

	@Override
	protected void containerTick()
	{
		super.containerTick();
		if(!field.isFocused()&&!menu.program().get(selected).equals(shownEntry))
			select(selected);
	}

	private void send(ProgramEditPayload.Action action, String text)
	{
		PacketDistributor.sendToServer(new ProgramEditPayload(menu.containerId, selected, action, text));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		field.render(graphics, mouseX, mouseY, partialTick);
		int entry = entryAt(mouseX, mouseY);
		List<Component> modeTooltip = field.tooltip(mouseX, mouseY);
		if(entry>=0&&menu.getCarried().isEmpty())
			graphics.renderComponentTooltip(font, FilterTextField.entryTooltip(menu.program().get(entry)), mouseX,
					mouseY);
		else if(!modeTooltip.isEmpty())
			graphics.renderComponentTooltip(font, modeTooltip, mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(graphics, x, y, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			if(slot.isActive())
				ECGui.slot(graphics, x+slot.x-1, y+slot.y-1);
		Program program = menu.program();
		for(int i = 0; i<menu.size(); i++)
		{
			int entryX = x+ProgramFloppyMenu.ENTRIES_X+i*18, entryY = y+ProgramFloppyMenu.ENTRIES_Y;
			ECGui.slot(graphics, entryX, entryY);
			ItemStack shown = ECGui.entryIcon(program.get(i));
			graphics.renderItem(shown, entryX+1, entryY+1);
			if(program.get(i).tag().isPresent())
				graphics.drawString(font, "#", entryX+12, entryY+10, 0x55FFFF, true);
			if(i==selected)
				graphics.renderOutline(entryX, entryY, 18, 18, 0xFFFFFFFF);
			if(i==entryAt(mouseX, mouseY))
				graphics.fill(entryX+1, entryY+1, entryX+17, entryY+17, 0x80FFFFFF);
		}
	}

	public Rect2i entryArea(int index)
	{
		return new Rect2i(leftPos+ProgramFloppyMenu.ENTRIES_X+index*18, topPos+ProgramFloppyMenu.ENTRIES_Y, 18, 18);
	}

	private int entryAt(double mouseX, double mouseY)
	{
		double relX = mouseX-leftPos-ProgramFloppyMenu.ENTRIES_X, relY = mouseY-topPos-ProgramFloppyMenu.ENTRIES_Y;
		if(relX<0||relY<0||relY>=18||relX>=menu.size()*18)
			return -1;
		return (int)(relX/18);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(field.mouseClicked(mouseX, mouseY, button))
			return true;
		int entry = entryAt(mouseX, mouseY);
		if(entry>=0)
		{
			selected = entry;
			if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
			{
				send(ProgramEditPayload.Action.CLEAR, "");
				field.clear();
			}
			else if(!menu.getCarried().isEmpty())
			{
				send(ProgramEditPayload.Action.SET_ITEM, "");
				field.clear();
			}
			else
			{
				select(entry);
				field.setFocused(true);
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(field.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		return field.charTyped(codePoint, modifiers)||super.charTyped(codePoint, modifiers);
	}
}
