package com.muddykat.engineeredcomputation.client.screen.terminal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.GuiIcon;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;

public class VideoTerminalScreen<M extends VideoTerminalMenu> extends TerminalScreen<M>
{
	private static final int RESULT_SIZE = 26;
	private static final int LINK_SIZE = 16;
	private static final int CLEAR_SIZE = 11;
	private static final int MISSING = 0xA08B8B8B;

	@Nullable
	private Set<Integer> linkDrag;

	public VideoTerminalScreen(M menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
	}

	@Override
	protected void renderSlot(GuiGraphics graphics, Slot slot)
	{
		super.renderSlot(graphics, slot);
		if(isMissing(slot))
			graphics.fill(RenderType.guiOverlay(), slot.x, slot.y, slot.x+16, slot.y+16, MISSING);
	}

	private boolean isMissing(Slot slot)
	{
		if(!menu.isLinked()||!slot.hasItem())
			return false;
		if(menu.isGridSlot(slot))
			return menu.storedOnClient(slot.getItem())==0;
		return slot.index==VideoTerminalMenu.RESULT_SLOT&&!menu.hasLinkedIngredients();
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack)
	{
		List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if(hoveredSlot!=null&&menu.isLinked()&&menu.isGridSlot(hoveredSlot))
			lines.add(Component.translatable("gui.engineered_computation.terminal.linked.stored",
					number(menu.storedOnClient(stack))).withStyle(ChatFormatting.GOLD));
		else if(hoveredSlot!=null&&isMissing(hoveredSlot))
			lines.add(Component.translatable("gui.engineered_computation.terminal.linked.missing")
					.withStyle(ChatFormatting.RED));
		return lines;
	}

	@Nullable
	@Override
	protected List<Component> widgetTooltip(double mouseX, double mouseY)
	{
		if(isOverClear(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.terminal.clear")
					.withStyle(ChatFormatting.YELLOW));
		if(isOverLink(mouseX, mouseY))
		{
			String key = "gui.engineered_computation.terminal.linked."+(menu.isLinked()? "on": "off");
			return List.of(Component.translatable(key).withStyle(ChatFormatting.YELLOW),
					Component.translatable(key+".detail").withStyle(ChatFormatting.GRAY));
		}
		return super.widgetTooltip(mouseX, mouseY);
	}

	@Override
	protected void renderSlotBackground(GuiGraphics graphics, Slot slot, int index)
	{
		if(index==VideoTerminalMenu.RESULT_SLOT)
			ECGui.sunken(graphics, leftPos+VideoTerminalMenu.RESULT_X, topPos+slot.y-5, RESULT_SIZE, RESULT_SIZE);
		else
			super.renderSlotBackground(graphics, slot, index);
	}

	@Override
	protected void renderExtras(GuiGraphics graphics, int mouseX, int mouseY)
	{
		int craftingY = topPos+layout.craftingY();
		int arrowX = leftPos+VideoTerminalMenu.CRAFT_X+3*18+6;
		int arrowY = craftingY+26;
		graphics.fill(arrowX, arrowY-2, arrowX+15, arrowY+2, 0xFF8B8B8B);
		for(int step = 0; step<7; step++)
			graphics.fill(arrowX+15+step, arrowY-7+step, arrowX+16+step, arrowY+7-step, 0xFF8B8B8B);

		int clearX = leftPos+VideoTerminalMenu.CLEAR_X;
		ECGui.button(graphics, clearX, craftingY, CLEAR_SIZE, CLEAR_SIZE, isOverClear(mouseX, mouseY));
		GuiIcon.CROSS.draw(graphics, clearX+3, craftingY+3);

		int toggleX = leftPos+VideoTerminalMenu.LINK_X;
		int toggleY = craftingY+linkY();
		ECGui.button(graphics, toggleX, toggleY, LINK_SIZE, LINK_SIZE, isOverLink(mouseX, mouseY));
		(menu.isLinked()? GuiIcon.LINK_ON: GuiIcon.LINK_OFF).draw(graphics, toggleX+4, toggleY+4);
	}

	private static int linkY()
	{
		return 27-LINK_SIZE/2;
	}

	private boolean isOverLink(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, VideoTerminalMenu.LINK_X, layout.craftingY()+linkY(), LINK_SIZE, LINK_SIZE);
	}

	private boolean isOverClear(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, VideoTerminalMenu.CLEAR_X, layout.craftingY(), CLEAR_SIZE, CLEAR_SIZE);
	}

	@Nullable
	private Slot linkedGridSlotAt(double mouseX, double mouseY)
	{
		if(!menu.isLinked())
			return null;
		for(Slot slot : menu.slots)
			if(menu.isGridSlot(slot)&&isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY))
				return slot;
		return null;
	}

	private void link(Slot slot)
	{
		if(linkDrag.add(slot.index))
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.PICKUP,
					minecraft.player);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		Slot gridSlot = linkedGridSlotAt(mouseX, mouseY);
		if(gridSlot!=null&&!menu.getCarried().isEmpty()
				&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			linkDrag = new HashSet<>();
			link(gridSlot);
			return true;
		}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&(isOverLink(mouseX, mouseY)||isOverClear(mouseX, mouseY)))
		{
			unfocusSearch();
			pressButton(isOverLink(mouseX, mouseY)? VideoTerminalMenu.TOGGLE_LINKED: VideoTerminalMenu.CLEAR_GRID);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(linkDrag!=null)
		{
			Slot gridSlot = linkedGridSlotAt(mouseX, mouseY);
			if(gridSlot!=null&&!menu.getCarried().isEmpty())
				link(gridSlot);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(linkDrag!=null)
		{
			linkDrag = null;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
}
