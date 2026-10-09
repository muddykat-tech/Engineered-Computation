package com.muddykat.engineeredcomputation.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.crafting.CraftingPlanner;
import com.muddykat.engineeredcomputation.crafting.OrderPlanPayload;
import com.muddykat.engineeredcomputation.crafting.OrderRequestPayload;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.PatternOffer;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ItemKey;

final class OrderPopup
{
	static final int WIDTH = 168;
	static final int HEIGHT = 178;
	private static final int LINE_HEIGHT = 18;
	private static final int LINES_Y = 44;
	private static final int VISIBLE_LINES = 5;
	private static final int BUTTON_HEIGHT = 14;
	private static final int BUTTON_WIDTH = 50;
	private static final int FIELD_X = 50;
	private static final int FIELD_WIDTH = 64;
	private static final int LESS_X = 120;
	private static final int MORE_X = 138;
	private static final int ASK_DELAY = 4;
	private static final int PICK_HEIGHT = 18;
	private static final int PICK_LIST_LINES = 6;

	private final TerminalMenu menu;
	private final Font font;
	private final ItemKey target;
	@Nullable
	private final PatternOffer offer;
	private final List<Integer> tagSlots = new ArrayList<>();
	private final Map<Integer, List<Item>> tagItems = new HashMap<>();
	private final Map<Integer, Item> picks = new HashMap<>();
	private int openRow = -1;
	private int listScroll;
	private Map<Integer, Item> askedPicks = Map.of();
	private final int x;
	private final int y;
	private final EditBox amount;
	private long asked = -1;
	private int sinceChange;
	private int scroll;
	private boolean closed;

	OrderPopup(TerminalMenu menu, Font font, ItemKey target, int x, int y)
	{
		this(menu, font, target, null, x, y);
	}

	OrderPopup(TerminalMenu menu, Font font, PatternOffer offer, int x, int y)
	{
		this(menu, font, ItemKey.of(offer.pattern().primaryOutput()), offer, x, y);
	}

	private OrderPopup(TerminalMenu menu, Font font, ItemKey target, @Nullable PatternOffer offer, int x, int y)
	{
		this.menu = menu;
		this.font = font;
		this.target = target;
		this.offer = offer;
		if(offer!=null)
			offer.pattern().inputTags().forEach((slot, tag) -> {
				tagSlots.add(slot);
				tagItems.put(slot, Pattern.itemsOf(tag));
			});
		tagSlots.sort(Comparator.naturalOrder());
		this.x = x;
		this.y = y;
		amount = new EditBox(font, x+FIELD_X+2, y+25, FIELD_WIDTH-4, 12,
				Component.translatable("gui.engineered_computation.order.amount"));
		amount.setMaxLength(7);
		amount.setFilter(text -> text.chars().allMatch(Character::isDigit));
		amount.setValue("1");
		amount.setFocused(true);
		amount.setResponder(text -> sinceChange = 0);
		menu.clearClientPlan();
	}

	boolean isClosed()
	{
		return closed;
	}

	private long amount()
	{
		try
		{
			return Math.min(CraftingPlanner.MAX_AMOUNT, Math.max(1, Long.parseLong(amount.getValue())));
		}
		catch(NumberFormatException e)
		{
			return 1;
		}
	}

	private long stored(Item item)
	{
		return menu.getClientContents().getOrDefault(ItemKey.of(new ItemStack(item)), 0L);
	}

	private boolean picked()
	{
		return picks.size()==tagSlots.size();
	}

	void tick()
	{
		OrderPlanPayload plan = menu.getClientPlan();
		if(plan!=null&&plan.placed())
		{
			menu.clearClientPlan();
			closed = true;
			return;
		}
		if(++sinceChange>=ASK_DELAY&&picked()&&(amount()!=asked||!askedPicks.equals(picks)))
		{
			asked = amount();
			askedPicks = Map.copyOf(picks);
			PacketDistributor.sendToServer(request(false));
		}
	}

	private OrderRequestPayload request(boolean place)
	{
		return new OrderRequestPayload(menu.containerId, target, amount(), place, offer==null? Optional.empty():
				Optional.of(new PatternOffer.Choice(offer, Map.copyOf(picks))));
	}

	private OrderPlanPayload plan()
	{
		OrderPlanPayload plan = menu.getClientPlan();
		return plan!=null&&picked()&&askedPicks.equals(picks)&&plan.target().equals(target)&&plan.amount()==amount()
				? plan: null;
	}

	private int height()
	{
		return HEIGHT+Math.max(0, tagSlots.size()-(VISIBLE_LINES-1))*PICK_HEIGHT;
	}

	private int listY()
	{
		return y+LINES_Y+tagSlots.size()*PICK_HEIGHT;
	}

	private int visibleLines()
	{
		return Math.max(1, VISIBLE_LINES-tagSlots.size());
	}

	private int pickX()
	{
		return x+WIDTH-26;
	}

	private int pickY(int row)
	{
		return y+LINES_Y+row*PICK_HEIGHT;
	}

	private int pickAt(double mouseX, double mouseY)
	{
		for(int row = 0; row<tagSlots.size(); row++)
			if(isOver(mouseX, mouseY, pickX(), pickY(row), 18, 18))
				return row;
		return -1;
	}

	private List<Item> stocked(int row)
	{
		List<Item> items = new ArrayList<>();
		for(Item item : tagItems.get(tagSlots.get(row)))
			if(stored(item)>0)
				items.add(item);
		items.sort(Comparator.comparingLong(item -> -stored(item)));
		return items;
	}

	private void cycle(int row, int by)
	{
		List<Item> items = stocked(row);
		if(items.isEmpty())
			return;
		int slot = tagSlots.get(row);
		int index = items.indexOf(picks.get(slot));
		index = index<0? (by>0? 0: items.size()-1): Math.floorMod(index+by, items.size());
		pick(row, items.get(index));
	}

	private void pick(int row, @Nullable Item item)
	{
		if(item==null)
			picks.remove(tagSlots.get(row));
		else
			picks.put(tagSlots.get(row), item);
		sinceChange = 0;
		click();
	}

	private int dropX()
	{
		return x+6;
	}

	private int dropY()
	{
		return pickY(openRow)+PICK_HEIGHT;
	}

	private int dropLines()
	{
		return Math.max(1, Math.min(PICK_LIST_LINES, stocked(openRow).size()));
	}

	private int listIndexAt(double mouseX, double mouseY)
	{
		if(openRow<0||!isOver(mouseX, mouseY, dropX(), dropY(), WIDTH-12, dropLines()*LINE_HEIGHT+2))
			return -1;
		int index = listScroll+(int)((mouseY-dropY()-1)/LINE_HEIGHT);
		return index<stocked(openRow).size()? index: -1;
	}

	private boolean isOverList(double mouseX, double mouseY)
	{
		return openRow>=0&&isOver(mouseX, mouseY, dropX(), dropY(), WIDTH-12, dropLines()*LINE_HEIGHT+2);
	}

	private void renderList(GuiGraphics graphics, int mouseX, int mouseY)
	{
		List<Item> items = stocked(openRow);
		int lines = dropLines();
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 300);
		graphics.fill(dropX()-1, dropY()-1, dropX()+WIDTH-11, dropY()+lines*LINE_HEIGHT+3, 0xFFA0A0A0);
		graphics.fill(dropX(), dropY(), dropX()+WIDTH-12, dropY()+lines*LINE_HEIGHT+2, 0xF0101010);
		if(items.isEmpty())
			graphics.drawString(font, Component.translatable("gui.engineered_computation.order.pick.none"), dropX()+4,
					dropY()+6, 0xA0A0A0, false);
		listScroll = Math.max(0, Math.min(listScroll, items.size()-lines));
		int hovered = listIndexAt(mouseX, mouseY);
		for(int i = 0; i<Math.min(lines, items.size()-listScroll); i++)
		{
			Item item = items.get(listScroll+i);
			int lineY = dropY()+1+i*LINE_HEIGHT;
			if(listScroll+i==hovered)
				graphics.fill(dropX()+1, lineY, dropX()+WIDTH-13, lineY+LINE_HEIGHT, 0x40FFFFFF);
			graphics.renderItem(new ItemStack(item), dropX()+2, lineY+1);
			boolean picked = item==picks.get(tagSlots.get(openRow));
			graphics.drawString(font, ECGui.fit(font, new ItemStack(item).getHoverName(), WIDTH-70), dropX()+21,
					lineY+5, picked? 0xFFD24A: 0xFFFFFF, false);
			String stored = count(stored(item));
			graphics.drawString(font, stored, dropX()+WIDTH-16-font.width(stored), lineY+5, 0x8CC8FF, false);
		}
		graphics.pose().popPose();
	}

	void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		ECGui.panel(graphics, x, y, WIDTH, height());
		long time = Minecraft.getInstance().level==null? 0: Minecraft.getInstance().level.getGameTime();
		ItemStack shown = offer!=null? offer.shown(time): target.displayStack();
		graphics.renderItem(shown, x+7, y+5);
		if(offer!=null&&offer.outputTag().isPresent())
			ECGui.tagBadge(graphics, font, x+7, y+5);
		Component name = offer!=null&&offer.outputTag().isPresent()? Component.literal("#"+offer.outputTag().get()
				.location()): shown.getHoverName();
		graphics.drawString(font, ECGui.fit(font, Component.translatable("gui.engineered_computation.order.title",
				name), WIDTH-36), x+28, y+9, ECGui.LABEL, false);
		for(int row = 0; row<tagSlots.size(); row++)
		{
			int slot = tagSlots.get(row);
			Component label = Component.translatable("gui.engineered_computation.order.send",
					"#"+offer.pattern().inputTag(slot).map(tag -> tag.location().toString()).orElse(""));
			graphics.drawString(font, ECGui.fit(font, label, WIDTH-40), x+7, pickY(row)+5, ECGui.LABEL, false);
			ECGui.slot(graphics, pickX(), pickY(row));
			Item pick = picks.get(slot);
			List<Item> items = tagItems.get(slot);
			if(pick!=null)
				graphics.renderItem(new ItemStack(pick), pickX()+1, pickY(row)+1);
			else if(!items.isEmpty())
			{
				graphics.renderItem(new ItemStack(items.get((int)(time/20%items.size()))), pickX()+1, pickY(row)+1);
				graphics.fill(RenderType.guiOverlay(), pickX()+1, pickY(row)+1, pickX()+17, pickY(row)+17, 0x908B8B8B);
				graphics.pose().pushPose();
				graphics.pose().translate(0, 0, 200);
				graphics.drawCenteredString(font, "?", pickX()+9, pickY(row)+5, 0xFFD24A);
				graphics.pose().popPose();
			}
			if(isOver(mouseX, mouseY, pickX(), pickY(row), 18, 18))
				graphics.fill(pickX()+1, pickY(row)+1, pickX()+17, pickY(row)+17, 300, 0x80FFFFFF);
		}

		graphics.drawString(font, Component.translatable("gui.engineered_computation.order.amount"), x+7, y+27,
				ECGui.LABEL, false);
		graphics.fill(x+FIELD_X, y+23, x+FIELD_X+FIELD_WIDTH, y+37, 0xFFA0A0A0);
		graphics.fill(x+FIELD_X+1, y+24, x+FIELD_X+FIELD_WIDTH-1, y+36, 0xFF000000);
		amount.render(graphics, mouseX, mouseY, partialTick);
		button(graphics, mouseX, mouseY, x+LESS_X, y+23, 14, Component.literal("-"));
		button(graphics, mouseX, mouseY, x+MORE_X, y+23, 14, Component.literal("+"));

		int listY = listY();
		int visible = visibleLines();
		ECGui.sunken(graphics, x+6, listY-1, WIDTH-12, visible*LINE_HEIGHT+2);
		OrderPlanPayload plan = plan();
		if(plan!=null)
		{
			List<OrderPlanPayload.Line> lines = plan.lines();
			scroll = Math.max(0, Math.min(scroll, lines.size()-visible));
			for(int i = 0; i<Math.min(visible, lines.size()-scroll); i++)
				renderLine(graphics, lines.get(scroll+i), x+8, listY+1+i*LINE_HEIGHT);
		}
		else
			graphics.drawCenteredString(font, Component.translatable(picked()? "gui.engineered_computation.order.working"
					: "gui.engineered_computation.order.pick"), x+WIDTH/2, listY+visible*LINE_HEIGHT/2-4, 0xA0A0A0);

		int statusY = listY+visible*LINE_HEIGHT+4;
		if(plan!=null)
		{
			StatusCode status = plan.status();
			Component text = status.title().copy().append(" - ").append(Component.translatable(
					"gui.engineered_computation.order.status."+status.code()));
			graphics.drawString(font, ECGui.fit(font, text, WIDTH-14), x+7, statusY, status.colour(), true);
		}
		boolean ready = plan!=null&&plan.status()==StatusCode.OK;
		int buttonsY = y+height()-BUTTON_HEIGHT-6;
		ECGui.button(graphics, x+WIDTH/2-BUTTON_WIDTH-3, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT,
				ready&&isOverOrder(mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable("gui.engineered_computation.order.place"),
				x+WIDTH/2-BUTTON_WIDTH/2-3, buttonsY+3, ready? 0xFFFFFF: 0x808080);
		ECGui.button(graphics, x+WIDTH/2+3, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, isOverCancel(mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable("gui.cancel"), x+WIDTH/2+BUTTON_WIDTH/2+3, buttonsY+3,
				0xFFFFFF);
		if(openRow>=0)
			renderList(graphics, mouseX, mouseY);
		graphics.pose().popPose();
	}

	private boolean isTagTarget(ItemKey item)
	{
		return offer!=null&&offer.outputTag().isPresent()&&item.equals(target);
	}

	private ItemStack shown(ItemKey item)
	{
		if(isTagTarget(item))
			return offer.shown(Minecraft.getInstance().level==null? 0: Minecraft.getInstance().level.getGameTime());
		return item.displayStack();
	}

	private void renderLine(GuiGraphics graphics, OrderPlanPayload.Line line, int lineX, int lineY)
	{
		ItemStack stack = shown(line.item());
		graphics.renderItem(stack, lineX, lineY);
		if(isTagTarget(line.item()))
			ECGui.tagBadge(graphics, font, lineX, lineY);
		List<Component> parts = new ArrayList<>();
		if(line.missing()>0)
			parts.add(Component.translatable("gui.engineered_computation.order.missing", count(line.missing()))
					.withStyle(ChatFormatting.RED));
		if(line.crafted()>0)
			parts.add(Component.translatable("gui.engineered_computation.order.crafts", count(line.crafted()))
					.withStyle(ChatFormatting.GOLD));
		if(line.fromStorage()>0)
			parts.add(Component.translatable("gui.engineered_computation.order.uses", count(line.fromStorage()))
					.withStyle(ChatFormatting.DARK_GREEN));
		MutableComponent text = Component.empty();
		for(int i = 0; i<parts.size(); i++)
			text.append(i==0? Component.empty(): Component.literal(", ")).append(parts.get(i));
		graphics.drawString(font, ECGui.fit(font, text, WIDTH-36), lineX+19, lineY+4, 0xFFFFFF, false);
	}

	private static String count(long count)
	{
		return String.format(Locale.ROOT, "%,d", count);
	}

	private void button(GuiGraphics graphics, int mouseX, int mouseY, int buttonX, int buttonY, int size, Component label)
	{
		ECGui.button(graphics, buttonX, buttonY, size, size, isOver(mouseX, mouseY, buttonX, buttonY, size, size));
		graphics.drawCenteredString(font, label, buttonX+size/2+1, buttonY+3, 0xFFFFFF);
	}

	List<Component> tooltip(int mouseX, int mouseY)
	{
		int index = listIndexAt(mouseX, mouseY);
		if(index>=0)
		{
			Item item = stocked(openRow).get(index);
			return List.of(new ItemStack(item).getHoverName(), Component.translatable(
					"gui.engineered_computation.terminal.stored", count(stored(item))).withStyle(ChatFormatting.GOLD));
		}
		if(isOverList(mouseX, mouseY))
			return List.of();
		int row = pickAt(mouseX, mouseY);
		if(row>=0)
		{
			Item pick = picks.get(tagSlots.get(row));
			List<Component> lines = new ArrayList<>();
			if(pick!=null)
			{
				lines.add(new ItemStack(pick).getHoverName());
				lines.add(Component.translatable("gui.engineered_computation.terminal.stored",
						count(stored(pick))).withStyle(ChatFormatting.GOLD));
			}
			lines.add(Component.translatable("gui.engineered_computation.order.pick.help")
					.withStyle(ChatFormatting.GRAY));
			return lines;
		}
		OrderPlanPayload plan = plan();
		if(plan==null)
			return List.of();
		for(int i = 0; i<Math.min(visibleLines(), plan.lines().size()-scroll); i++)
			if(isOver(mouseX, mouseY, x+8, listY()+1+i*LINE_HEIGHT, 16, 16))
			{
				ItemKey item = plan.lines().get(scroll+i).item();
				return List.of(isTagTarget(item)? Component.literal("#"+offer.outputTag().get().location())
						: shown(item).getHoverName());
			}
		return List.of();
	}

	boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(!isOver(mouseX, mouseY, x, y, WIDTH, height()))
		{
			closed = true;
			return true;
		}
		if(openRow>=0)
		{
			int index = listIndexAt(mouseX, mouseY);
			int row = openRow;
			openRow = -1;
			if(index>=0)
				pick(row, stocked(row).get(index));
			else if(pickAt(mouseX, mouseY)!=row&&pickAt(mouseX, mouseY)>=0)
				open(pickAt(mouseX, mouseY));
			return true;
		}
		int row = pickAt(mouseX, mouseY);
		if(row>=0)
		{
			if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
				pick(row, null);
			else
				open(row);
			return true;
		}
		int step = Screen.hasShiftDown()? 10: 1;
		if(isOver(mouseX, mouseY, x+LESS_X, y+23, 14, 14))
			return step(-step);
		if(isOver(mouseX, mouseY, x+MORE_X, y+23, 14, 14))
			return step(step);
		if(isOverCancel(mouseX, mouseY))
		{
			click();
			closed = true;
			return true;
		}
		if(isOverOrder(mouseX, mouseY))
			place();
		amount.setFocused(amount.isMouseOver(mouseX, mouseY)||amount.isFocused());
		amount.mouseClicked(mouseX, mouseY, button);
		return true;
	}

	private boolean step(int by)
	{
		click();
		amount.setValue(Long.toString(Math.max(1, Math.min(CraftingPlanner.MAX_AMOUNT, amount()+by))));
		return true;
	}

	private void place()
	{
		OrderPlanPayload plan = plan();
		if(plan==null||plan.status()!=StatusCode.OK)
			return;
		click();
		PacketDistributor.sendToServer(request(true));
	}

	private static void click()
	{
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
	}

	private void open(int row)
	{
		openRow = row;
		listScroll = Math.max(0, stocked(row).indexOf(picks.get(tagSlots.get(row))));
		click();
	}

	boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
	{
		int row = pickAt(mouseX, mouseY);
		if(isOverList(mouseX, mouseY))
			listScroll = Math.max(0, listScroll-(int)Math.signum(scrollY));
		else if(row>=0&&scrollY!=0)
			cycle(row, scrollY>0? -1: 1);
		else if(isOver(mouseX, mouseY, x+6, listY(), WIDTH-12, visibleLines()*LINE_HEIGHT))
			scroll = Math.max(0, scroll-(int)Math.signum(scrollY));
		else if(scrollY!=0)
			step((int)Math.signum(scrollY)*(Screen.hasShiftDown()? 10: 1));
		return true;
	}

	boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode==GLFW.GLFW_KEY_ESCAPE)
		{
			if(openRow>=0)
				openRow = -1;
			else
				closed = true;
			return true;
		}
		if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER)
		{
			place();
			return true;
		}
		amount.keyPressed(keyCode, scanCode, modifiers);
		return true;
	}

	boolean charTyped(char codePoint, int modifiers)
	{
		return amount.charTyped(codePoint, modifiers);
	}

	private boolean isOverOrder(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, x+WIDTH/2-BUTTON_WIDTH-3, y+height()-BUTTON_HEIGHT-6, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	private boolean isOverCancel(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, x+WIDTH/2+3, y+height()-BUTTON_HEIGHT-6, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	private static boolean isOver(double mouseX, double mouseY, int left, int top, int width, int height)
	{
		return mouseX>=left&&mouseX<left+width&&mouseY>=top&&mouseY<top+height;
	}
}
