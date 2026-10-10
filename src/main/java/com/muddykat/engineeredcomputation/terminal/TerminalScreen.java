package com.muddykat.engineeredcomputation.terminal;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.crafting.PatternOffer;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ByteSize;
import com.muddykat.engineeredcomputation.storage.ItemKey;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu>
{
	private static final int WIDTH = 195;
	private static final int GRID_WIDTH = TerminalMenu.COLUMNS*18;
	private static final int SCROLLBAR_X = TerminalMenu.GRID_X+GRID_WIDTH+4;
	private static final int SCROLLBAR_WIDTH = 14;
	private static final int THUMB_HEIGHT = 15;
	private static final int SEARCH_Y = 17;
	private static final int GAUGE_HEIGHT = 6;
	private static final int BUTTON_SIZE = 11;
	private static final int RESULT_SIZE = 26;
	private static final int LINK_SIZE = 16;
	private static final int CLEAR_SIZE = 11;
	private static final String[] LINK_ICON = {
			"..####.#",
			".#....##",
			"#....###",
			"#.......",
			".......#",
			"###....#",
			"##....#.",
			"#.####.."};
	private static final int LINK_ON = 0xFF7CE07C;
	private static final int LINK_OFF = 0xFFA0A0A0;
	private static final int MISSING = 0xA08B8B8B;
	private static final String[] CLEAR_ICON = {
			"#...#",
			".#.#.",
			"..#..",
			".#.#.",
			"#...#"};
	private static final int HOVER = 0x80FFFFFF;
	private static final String[] JOBS_ICON = {
			"#.###",
			".....",
			"#.###",
			".....",
			"#.###"};
	private static final String[] SEND_ICON = {
			"..#..",
			"...#.",
			"#####",
			"...#.",
			"..#.."};
	private static final int SLIP_HEIGHT = 18;
	private static final int TOOLTIP_WIDTH = 200;
	private static final int CRAFTABLE_FADE = 0x908B8B8B;

	private final TerminalMenu.Layout layout;
	private final int gridHeight;
	private final int gaugeY;
	private final Map<ItemKey, String> searchNames = new HashMap<>();
	private final List<Map.Entry<ItemKey, Long>> view = new ArrayList<>();
	private final Map<Map.Entry<ItemKey, Long>, PatternOffer> offerEntries = new IdentityHashMap<>();
	@Nullable
	private EditBox search;
	private int seenVersion = -1;
	private String seenQuery = "";
	private int scrollRow;
	private boolean draggingScrollbar;
	@Nullable
	private OrderPopup orderPopup;
	@Nullable
	private JobsPanel jobs;
	@Nullable
	private OutletPicker outletPicker;
	@Nullable
	private Set<Integer> linkDrag;

	public TerminalScreen(TerminalMenu menu, Inventory playerInventory, Component title)
	{
		super(menu, playerInventory, title);
		this.layout = menu.getLayout();
		this.gridHeight = layout.rows()*18;
		this.gaugeY = layout.gridY()+gridHeight+3;
		this.imageWidth = WIDTH;
		this.imageHeight = layout.height();
		this.titleLabelY = 6;
		this.inventoryLabelX = TerminalMenu.GRID_X+1;
		this.inventoryLabelY = layout.inventoryY()-10;
	}

	@Override
	protected void init()
	{
		super.init();
		if(!menu.getFeatures().search())
			return;
		search = new EditBox(font, leftPos+TerminalMenu.GRID_X+3, topPos+SEARCH_Y+2, GRID_WIDTH+SCROLLBAR_WIDTH-2, 10,
				Component.translatable("gui.engineered_computation.terminal.search"));
		search.setBordered(false);
		search.setMaxLength(64);
		search.setTextColor(0xFFFFFF);
		search.setHint(Component.translatable("gui.engineered_computation.terminal.search")
				.withStyle(ChatFormatting.GRAY));
		search.setTooltip(Tooltip.create(Component.translatable("gui.engineered_computation.terminal.search.tooltip")));
		search.setValue(seenQuery);
		addRenderableWidget(search);
	}

	@Override
	protected void containerTick()
	{
		super.containerTick();
		if(orderPopup!=null)
		{
			orderPopup.tick();
			if(orderPopup.isClosed())
				orderPopup = null;
		}
		if(menu.getClientVersion()!=seenVersion||!query().equals(seenQuery))
			rebuildView();
	}

	private String query()
	{
		return search==null? "": search.getValue();
	}

	private void rebuildView()
	{
		seenVersion = menu.getClientVersion();
		seenQuery = query();
		String query = seenQuery.trim().toLowerCase(Locale.ROOT);
		view.clear();
		for(Map.Entry<ItemKey, Long> entry : menu.getClientContents().entrySet())
			if(matches(entry.getKey(), query))
				view.add(entry);
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
		view.sort(Comparator.comparing((Map.Entry<ItemKey, Long> e) -> searchName(e.getKey()))
				.thenComparing(e -> -e.getValue()));
		scrollRow = Math.min(scrollRow, maxScrollRow());
	}

	private boolean matches(ItemKey key, String query)
	{
		if(query.isEmpty())
			return true;
		if(query.startsWith("@"))
			return BuiltInRegistries.ITEM.getKey(key.getItem()).getNamespace().startsWith(query.substring(1));
		return searchName(key).contains(query);
	}

	private String searchName(ItemKey key)
	{
		return searchNames.computeIfAbsent(key,
				k -> k.displayStack().getHoverName().getString().toLowerCase(Locale.ROOT));
	}

	private int maxScrollRow()
	{
		int rows = (view.size()+TerminalMenu.COLUMNS-1)/TerminalMenu.COLUMNS;
		return Math.max(0, rows-layout.rows());
	}

	@Nullable
	private Map.Entry<ItemKey, Long> entryAt(double mouseX, double mouseY)
	{
		int index = gridIndexAt(mouseX, mouseY);
		return index>=0&&index<view.size()? view.get(index): null;
	}

	/** A stored item as drawn in the item grid, and its cell on screen. */
	public record GridIngredient(ItemStack stack, Rect2i area)
	{
	}

	/** The stored item under the mouse, unless a panel covers the grid (for JEI, so its recipe and use keys work). */
	public Optional<GridIngredient> gridIngredientAt(double mouseX, double mouseY)
	{
		Map.Entry<ItemKey, Long> entry = orderPopup!=null||outletPicker!=null||jobs!=null||statusCode()!=null? null:
				entryAt(mouseX, mouseY);
		if(entry==null)
			return Optional.empty();
		int cell = gridIndexAt(mouseX, mouseY)-scrollRow*TerminalMenu.COLUMNS;
		PatternOffer offer = offerEntries.get(entry);
		ItemStack stack = offer!=null? offer.shown(minecraft.level.getGameTime()): entry.getKey().displayStack();
		int x = leftPos+TerminalMenu.GRID_X+cell%TerminalMenu.COLUMNS*18+1;
		int y = topPos+layout.gridY()+cell/TerminalMenu.COLUMNS*18+1;
		return Optional.of(new GridIngredient(stack, new Rect2i(x, y, 16, 16)));
	}

	private int gridIndexAt(double mouseX, double mouseY)
	{
		double x = mouseX-leftPos-TerminalMenu.GRID_X;
		double y = mouseY-topPos-layout.gridY();
		if(x<0||y<0||x>=GRID_WIDTH||y>=gridHeight)
			return -1;
		return (scrollRow+(int)(y/18))*TerminalMenu.COLUMNS+(int)(x/18);
	}

	private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height)
	{
		double relX = mouseX-leftPos-x;
		double relY = mouseY-topPos-y;
		return relX>=0&&relY>=0&&relX<width&&relY<height;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
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
		if(outletPicker!=null&&outletPicker.isClosed())
			outletPicker = null;
		if(outletPicker!=null)
		{
			outletPicker.render(graphics, mouseX, mouseY, partialTick);
			return;
		}
		if(!renderGridTooltip(graphics, mouseX, mouseY)&&!renderWidgetTooltips(graphics, mouseX, mouseY))
			renderTooltip(graphics, mouseX, mouseY);
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
		return slot.index==TerminalMenu.RESULT_SLOT&&!menu.hasLinkedIngredients();
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack)
	{
		List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if(hoveredSlot!=null&&menu.isLinked()&&menu.isGridSlot(hoveredSlot))
			lines.add(Component.translatable("gui.engineered_computation.terminal.linked.stored",
							String.format(Locale.ROOT, "%,d", menu.storedOnClient(stack)))
					.withStyle(ChatFormatting.GOLD));
		else if(hoveredSlot!=null&&isMissing(hoveredSlot))
			lines.add(Component.translatable("gui.engineered_computation.terminal.linked.missing")
					.withStyle(ChatFormatting.RED));
		return lines;
	}

	private boolean renderGridTooltip(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(gridIndexAt(mouseX, mouseY)<0||!menu.getClientStats().conflictingEras().isEmpty())
			return false;
		if(!menu.getCarried().isEmpty()&&menu.getFeatures().requests())
		{
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.teletype.put_away"),
					Component.translatable("gui.engineered_computation.teletype.put_away.detail")
							.withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			return true;
		}
		if(!menu.getCarried().isEmpty())
		{
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.terminal.store_all"),
					Component.translatable("gui.engineered_computation.terminal.store_one")
							.withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			return true;
		}
		Map.Entry<ItemKey, Long> hovered = entryAt(mouseX, mouseY);
		if(hovered==null)
			return false;
		PatternOffer offer = offerEntries.get(hovered);
		if(offer!=null)
		{
			ItemStack shown = offer.shown(minecraft.level.getGameTime());
			List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(shown));
			offer.outputTag().ifPresent(tag -> lines.add(Component.translatable(
					"gui.engineered_computation.terminal.offer.any", "#"+tag.location()).withStyle(ChatFormatting.GRAY)));
			if(!offer.pattern().inputTags().isEmpty())
				lines.add(Component.translatable("gui.engineered_computation.terminal.offer.pick")
						.withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("gui.engineered_computation.terminal.order").withStyle(ChatFormatting.AQUA));
			graphics.renderTooltip(font, lines, shown.getTooltipImage(), shown, mouseX, mouseY);
			return true;
		}
		ItemStack stack = hovered.getKey().displayStack();
		List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
		if(menu.getFeatures().requests())
		{
			lines.add(Component.translatable("gui.engineered_computation.teletype.tallied",
					String.format(Locale.ROOT, "%,d", hovered.getValue())).withStyle(ChatFormatting.GOLD));
			lines.add(menu.isUnreachableOnClient(hovered.getKey())
					? Component.translatable("gui.engineered_computation.teletype.unreachable").withStyle(ChatFormatting.RED)
					: Component.translatable("gui.engineered_computation.teletype.add").withStyle(ChatFormatting.AQUA));
			graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
			return true;
		}
		lines.add(Component.translatable("gui.engineered_computation.terminal.stored",
				String.format(Locale.ROOT, "%,d", hovered.getValue())).withStyle(ChatFormatting.GOLD));
		if(menu.getClientCraftables().contains(hovered.getKey()))
			lines.add(Component.translatable(hovered.getValue()==0? "gui.engineered_computation.terminal.order"
					: "gui.engineered_computation.terminal.order_more").withStyle(ChatFormatting.AQUA));
		graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
		return true;
	}

	private boolean renderWidgetTooltips(GuiGraphics graphics, int mouseX, int mouseY)
	{
		List<Component> lines = null;
		int slipLine = slipLineAt(mouseX, mouseY);
		if(slipLine>=0)
		{
			RequestSlip.Line line = menu.getClientSlip().get(slipLine);
			ItemStack stack = line.key().displayStack();
			List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(stack));
			tooltip.add(Component.translatable("gui.engineered_computation.teletype.on_slip",
					String.format(Locale.ROOT, "%,d", line.amount())).withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.translatable("gui.engineered_computation.teletype.on_slip.detail")
					.withStyle(ChatFormatting.GRAY));
			graphics.renderTooltip(font, tooltip, stack.getTooltipImage(), stack, mouseX, mouseY);
			return true;
		}
		if(isOverSend(mouseX, mouseY))
			lines = List.of(Component.translatable("gui.engineered_computation.teletype.send").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.teletype.send.detail", menu.getClientOutlet())
							.withStyle(ChatFormatting.GRAY));
		else if(isOverOutlet(mouseX, mouseY))
			lines = List.of(Component.translatable("gui.engineered_computation.teletype.outlet",
							menu.getClientOutlet()).withStyle(ChatFormatting.YELLOW),
					Component.translatable(menu.isClientOutletHome()? "gui.engineered_computation.teletype.outlet.home"
							: "gui.engineered_computation.teletype.outlet.away").withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.teletype.outlet.detail")
							.withStyle(ChatFormatting.DARK_GRAY));
		else if(menu.getFeatures().requests()
				&&isOver(mouseX, mouseY, TerminalMenu.GRID_X, gaugeY, GRID_WIDTH+SCROLLBAR_WIDTH+4, GAUGE_HEIGHT))
		{
			TerminalStats stats = menu.getClientStats();
			lines = List.of(
					Component.translatable("gui.engineered_computation.teletype.catalogue").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.teletype.cards", stats.usedBytes(),
							stats.totalBytes()).withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.teletype.kinds", stats.usedTypes())
							.withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.teletype.stations", stats.totalTypes())
							.withStyle(ChatFormatting.GRAY));
		}
		else if(isOver(mouseX, mouseY, TerminalMenu.GRID_X, gaugeY, GRID_WIDTH+SCROLLBAR_WIDTH+4, GAUGE_HEIGHT))
		{
			TerminalStats stats = menu.getClientStats();
			lines = List.of(
					Component.translatable("gui.engineered_computation.terminal.capacity").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.terminal.capacity.bytes",
							ByteSize.format(stats.usedBytes()), ByteSize.format(stats.totalBytes()),
							(int)Math.floor(stats.byteFraction()*100)).withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.terminal.capacity.types",
							stats.usedTypes(), stats.totalTypes()).withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.terminal.capacity.cells",
							stats.cells()).withStyle(ChatFormatting.GRAY));
		}
		else if(menu.getFeatures().orders()&&isOver(mouseX, mouseY, jobsX(), 4, BUTTON_SIZE, BUTTON_SIZE))
			lines = List.of(Component.translatable("gui.engineered_computation.jobs").withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.jobs.detail", menu.getClientOrders().size())
							.withStyle(ChatFormatting.GRAY));
		else if(!menu.getFeatures().orders()&&menu.getClientStats().processing()
				&&isOver(mouseX, mouseY, jobsX()-4, 4, BUTTON_SIZE+4, BUTTON_SIZE))
			lines = List.of(StatusCode.PROCESSING.title().copy().withStyle(ChatFormatting.YELLOW),
					Component.translatable(menu.getFeatures().requests()? "gui.engineered_computation.teletype.coming"
							: "gui.engineered_computation.terminal.processing").withStyle(ChatFormatting.GRAY));
		else if(menu.getFeatures().crafting()&&isOverClear(mouseX, mouseY))
			lines = List.of(Component.translatable("gui.engineered_computation.terminal.clear")
					.withStyle(ChatFormatting.YELLOW));
		else if(menu.getFeatures().crafting()&&isOverLink(mouseX, mouseY))
		{
			String key = "gui.engineered_computation.terminal.linked."+(menu.isLinked()? "on": "off");
			lines = List.of(Component.translatable(key).withStyle(ChatFormatting.YELLOW),
					Component.translatable(key+".detail").withStyle(ChatFormatting.GRAY));
		}
		if(lines==null)
			return false;
		graphics.renderTooltip(font, lines.stream().flatMap(line -> font.split(line, TOOLTIP_WIDTH).stream()).toList(),
				mouseX, mouseY);
		return true;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos;
		int y = topPos;
		ECGui.panel(graphics, x, y, imageWidth, imageHeight);

		if(search!=null)
		{
			int searchX = x+TerminalMenu.GRID_X;
			graphics.fill(searchX, y+SEARCH_Y, searchX+GRID_WIDTH+SCROLLBAR_WIDTH+4, y+SEARCH_Y+13, 0xFFA0A0A0);
			graphics.fill(searchX+1, y+SEARCH_Y+1, searchX+GRID_WIDTH+SCROLLBAR_WIDTH+3, y+SEARCH_Y+12, 0xFF000000);
		}

		if(menu.getFeatures().orders())
		{
			ECGui.button(graphics, x+jobsX(), y+4, BUTTON_SIZE, BUTTON_SIZE,
					jobs!=null||isOver(mouseX, mouseY, jobsX(), 4, BUTTON_SIZE, BUTTON_SIZE));
			ECGui.icon(graphics, x+jobsX()+3, y+7, JOBS_ICON,
					menu.getClientOrders().isEmpty()? 0xFFFFFFFF: 0xFF000000|StatusCode.PROCESSING.colour());
		}
		else if(menu.getClientStats().processing())
			graphics.drawString(font, "102", x+jobsX()-4, y+6, StatusCode.PROCESSING.colour(), true);

		for(int row = 0; row<layout.rows(); row++)
			for(int col = 0; col<TerminalMenu.COLUMNS; col++)
				ECGui.slot(graphics, x+TerminalMenu.GRID_X+col*18, y+layout.gridY()+row*18);
		int trackX = x+SCROLLBAR_X;
		int trackY = y+layout.gridY();
		ECGui.sunken(graphics, trackX, trackY, SCROLLBAR_WIDTH, gridHeight);
		graphics.blitSprite(maxScrollRow()>0? ECGui.SCROLLER: ECGui.SCROLLER_DISABLED,
				trackX+1, trackY+1+thumbOffset(), 12, THUMB_HEIGHT);
		ECGui.gauge(graphics, x+TerminalMenu.GRID_X, y+gaugeY, GRID_WIDTH+SCROLLBAR_WIDTH+4, GAUGE_HEIGHT,
				menu.getClientStats().byteFraction());

		for(int i = 0; i<menu.slots.size(); i++)
		{
			Slot slot = menu.slots.get(i);
			if(menu.getFeatures().crafting()&&i==TerminalMenu.RESULT_SLOT)
				ECGui.sunken(graphics, x+TerminalMenu.RESULT_X, y+slot.y-5, RESULT_SIZE, RESULT_SIZE);
			else
				ECGui.slot(graphics, x+slot.x-1, y+slot.y-1);
		}
		if(menu.getFeatures().crafting())
			renderCraftingExtras(graphics, mouseX, mouseY);
		if(menu.getFeatures().requests())
			renderSlip(graphics, mouseX, mouseY);

		renderGrid(graphics, mouseX, mouseY);
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
				ItemStack stack = lines.get(i).key().displayStack();
				graphics.renderItem(stack, sx+1, slipY+1);
				renderCount(graphics, lines.get(i).amount(), sx+1, slipY+1);
			}
			if(i==hovered)
				graphics.fill(sx+1, slipY+1, sx+17, slipY+17, 300, HOVER);
		}
		int sendX = leftPos+SCROLLBAR_X;
		ECGui.button(graphics, sendX, slipY, SCROLLBAR_WIDTH, SLIP_HEIGHT, isOverSend(mouseX, mouseY));
		ECGui.icon(graphics, sendX+(SCROLLBAR_WIDTH-5)/2, slipY+(SLIP_HEIGHT-5)/2, SEND_ICON,
				lines.isEmpty()? 0xFF8B8B8B: 0xFFFFFFFF);
	}

	private int slipLineAt(double mouseX, double mouseY)
	{
		if(!menu.getFeatures().requests()
				||!isOver(mouseX, mouseY, TerminalMenu.GRID_X, layout.craftingY(), GRID_WIDTH, SLIP_HEIGHT))
			return -1;
		int line = (int)(mouseX-leftPos-TerminalMenu.GRID_X)/18;
		return line<menu.getClientSlip().size()? line: -1;
	}

	private boolean isOverSend(double mouseX, double mouseY)
	{
		return menu.getFeatures().requests()
				&&isOver(mouseX, mouseY, SCROLLBAR_X, layout.craftingY(), SCROLLBAR_WIDTH, SLIP_HEIGHT);
	}

	private Component outletLabel()
	{
		return Component.translatable("gui.engineered_computation.teletype.to", menu.getClientOutlet());
	}

	private boolean isOverOutlet(double mouseX, double mouseY)
	{
		if(!menu.getFeatures().requests())
			return false;
		int width = font.width(outletLabel());
		return isOver(mouseX, mouseY, imageWidth-8-width, inventoryLabelY-1, width, font.lineHeight+1);
	}

	private void renderCraftingExtras(GuiGraphics graphics, int mouseX, int mouseY)
	{
		int craftingY = topPos+layout.craftingY();
		int arrowX = leftPos+TerminalMenu.CRAFT_X+3*18+6;
		int arrowY = craftingY+26;
		graphics.fill(arrowX, arrowY-2, arrowX+15, arrowY+2, 0xFF8B8B8B);
		for(int step = 0; step<7; step++)
			graphics.fill(arrowX+15+step, arrowY-7+step, arrowX+16+step, arrowY+7-step, 0xFF8B8B8B);

		int clearX = leftPos+TerminalMenu.CLEAR_X;
		ECGui.button(graphics, clearX, craftingY, CLEAR_SIZE, CLEAR_SIZE, isOverClear(mouseX, mouseY));
		ECGui.icon(graphics, clearX+3, craftingY+3, CLEAR_ICON, 0xFFFFFFFF);

		int toggleX = leftPos+TerminalMenu.LINK_X;
		int toggleY = craftingY+linkY();
		ECGui.button(graphics, toggleX, toggleY, LINK_SIZE, LINK_SIZE, isOverLink(mouseX, mouseY));
		ECGui.icon(graphics, toggleX+4, toggleY+4, LINK_ICON, menu.isLinked()? LINK_ON: LINK_OFF);
	}

	private static int linkY()
	{
		return 27-LINK_SIZE/2;
	}

	private boolean isOverLink(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, TerminalMenu.LINK_X, layout.craftingY()+linkY(), LINK_SIZE, LINK_SIZE);
	}

	private boolean isOverClear(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, TerminalMenu.CLEAR_X, layout.craftingY(), CLEAR_SIZE, CLEAR_SIZE);
	}

	private int jobsX()
	{
		return WIDTH-7-BUTTON_SIZE;
	}

	private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY)
	{
		StatusCode code = statusCode();
		if(code!=null)
		{
			renderCode(graphics, code);
			return;
		}
		int hoveredIndex = gridIndexAt(mouseX, mouseY);
		int first = scrollRow*TerminalMenu.COLUMNS;
		for(int i = 0; i<layout.rows()*TerminalMenu.COLUMNS; i++)
		{
			int sx = leftPos+TerminalMenu.GRID_X+(i%TerminalMenu.COLUMNS)*18+1;
			int sy = topPos+layout.gridY()+(i/TerminalMenu.COLUMNS)*18+1;
			if(first+i<view.size())
			{
				Map.Entry<ItemKey, Long> entry = view.get(first+i);
				PatternOffer offer = offerEntries.get(entry);
				ItemStack stack = offer!=null? offer.shown(minecraft.level.getGameTime()): entry.getKey().displayStack();
				graphics.renderItem(stack, sx, sy);
				graphics.renderItemDecorations(font, stack, sx, sy, "");
				if(entry.getValue()==0)
				{
					graphics.fill(RenderType.guiOverlay(), sx, sy, sx+16, sy+16, CRAFTABLE_FADE);
					renderSmall(graphics, Component.translatable("gui.engineered_computation.terminal.craft").getString(), sx+16,
							sy+16, StatusCode.PROCESSING.colour());
					if(offer!=null)
						ECGui.tagBadge(graphics, font, sx, sy);
				}
				else
				{
					renderCount(graphics, entry.getValue(), sx, sy);
					if(menu.getClientCraftables().contains(entry.getKey()))
						renderSmall(graphics, "+", sx+5, sy+7, StatusCode.PROCESSING.colour());
					if(menu.isUnreachableOnClient(entry.getKey()))
						graphics.fill(RenderType.guiOverlay(), sx, sy, sx+16, sy+16, CRAFTABLE_FADE);
				}
			}
			if(first+i==hoveredIndex)
				graphics.fill(sx, sy, sx+16, sy+16, 300, HOVER);
		}
	}

	@Nullable
	private StatusCode statusCode()
	{
		TerminalStats stats = menu.getClientStats();
		if(stats.tooLarge())
			return StatusCode.CONTENT_TOO_LARGE;
		if(stats.noOutlet())
			return StatusCode.FAILED_DEPENDENCY;
		if(!stats.conflictingEras().isEmpty())
			return StatusCode.CONFLICT;
		if(!view.isEmpty())
			return null;
		if(!stats.erasWithoutProcessor().isEmpty())
			return StatusCode.FAILED_DEPENDENCY;
		if(stats.cells()==0)
			return StatusCode.SERVICE_UNAVAILABLE;
		return seenQuery.isBlank()? StatusCode.NO_CONTENT: StatusCode.NOT_FOUND;
	}

	private void renderCode(GuiGraphics graphics, StatusCode code)
	{
		Component text = code.title();
		int centreX = leftPos+TerminalMenu.GRID_X+GRID_WIDTH/2;
		int centreY = topPos+layout.gridY()+gridHeight/2;
		int halfWidth = font.width(text)/2+6;
		graphics.fill(centreX-halfWidth-1, centreY-9, centreX+halfWidth+1, centreY+9, 0xFF000000);
		graphics.fill(centreX-halfWidth, centreY-8, centreX+halfWidth, centreY+8, 0xFF101814);
		graphics.drawCenteredString(font, text, centreX, centreY-4, code.colour());
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

	private void renderSmall(GuiGraphics graphics, String text, int right, int bottom, int colour)
	{
		float scale = Math.min(0.666F, 16F/font.width(text));
		graphics.pose().pushPose();
		graphics.pose().translate(right, bottom, 250);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, -font.width(text), -font.lineHeight+1, colour, true);
		graphics.pose().popPose();
	}

	private void renderCount(GuiGraphics graphics, long count, int x, int y)
	{
		String text = formatCount(count);
		float scale = 0.666F;
		graphics.pose().pushPose();
		graphics.pose().translate(x+16, y+16, 250);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, -font.width(text), -font.lineHeight+1, 0xFFFFFF, true);
		graphics.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
		TerminalStats stats = menu.getClientStats();
		Component usage = menu.getFeatures().requests()? outletLabel()
				: Component.translatable("gui.engineered_computation.terminal.bytes", ByteSize.format(stats.usedBytes()),
						ByteSize.format(stats.totalBytes()));
		graphics.drawString(font, usage, imageWidth-8-font.width(usage), inventoryLabelY,
				isOverOutlet(mouseX, mouseY)? 0x2050A0: ECGui.LABEL, false);
	}

	static String formatCount(long count)
	{
		if(count<1000)
			return Long.toString(count);
		String[] units = {"k", "M", "G", "T", "P", "E"};
		double value = count;
		int unit = -1;
		while(value>=1000&&unit<units.length-1)
		{
			value /= 1000;
			unit++;
		}
		return value<10
				? String.format(Locale.ROOT, "%.1f%s", Math.floor(value*10)/10, units[unit])
				: (long)value+units[unit];
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(orderPopup!=null)
			return orderPopup.mouseClicked(mouseX, mouseY, button);
		if(outletPicker!=null)
			return outletPicker.mouseClicked(mouseX, mouseY, button);
		Slot gridSlot = linkedGridSlotAt(mouseX, mouseY);
		if(gridSlot!=null&&!menu.getCarried().isEmpty()
				&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			linkDrag = new HashSet<>();
			link(gridSlot);
			return true;
		}
		if(menu.getFeatures().orders()&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				&&isOver(mouseX, mouseY, jobsX(), 4, BUTTON_SIZE, BUTTON_SIZE))
		{
			jobs = jobs==null? new JobsPanel(menu, font, leftPos+TerminalMenu.GRID_X, topPos+layout.gridY(),
					GRID_WIDTH+SCROLLBAR_WIDTH+4, gridHeight): null;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		if(jobs!=null&&jobs.mouseClicked(mouseX, mouseY))
			return true;
		Map.Entry<ItemKey, Long> craftable = entryAt(mouseX, mouseY);
		PatternOffer offer = craftable==null? null: offerEntries.get(craftable);
		if(offer!=null&&menu.getCarried().isEmpty()&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				||button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
		{
			if(search!=null)
				search.setFocused(false);
			orderPopup = new OrderPopup(menu, font, offer, leftPos+(imageWidth-OrderPopup.WIDTH)/2,
					topPos+layout.gridY()-8);
			return true;
		}
		if(craftable!=null&&menu.getCarried().isEmpty()&&menu.getClientCraftables().contains(craftable.getKey())
				&&(craftable.getValue()==0&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				||hasControlDown()&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
		{
			if(search!=null)
				search.setFocused(false);
			orderPopup = new OrderPopup(menu, font, craftable.getKey(), leftPos+(imageWidth-OrderPopup.WIDTH)/2,
					topPos+layout.gridY()-8);
			return true;
		}
		if(search!=null&&!search.isMouseOver(mouseX, mouseY))
			search.setFocused(false);
		if(menu.getFeatures().crafting()&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT
				&&(isOverLink(mouseX, mouseY)||isOverClear(mouseX, mouseY)))
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
					isOverLink(mouseX, mouseY)? TerminalMenu.TOGGLE_LINKED: TerminalMenu.CLEAR_GRID);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		if(menu.getFeatures().requests()&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverOutlet(mouseX, mouseY))
		{
			if(search!=null)
				search.setFocused(false);
			outletPicker = new OutletPicker(menu, font, leftPos+TerminalMenu.GRID_X, topPos+inventoryLabelY+10);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		if(menu.getFeatures().requests()&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			boolean left = button==GLFW.GLFW_MOUSE_BUTTON_LEFT;
			int slipLine = slipLineAt(mouseX, mouseY);
			int id = slipLine>=0? (left? TerminalMenu.SLIP_REMOVE: TerminalMenu.SLIP_LOWER)+slipLine
					: isOverSend(mouseX, mouseY)&&left? TerminalMenu.SEND_SLIP: -1;
			if(id>=0)
			{
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
				return true;
			}
		}
		if(isOver(mouseX, mouseY, SCROLLBAR_X, layout.gridY(), SCROLLBAR_WIDTH, gridHeight))
		{
			draggingScrollbar = true;
			scrollToMouse(mouseY);
			return true;
		}
		if(gridIndexAt(mouseX, mouseY)>=0&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			boolean left = button==GLFW.GLFW_MOUSE_BUTTON_LEFT;
			Map.Entry<ItemKey, Long> entry = entryAt(mouseX, mouseY);
			TerminalAction action;
			if(!menu.getCarried().isEmpty())
				action = left? TerminalAction.INSERT_ALL: TerminalAction.INSERT_ONE;
			else if(entry==null)
				return true;
			else if(left)
				action = hasShiftDown()? TerminalAction.QUICK_MOVE: TerminalAction.TAKE_STACK;
			else
				action = TerminalAction.TAKE_HALF;
			PacketDistributor.sendToServer(new TerminalClickPayload(
					menu.containerId, Optional.ofNullable(entry).map(Map.Entry::getKey), action));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers)
	{
		if(orderPopup!=null)
			return orderPopup.charTyped(codePoint, modifiers);
		if(outletPicker!=null)
			return outletPicker.charTyped(codePoint, modifiers);
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(draggingScrollbar)
		{
			scrollToMouse(mouseY);
			return true;
		}
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
		draggingScrollbar = false;
		if(linkDrag!=null)
		{
			linkDrag = null;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(orderPopup!=null)
			return orderPopup.mouseScrolled(mouseX, mouseY, scrollY);
		if(outletPicker!=null)
			return outletPicker.mouseScrolled(mouseX, mouseY, scrollY);
		if(isOverOutlet(mouseX, mouseY)&&scrollY!=0)
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
					scrollY>0? TerminalMenu.PREVIOUS_OUTLET: TerminalMenu.NEXT_OUTLET);
			return true;
		}
		if(jobs!=null&&jobs.isOver(mouseX, mouseY))
		{
			jobs.mouseScrolled(scrollY);
			return true;
		}
		if(scrollY!=0)
		{
			scrollRow = Math.clamp(scrollRow-(int)Math.signum(scrollY), 0, maxScrollRow());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(orderPopup!=null)
			return orderPopup.keyPressed(keyCode, scanCode, modifiers);
		if(outletPicker!=null)
			return outletPicker.keyPressed(keyCode, scanCode, modifiers);
		if(jobs!=null&&keyCode==GLFW.GLFW_KEY_ESCAPE)
		{
			jobs = null;
			return true;
		}
		if(search!=null&&search.isFocused()&&keyCode!=GLFW.GLFW_KEY_ESCAPE)
		{
			search.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private int thumbOffset()
	{
		int max = maxScrollRow();
		return max==0? 0: scrollRow*(gridHeight-2-THUMB_HEIGHT)/max;
	}

	private void scrollToMouse(double mouseY)
	{
		double fraction = (mouseY-topPos-layout.gridY()-1-THUMB_HEIGHT/2.0)/(gridHeight-2-THUMB_HEIGHT);
		scrollRow = Math.clamp(Math.round(fraction*maxScrollRow()), 0, maxScrollRow());
	}
}
