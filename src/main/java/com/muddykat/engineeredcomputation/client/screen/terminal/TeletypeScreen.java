package com.muddykat.engineeredcomputation.client.screen.terminal;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.GuiIcon;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.ItemSearch;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;
import com.muddykat.engineeredcomputation.terminal.TerminalStats;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeMenu;

public class TeletypeScreen extends TerminalScreen<TeletypeMenu>
{
	private static final int SLIP_HEIGHT = 18;
	/** Lighter than unreachable items, because a slipped item is still there for now. */
	private static final int SLIPPED_FADE = 0x608B8B8B;
	private static final int SLIPPED_COUNT = 0xFF6A5A;
	private static final int LOCKED_COUNT = 0xB0B0B0;

	private final Set<Map.Entry<ItemKey, Long>> lockedEntries = Collections.newSetFromMap(new IdentityHashMap<>());
	private ProgramEntry seenFilter = ProgramEntry.EMPTY;
	private boolean seenHidesLocked;
	@Nullable
	private OutletPicker outletPicker;
	@Nullable
	private CardEditor cardEditor;

	public TeletypeScreen(TeletypeMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
	}

	@Override
	protected void containerTick()
	{
		if(cardEditor!=null)
		{
			cardEditor.tick();
			if(cardEditor.isClosed())
				cardEditor = null;
		}
		super.containerTick();
	}

	@Override
	protected boolean viewChanged()
	{
		return super.viewChanged()||!menu.cardFilter().equals(seenFilter)||menu.hidesLocked()!=seenHidesLocked;
	}

	@Override
	protected void collectEntries(List<Map.Entry<ItemKey, Long>> view, String query)
	{
		seenFilter = menu.cardFilter();
		seenHidesLocked = menu.hidesLocked();
		super.collectEntries(view, query);
		lockedEntries.clear();
		if(seenHidesLocked)
			return;
		for(Map.Entry<ItemKey, Long> held : menu.getClientLocked().entrySet())
			if(matches(held.getKey(), query)&&shows(held.getKey()))
			{
				Map.Entry<ItemKey, Long> entry = new AbstractMap.SimpleImmutableEntry<>(held.getKey(), held.getValue());
				view.add(entry);
				lockedEntries.add(entry);
			}
	}

	@Override
	protected boolean shows(ItemKey key)
	{
		if(seenFilter.isEmpty())
			return true;
		return seenFilter.name().map(name -> matches(key, ItemSearch.normalise(name)))
				.orElseGet(() -> seenFilter.matches(key.displayStack()));
	}

	@Override
	protected int sortGroup(Map.Entry<ItemKey, Long> entry)
	{
		return lockedEntries.contains(entry)? 1: 0;
	}

	@Override
	protected boolean gridCovered(double mouseX, double mouseY)
	{
		return outletPicker!=null||cardEditor!=null&&cardEditor.isOver(mouseX, mouseY);
	}

	@Override
	public List<Rect2i> extraAreas()
	{
		return cardEditor==null? List.of(): List.of(cardEditor.area());
	}

	@Override
	protected void renderPopups(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		if(outletPicker!=null&&outletPicker.isClosed())
			outletPicker = null;
		if(cardEditor!=null)
		{
			cardEditor.render(graphics, mouseX, mouseY, partialTick);
			if(outletPicker==null&&cardEditor.isOver(mouseX, mouseY))
			{
				List<Component> lines = cardEditor.tooltip(mouseX, mouseY);
				graphics.pose().pushPose();
				graphics.pose().translate(0, 0, 500);
				if(!lines.isEmpty())
					graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
				graphics.pose().popPose();
				return;
			}
		}
		if(outletPicker!=null)
		{
			outletPicker.render(graphics, mouseX, mouseY, partialTick);
			return;
		}
		super.renderPopups(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack)
	{
		List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if(hoveredSlot!=null&&hoveredSlot.index==TeletypeMenu.CARD_SLOT)
			lines.add(Component.translatable("gui.engineered_computation.teletype.card.edit")
					.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	@Override
	protected List<Component> carriedTooltip()
	{
		return List.of(Component.translatable("gui.engineered_computation.teletype.put_away"),
				Component.translatable("gui.engineered_computation.teletype.put_away.detail")
						.withStyle(ChatFormatting.GRAY));
	}

	@Override
	protected void addEntryLines(List<Component> lines, Map.Entry<ItemKey, Long> entry)
	{
		if(lockedEntries.contains(entry))
		{
			lines.add(Component.translatable("gui.engineered_computation.teletype.locked", number(entry.getValue()))
					.withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("gui.engineered_computation.teletype.locked.detail")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		lines.add(Component.translatable("gui.engineered_computation.teletype.tallied", number(entry.getValue()))
				.withStyle(ChatFormatting.GOLD));
		long slipped = onSlip(entry.getKey());
		if(slipped>0)
			lines.add(Component.translatable("gui.engineered_computation.teletype.slipped", number(slipped),
					number(Math.max(0, entry.getValue()-slipped))).withStyle(ChatFormatting.RED));
		lines.add(menu.isUnreachableOnClient(entry.getKey())
				? Component.translatable("gui.engineered_computation.teletype.unreachable").withStyle(ChatFormatting.RED)
				: Component.translatable("gui.engineered_computation.teletype.add").withStyle(ChatFormatting.AQUA));
	}

	@Override
	protected boolean renderWidgetTooltips(GuiGraphics graphics, int mouseX, int mouseY)
	{
		int slipLine = slipLineAt(mouseX, mouseY);
		if(slipLine<0)
			return super.renderWidgetTooltips(graphics, mouseX, mouseY);
		RequestSlip.Line line = menu.getClientSlip().get(slipLine);
		ItemStack stack = line.key().displayStack();
		List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(stack));
		tooltip.add(Component.translatable("gui.engineered_computation.teletype.on_slip", number(line.amount()))
				.withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("gui.engineered_computation.teletype.on_slip.detail")
				.withStyle(ChatFormatting.GRAY));
		graphics.renderTooltip(font, tooltip, stack.getTooltipImage(), stack, mouseX, mouseY);
		return true;
	}

	@Nullable
	@Override
	protected List<Component> widgetTooltip(double mouseX, double mouseY)
	{
		if(isOverEmptyCardSlot(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.teletype.card")
							.withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.teletype.card.empty").withStyle(ChatFormatting.GRAY));
		if(isOverSend(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.teletype.send").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.teletype.send.detail", menu.getClientOutlet())
							.withStyle(ChatFormatting.GRAY));
		if(isOverOutlet(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.teletype.outlet",
							menu.getClientOutlet()).withStyle(ChatFormatting.YELLOW),
					Component.translatable(menu.isClientOutletHome()? "gui.engineered_computation.teletype.outlet.home"
							: "gui.engineered_computation.teletype.outlet.away").withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.teletype.outlet.detail")
							.withStyle(ChatFormatting.DARK_GRAY));
		if(isOverCorner(mouseX, mouseY))
		{
			String key = "gui.engineered_computation.teletype.locked_items."+(menu.hidesLocked()? "hidden": "shown");
			return List.of(Component.translatable(key).withStyle(ChatFormatting.YELLOW),
					Component.translatable(key+".detail").withStyle(ChatFormatting.GRAY));
		}
		return super.widgetTooltip(mouseX, mouseY);
	}

	@Override
	protected List<Component> gaugeTooltip()
	{
		TerminalStats stats = menu.getClientStats();
		return List.of(
				Component.translatable("gui.engineered_computation.teletype.catalogue").withStyle(ChatFormatting.YELLOW),
				Component.translatable("gui.engineered_computation.teletype.cards", stats.usedBytes(),
						stats.totalBytes()).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.engineered_computation.teletype.kinds", stats.usedTypes())
						.withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.engineered_computation.teletype.stations", stats.totalTypes())
						.withStyle(ChatFormatting.GRAY),
				stats.lowPressure()
						? Component.translatable("gui.engineered_computation.teletype.low_pressure")
								.withStyle(ChatFormatting.GOLD)
						: Component.translatable("gui.engineered_computation.teletype.high_pressure")
								.withStyle(ChatFormatting.GRAY));
	}

	@Override
	protected String processingDetail()
	{
		return "gui.engineered_computation.teletype.coming";
	}

	@Override
	protected int processingX()
	{
		return cornerX()-BUTTON_SIZE-8;
	}

	@Override
	protected void renderCorner(GuiGraphics graphics, int mouseX, int mouseY)
	{
		ECGui.button(graphics, leftPos+cornerX(), topPos+4, BUTTON_SIZE, BUTTON_SIZE, isOverCorner(mouseX, mouseY));
		(menu.hidesLocked()? GuiIcon.LOCKED_HIDDEN: GuiIcon.LOCKED_SHOWN).draw(graphics, leftPos+cornerX()+3, topPos+7);
		super.renderCorner(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderExtras(GuiGraphics graphics, int mouseX, int mouseY)
	{
		renderSlip(graphics, mouseX, mouseY);
		Slot card = menu.getSlot(TeletypeMenu.CARD_SLOT);
		if(!card.hasItem())
			ECGui.ghost(graphics, new ItemStack(ECCatalogue.FILTER_CARD.get()), leftPos+card.x, topPos+card.y);
	}

	private void renderSlip(GuiGraphics graphics, int mouseX, int mouseY)
	{
		int slipY = topPos+layout.craftingY();
		List<RequestSlip.Line> lines = menu.getClientSlip();
		int hovered = slipLineAt(mouseX, mouseY);
		for(int i = 0; i<RequestSlip.SIZE; i++)
		{
			int sx = leftPos+TerminalMenu.GRID_X+i*18;
			ECGui.slot(graphics, sx, slipY);
			if(i<lines.size())
			{
				graphics.renderItem(lines.get(i).key().displayStack(), sx+1, slipY+1);
				renderCount(graphics, lines.get(i).amount(), sx+1, slipY+1);
			}
			if(i==hovered)
				graphics.fill(sx+1, slipY+1, sx+17, slipY+17, 300, HOVER);
		}
		int sendX = leftPos+SCROLLBAR_X;
		ECGui.button(graphics, sendX, slipY, SCROLLBAR_WIDTH, SLIP_HEIGHT, isOverSend(mouseX, mouseY));
		(lines.isEmpty()? GuiIcon.SEND_DISABLED: GuiIcon.SEND).draw(graphics, sendX+(SCROLLBAR_WIDTH-5)/2,
				slipY+(SLIP_HEIGHT-5)/2);
	}

	@Override
	protected void renderEntry(GuiGraphics graphics, Map.Entry<ItemKey, Long> entry, int x, int y)
	{
		if(lockedEntries.contains(entry))
		{
			graphics.fill(RenderType.guiOverlay(), x, y, x+16, y+16, CRAFTABLE_FADE);
			renderCount(graphics, entry.getValue(), x, y, LOCKED_COUNT);
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 250);
			GuiIcon.LOCKED.draw(graphics, x, y);
			graphics.pose().popPose();
		}
		else if(onSlip(entry.getKey())>0)
		{
			graphics.fill(RenderType.guiOverlay(), x, y, x+16, y+16, SLIPPED_FADE);
			renderCount(graphics, Math.max(0, entry.getValue()-onSlip(entry.getKey())), x, y, SLIPPED_COUNT);
		}
		else
		{
			super.renderEntry(graphics, entry, x, y);
			if(menu.isUnreachableOnClient(entry.getKey()))
				graphics.fill(RenderType.guiOverlay(), x, y, x+16, y+16, CRAFTABLE_FADE);
		}
	}

	private long onSlip(ItemKey key)
	{
		long asked = 0;
		for(RequestSlip.Line line : menu.getClientSlip())
			if(line.key().equals(key))
				asked += line.amount();
		return asked;
	}

	@Override
	protected boolean canTake(Map.Entry<ItemKey, Long> entry)
	{
		return !lockedEntries.contains(entry);
	}

	@Override
	protected Component usageLabel()
	{
		return Component.translatable("gui.engineered_computation.teletype.to", menu.getClientOutlet());
	}

	@Override
	protected int usageColour(int mouseX, int mouseY)
	{
		return isOverOutlet(mouseX, mouseY)? 0x2050A0: ECGui.LABEL;
	}

	private int slipLineAt(double mouseX, double mouseY)
	{
		if(!isOver(mouseX, mouseY, TerminalMenu.GRID_X, layout.craftingY(), GRID_WIDTH, SLIP_HEIGHT))
			return -1;
		int line = (int)(mouseX-leftPos-TerminalMenu.GRID_X)/18;
		return line<menu.getClientSlip().size()? line: -1;
	}

	private boolean isOverSend(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, SCROLLBAR_X, layout.craftingY(), SCROLLBAR_WIDTH, SLIP_HEIGHT);
	}

	private boolean isOverOutlet(double mouseX, double mouseY)
	{
		int width = font.width(usageLabel());
		return isOver(mouseX, mouseY, imageWidth-8-width, inventoryLabelY-1, width, font.lineHeight+1);
	}

	private boolean isOverCardToEdit(double mouseX, double mouseY)
	{
		if(!menu.getCarried().isEmpty())
			return false;
		Slot card = menu.getSlot(TeletypeMenu.CARD_SLOT);
		return card.hasItem()&&isOver(mouseX, mouseY, card.x-1, card.y-1, 18, 18);
	}

	private boolean isOverEmptyCardSlot(double mouseX, double mouseY)
	{
		if(!menu.getCarried().isEmpty())
			return false;
		Slot card = menu.getSlot(TeletypeMenu.CARD_SLOT);
		return !card.hasItem()&&isOver(mouseX, mouseY, card.x-1, card.y-1, 18, 18);
	}

	private void openCardEditor()
	{
		int x = leftPos-CardEditor.WIDTH-4;
		int y = topPos+layout.gridY();
		if(x<0)
		{
			x = leftPos+imageWidth-CardEditor.WIDTH-4;
			y = topPos+layout.inventoryY()+4;
		}
		cardEditor = new CardEditor(menu, font, x, y);
		clickSound();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(outletPicker!=null)
			return outletPicker.mouseClicked(mouseX, mouseY, button);
		if(cardEditor!=null&&cardEditor.mouseClicked(mouseX, mouseY, button))
			return true;
		if(isOverCardToEdit(mouseX, mouseY)&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&!hasShiftDown())
		{
			if(cardEditor==null)
				openCardEditor();
			return true;
		}
		boolean left = button==GLFW.GLFW_MOUSE_BUTTON_LEFT;
		if(left&&isOverCorner(mouseX, mouseY))
		{
			pressButton(TeletypeMenu.HIDE_LOCKED);
			return true;
		}
		if(left&&isOverOutlet(mouseX, mouseY))
		{
			unfocusSearch();
			outletPicker = new OutletPicker(menu, font, leftPos+TerminalMenu.GRID_X, topPos+inventoryLabelY+10);
			clickSound();
			return true;
		}
		if(left||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
		{
			int slipLine = slipLineAt(mouseX, mouseY);
			int id = slipLine>=0? (left? TeletypeMenu.SLIP_REMOVE: TeletypeMenu.SLIP_LOWER)+slipLine
					: isOverSend(mouseX, mouseY)&&left? TeletypeMenu.SEND_SLIP: -1;
			if(id>=0)
			{
				pressButton(id);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		if(outletPicker!=null)
			return outletPicker.charTyped(codePoint, modifiers);
		if(cardEditor!=null&&cardEditor.charTyped(codePoint, modifiers))
			return true;
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(cardEditor!=null&&cardEditor.mouseDragged(mouseX, mouseY))
			return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(cardEditor!=null&&cardEditor.mouseReleased())
			return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(outletPicker!=null)
			return outletPicker.mouseScrolled(mouseX, mouseY, scrollY);
		if(cardEditor!=null&&cardEditor.isOver(mouseX, mouseY))
			return true;
		if(isOverOutlet(mouseX, mouseY)&&scrollY!=0)
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
					scrollY>0? TeletypeMenu.PREVIOUS_OUTLET: TeletypeMenu.NEXT_OUTLET);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(outletPicker!=null)
			return outletPicker.keyPressed(keyCode, scanCode, modifiers);
		if(cardEditor!=null&&cardEditor.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
