package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public class CraftingComputerScreen extends AbstractContainerScreen<CraftingComputerMenu>
{
	private static final int SCREEN = 0xFF101814;
	private static final int RULE = 0xFF3E5246;
	private static final int TEXT = 0xFFD8E0D8;
	private static final int DIM = 0xFF8C9890;
	private static final int DIRECT = 0xFFFFD24A;
	private static final int MACHINE = 0xFFFF9A50;
	private static final int STORED = 0xFF8CC890;
	private static final int DONE = 0xFF55FF77;
	private static final float SMALL = 0.75F;
	private static final int LINE_HEIGHT = 7;
	private static final int TREE_Y = 28;
	private static final int INDENT = 5;
	private static final int ARROW_SIZE = 7;
	private static final int PICKER_Y = 14;

	private int selected;
	private int scroll;

	public CraftingComputerScreen(CraftingComputerMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = CraftingComputerMenu.INVENTORY_Y+58+18+6;
		this.inventoryLabelY = CraftingComputerMenu.INVENTORY_Y-11;
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		if(hoveredSlot!=null&&hoveredSlot.index<CraftingComputerState.PATTERN_SLOTS&&!hoveredSlot.hasItem())
		{
			renderComponentTooltip(pose, List.of(
					new TranslatableComponent("gui.engineered_computation.crafting_computer.bay", hoveredSlot.index+1),
					new TranslatableComponent("gui.engineered_computation.crafting_computer.bay.detail")
							.withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			return;
		}
		ItemKey line = lineAt(mouseX, mouseY);
		if(line!=null)
			renderTooltip(pose, line.displayStack(), mouseX, mouseY);
		else
			renderTooltip(pose, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(pose, leftPos, topPos, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
		{
			ECGui.slot(pose, leftPos+slot.x-1, topPos+slot.y-1);
			if(slot.index<CraftingComputerState.PATTERN_SLOTS&&!slot.hasItem())
				GuiComponent.drawCenteredString(pose, font, Integer.toString(slot.index+1), leftPos+slot.x+8,
				topPos+slot.y+4, 0xFF9A9A9A);
		}
		int x = leftPos+CraftingComputerMenu.DISPLAY_X, y = topPos+CraftingComputerMenu.DISPLAY_Y;
		int width = CraftingComputerMenu.DISPLAY_WIDTH, height = CraftingComputerMenu.DISPLAY_HEIGHT;
		ECGui.sunken(pose, x-1, y-1, width+2, height+2);
		GuiComponent.fill(pose, x, y, x+width, y+height, SCREEN);

		List<OrderView> orders = menu.getClientOrders();
		selected = Math.max(0, Math.min(selected, orders.size()-1));
		OrderView order = orders.isEmpty()? null: orders.get(selected);
		StatusCode status = order==null? StatusCode.OK: order.status();
		font.draw(pose, ECGui.fit(font, status.title(), width-8), x+4, y+4, status.colour());
		String page = (selected+1)+"/"+orders.size();
		int picker = orders.size()>1? font.width(page)+2*ARROW_SIZE+8: 0;
		if(orders.size()>1)
		{
			int pageX = x+width-4-ARROW_SIZE-font.width(page)-2;
			font.draw(pose, page, pageX, y+PICKER_Y, DIM);
			arrow(pose, x+width-4-ARROW_SIZE, y+PICKER_Y, false, isOverArrow(mouseX, mouseY, false));
			arrow(pose, pageX-ARROW_SIZE-2, y+PICKER_Y, true, isOverArrow(mouseX, mouseY, true));
		}
		Component subtitle = order==null
				? new TranslatableComponent("gui.engineered_computation.crafting_computer.idle")
				: new TextComponent(String.format(Locale.ROOT, "%,d× ", order.amount())).append(order.target().name());
		drawSmall(pose, ECGui.fit(font, subtitle, (int)((width-8-picker)/SMALL)), x+4, y+15, TEXT);
		GuiComponent.fill(pose, x+3, y+24, x+width-3, y+25, RULE);

		List<Line> lines = lines(order);
		int rows = rows();
		scroll = Math.max(0, Math.min(scroll, lines.size()-rows));
		for(int i = 0; i<Math.min(rows, lines.size()-scroll); i++)
		{
			Line line = lines.get(scroll+i);
			int lineY = y+TREE_Y+i*LINE_HEIGHT;
			int indent = line.depth()*INDENT;
			int labelWidth = font.width(line.right());
			drawSmall(pose, ECGui.fit(font, line.text(), (int)((width-8-indent)/SMALL)-labelWidth-4), x+4+indent, lineY,
					line.colour());
			drawSmall(pose, new TextComponent(line.right()).getVisualOrderText(),
					x+width-4-(int)(labelWidth*SMALL), lineY, line.rightColour());
		}
		if(lines.size()>rows)
		{
			int track = height-TREE_Y-2;
			int thumb = Math.max(4, track*rows/lines.size());
			int thumbY = y+TREE_Y+(track-thumb)*scroll/Math.max(1, lines.size()-rows);
			GuiComponent.fill(pose, x+width-2, thumbY, x+width-1, thumbY+thumb, DIM);
		}
	}

	private record Line(int depth, Component text, int colour, String right, int rightColour, ItemKey item)
	{
	}

	private List<Line> lines(OrderView order)
	{
		List<Line> lines = new ArrayList<>();
		if(order!=null)
		{
			for(OrderView.Node node : order.tree())
			{
				Component name = node.depth()==0&&node.item().equals(order.target().item())? order.target().name()
						: node.item().displayStack().getHoverName();
				Component text = new TextComponent(
						(node.crafted()? "» ": "· ")+String.format(Locale.ROOT, "%,d ", node.amount()))
						.append(name);
				int colour = !node.crafted()? STORED: node.machine()? MACHINE: DIRECT;
				String right = node.crafted()? Math.round(node.progress()*100)+"%"
						: new TranslatableComponent("gui.engineered_computation.crafting_computer.stored").getString();
				lines.add(new Line(node.depth(), text, colour, right, node.crafted()&&node.progress()>=1? DONE: DIM,
						node.item()));
			}
			return lines;
		}
		List<ItemKey> makes = new ArrayList<>();
		for(int slot = 0; slot<CraftingComputerState.PATTERN_SLOTS; slot++)
		{
			ItemStack floppy = menu.slots.get(slot).getItem();
			if(PatternFloppyItem.holds(floppy, Pattern.Mode.DIRECT))
				makes.add(ItemKey.of(PatternFloppyItem.pattern(floppy).primaryOutput()));
		}
		if(makes.isEmpty())
		{
			lines.add(new Line(0, new TranslatableComponent("gui.engineered_computation.crafting_computer.no_patterns"),
					DIM, "", DIM, null));
			return lines;
		}
		lines.add(new Line(0, new TranslatableComponent("gui.engineered_computation.crafting_computer.makes"), DIM, "",
				DIM, null));
		for(ItemKey item : makes)
			lines.add(new Line(1, new TextComponent("» ").append(item.displayStack().getHoverName()), DIRECT, "", DIM,
					item));
		return lines;
	}

	private int rows()
	{
		return (CraftingComputerMenu.DISPLAY_HEIGHT-TREE_Y-2)/LINE_HEIGHT;
	}

	private void drawSmall(PoseStack pose, FormattedCharSequence text, int x, int y, int colour)
	{
		pose.pushPose();
		pose.translate(x, y, 0);
		pose.scale(SMALL, SMALL, 1);
		font.draw(pose, text, 0, 0, colour);
		pose.popPose();
	}

	private static void arrow(PoseStack pose, int x, int y, boolean left, boolean hovered)
	{
		int colour = hovered? 0xFFFFFFFF: DIM;
		for(int column = 0; column<4; column++)
		{
			int reach = left? 3-column: column;
			GuiComponent.fill(pose, x+1+column, y+reach, x+2+column, y+7-reach, colour);
		}
	}

	private boolean isOverArrow(double mouseX, double mouseY, boolean left)
	{
		List<OrderView> orders = menu.getClientOrders();
		if(orders.size()<2)
			return false;
		int x = leftPos+CraftingComputerMenu.DISPLAY_X, y = topPos+CraftingComputerMenu.DISPLAY_Y;
		int width = CraftingComputerMenu.DISPLAY_WIDTH;
		String page = (selected+1)+"/"+orders.size();
		int rightX = x+width-4-ARROW_SIZE;
		int leftX = rightX-font.width(page)-2-ARROW_SIZE-2;
		int arrowX = left? leftX: rightX;
		return mouseX>=arrowX&&mouseX<arrowX+ARROW_SIZE&&mouseY>=y+PICKER_Y-1&&mouseY<y+PICKER_Y+8;
	}

	private ItemKey lineAt(double mouseX, double mouseY)
	{
		int x = leftPos+CraftingComputerMenu.DISPLAY_X, y = topPos+CraftingComputerMenu.DISPLAY_Y+TREE_Y;
		if(mouseX<x||mouseX>=x+CraftingComputerMenu.DISPLAY_WIDTH||mouseY<y)
			return null;
		int row = (int)((mouseY-y)/LINE_HEIGHT);
		List<OrderView> orders = menu.getClientOrders();
		List<Line> lines = lines(orders.isEmpty()? null: orders.get(Math.min(selected, orders.size()-1)));
		return row<rows()&&scroll+row<lines.size()? lines.get(scroll+row).item(): null;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&(isOverArrow(mouseX, mouseY, true)||isOverArrow(mouseX, mouseY, false)))
		{
			int count = menu.getClientOrders().size();
			selected = (selected+(isOverArrow(mouseX, mouseY, true)? count-1: 1))%count;
			scroll = 0;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
	{
		int x = leftPos+CraftingComputerMenu.DISPLAY_X, y = topPos+CraftingComputerMenu.DISPLAY_Y;
		if(scrollY!=0&&mouseX>=x&&mouseX<x+CraftingComputerMenu.DISPLAY_WIDTH&&mouseY>=y
				&&mouseY<y+CraftingComputerMenu.DISPLAY_HEIGHT)
		{
			scroll = Math.max(0, scroll-(int)Math.signum(scrollY));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollY);
	}
}
