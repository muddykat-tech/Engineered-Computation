package com.muddykat.engineeredcomputation.client.screen.terminal;

import java.util.AbstractMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.GuiIcon;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternOffer;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.FloppyTerminalMenu;

public class FloppyTerminalScreen extends VideoTerminalScreen<FloppyTerminalMenu>
{
	private final Map<Map.Entry<ItemKey, Long>, PatternOffer> offerEntries = new IdentityHashMap<>();
	@Nullable
	private OrderPopup orderPopup;
	@Nullable
	private JobsPanel jobs;

	public FloppyTerminalScreen(FloppyTerminalMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
	}

	@Override
	protected void containerTick()
	{
		if(orderPopup!=null)
		{
			orderPopup.tick();
			if(orderPopup.isClosed())
				orderPopup = null;
		}
		super.containerTick();
	}

	@Override
	protected void collectEntries(List<Map.Entry<ItemKey, Long>> view, String query)
	{
		super.collectEntries(view, query);
		for(ItemKey craftable : menu.getClientCraftables())
			if(!menu.getClientContents().containsKey(craftable)&&matches(craftable, query))
				view.add(Map.entry(craftable, 0L));
		offerEntries.clear();
		for(PatternOffer offer : menu.getClientOffers())
		{
			ItemKey shown = ItemKey.of(offer.pattern().primaryOutput());
			if(matches(shown, query)||offer.outputTag().map(tag -> tag.location().toString().contains(
					query.startsWith("#")? query.substring(1): query)).orElse(false))
			{
				Map.Entry<ItemKey, Long> entry = new AbstractMap.SimpleImmutableEntry<>(shown, 0L);
				view.add(entry);
				offerEntries.put(entry, offer);
			}
		}
	}

	@Override
	protected ItemStack shownStack(Map.Entry<ItemKey, Long> entry)
	{
		PatternOffer offer = offerEntries.get(entry);
		return offer!=null? offer.shown(minecraft.level.getGameTime()): super.shownStack(entry);
	}

	@Override
	protected boolean gridCovered(double mouseX, double mouseY)
	{
		return orderPopup!=null||jobs!=null;
	}

	@Override
	protected void renderPopups(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		if(orderPopup!=null)
		{
			orderPopup.render(graphics, mouseX, mouseY, partialTick);
			List<Component> lines = orderPopup.tooltip(mouseX, mouseY);
			if(!lines.isEmpty())
			{
				graphics.pose().pushPose();
				graphics.pose().translate(0, 0, 500);
				graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
				graphics.pose().popPose();
			}
			return;
		}
		if(jobs!=null)
		{
			jobs.render(graphics, mouseX, mouseY);
			List<Component> lines = jobs.tooltip(mouseX, mouseY);
			if(!lines.isEmpty())
				graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
			else if(!jobs.isOver(mouseX, mouseY)&&!renderWidgetTooltips(graphics, mouseX, mouseY))
				renderTooltip(graphics, mouseX, mouseY);
			return;
		}
		super.renderPopups(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void addEntryLines(List<Component> lines, Map.Entry<ItemKey, Long> entry)
	{
		PatternOffer offer = offerEntries.get(entry);
		if(offer!=null)
		{
			offer.outputTag().ifPresent(tag -> lines.add(Component.translatable(
					"gui.engineered_computation.terminal.offer.any", "#"+tag.location()).withStyle(ChatFormatting.GRAY)));
			if(!offer.pattern().inputTags().isEmpty())
				lines.add(Component.translatable("gui.engineered_computation.terminal.offer.pick")
						.withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("gui.engineered_computation.terminal.order").withStyle(ChatFormatting.AQUA));
			return;
		}
		super.addEntryLines(lines, entry);
		if(menu.getClientCraftables().contains(entry.getKey()))
			lines.add(Component.translatable(entry.getValue()==0? "gui.engineered_computation.terminal.order"
					: "gui.engineered_computation.terminal.order_more").withStyle(ChatFormatting.AQUA));
	}

	@Override
	protected boolean showsProcessing()
	{
		return false;
	}

	@Nullable
	@Override
	protected List<Component> widgetTooltip(double mouseX, double mouseY)
	{
		if(isOverCorner(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.jobs").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.jobs.detail", menu.getClientOrders().size())
							.withStyle(ChatFormatting.GRAY));
		return super.widgetTooltip(mouseX, mouseY);
	}

	@Override
	protected void renderCorner(GuiGraphics graphics, int mouseX, int mouseY)
	{
		ECGui.button(graphics, leftPos+cornerX(), topPos+4, BUTTON_SIZE, BUTTON_SIZE,
				jobs!=null||isOverCorner(mouseX, mouseY));
		(menu.getClientOrders().isEmpty()? GuiIcon.JOBS: GuiIcon.JOBS_ACTIVE).draw(graphics, leftPos+cornerX()+3,
				topPos+7);
	}

	@Override
	protected void renderEntry(GuiGraphics graphics, Map.Entry<ItemKey, Long> entry, int x, int y)
	{
		if(entry.getValue()==0)
		{
			graphics.fill(RenderType.guiOverlay(), x, y, x+16, y+16, CRAFTABLE_FADE);
			renderSmall(graphics, Component.translatable("gui.engineered_computation.terminal.craft").getString(), x+16,
					y+16, StatusCode.PROCESSING.colour());
			if(offerEntries.containsKey(entry))
				ECGui.tagBadge(graphics, font, x, y);
			return;
		}
		super.renderEntry(graphics, entry, x, y);
		if(menu.getClientCraftables().contains(entry.getKey()))
			renderSmall(graphics, "+", x+5, y+7, StatusCode.PROCESSING.colour());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(orderPopup!=null)
			return orderPopup.mouseClicked(mouseX, mouseY, button);
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverCorner(mouseX, mouseY))
		{
			jobs = jobs==null? new JobsPanel(menu, font, leftPos+TerminalMenu.GRID_X, topPos+layout.gridY(),
					GRID_WIDTH+SCROLLBAR_WIDTH+4, gridHeight): null;
			clickSound();
			return true;
		}
		if(jobs!=null&&jobs.mouseClicked(mouseX, mouseY))
			return true;
		Map.Entry<ItemKey, Long> craftable = entryAt(mouseX, mouseY);
		PatternOffer offer = craftable==null? null: offerEntries.get(craftable);
		if(offer!=null&&menu.getCarried().isEmpty()&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				||button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
		{
			unfocusSearch();
			orderPopup = new OrderPopup(menu, font, offer, leftPos+(imageWidth-OrderPopup.WIDTH)/2,
					topPos+layout.gridY()-8);
			return true;
		}
		if(craftable!=null&&menu.getCarried().isEmpty()&&menu.getClientCraftables().contains(craftable.getKey())
				&&(craftable.getValue()==0&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				||hasControlDown()&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
		{
			unfocusSearch();
			orderPopup = new OrderPopup(menu, font, craftable.getKey(), leftPos+(imageWidth-OrderPopup.WIDTH)/2,
					topPos+layout.gridY()-8);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		if(orderPopup!=null)
			return orderPopup.charTyped(codePoint, modifiers);
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(orderPopup!=null)
			return orderPopup.mouseScrolled(mouseX, mouseY, scrollY);
		if(jobs!=null&&jobs.isOver(mouseX, mouseY))
		{
			jobs.mouseScrolled(scrollY);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(orderPopup!=null)
			return orderPopup.keyPressed(keyCode, scanCode, modifiers);
		if(jobs!=null&&keyCode==GLFW.GLFW_KEY_ESCAPE)
		{
			jobs = null;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
