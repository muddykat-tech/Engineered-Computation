package com.muddykat.engineeredcomputation.client.screen.terminal;

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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.ECClientConfig;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.gui.GuiIcon;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.ByteSize;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.ItemSearch;
import com.muddykat.engineeredcomputation.terminal.TerminalAction;
import com.muddykat.engineeredcomputation.terminal.TerminalClickPayload;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;
import com.muddykat.engineeredcomputation.terminal.TerminalStats;

public abstract class TerminalScreen<M extends TerminalMenu> extends AbstractContainerScreen<M>
{
	protected static final int WIDTH = 195;
	protected static final int GRID_WIDTH = TerminalMenu.COLUMNS*18;
	protected static final int SCROLLBAR_X = TerminalMenu.GRID_X+GRID_WIDTH+4;
	protected static final int SCROLLBAR_WIDTH = 14;
	protected static final int BUTTON_SIZE = 11;
	protected static final int HOVER = 0x80FFFFFF;
	protected static final int CRAFTABLE_FADE = 0x908B8B8B;
	private static final int THUMB_HEIGHT = 15;
	private static final int SEARCH_Y = 17;
	private static final int BAR_WIDTH = GRID_WIDTH+SCROLLBAR_WIDTH+4;
	private static final int TOOL_GAP = 1;
	private static final int GAUGE_HEIGHT = 6;
	private static final int TOOLTIP_WIDTH = 200;

	protected final TerminalMenu.Layout layout;
	protected final int gridHeight;
	private final int gaugeY;
	private final Map<ItemKey, String> searchNames = new HashMap<>();
	private final List<Map.Entry<ItemKey, Long>> view = new ArrayList<>();
	@Nullable
	protected EditBox search;
	private int seenVersion = -1;
	private String seenQuery = "";
	private int scrollRow;
	private boolean draggingScrollbar;

	protected TerminalScreen(M menu, Inventory playerInventory, Component title)
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
		search = new EditBox(font, leftPos+TerminalMenu.GRID_X+3, topPos+SEARCH_Y+2, searchWidth()-6, 10,
				Component.translatable("gui.engineered_computation.terminal.search"));
		search.setBordered(false);
		search.setMaxLength(64);
		search.setTextColor(0xFFFFFF);
		search.setHint(Component.translatable("gui.engineered_computation.terminal.search")
				.withStyle(ChatFormatting.GRAY));
		search.setValue(seenQuery);
		addRenderableWidget(search);
	}

	@Override
	protected void containerTick()
	{
		super.containerTick();
		if(viewChanged())
			rebuildView();
	}

	protected boolean viewChanged()
	{
		return menu.getClientVersion()!=seenVersion||!query().equals(seenQuery);
	}

	private String query()
	{
		return search==null? "": search.getValue();
	}

	protected boolean isQueryBlank()
	{
		return seenQuery.isBlank();
	}

	private void rebuildView()
	{
		seenVersion = menu.getClientVersion();
		seenQuery = query();
		view.clear();
		collectEntries(view, seenQuery.trim().toLowerCase(Locale.ROOT));
		Comparator<Map.Entry<ItemKey, Long>> byName = Comparator.comparing(e -> searchName(e.getKey()));
		Comparator<Map.Entry<ItemKey, Long>> order = switch(ECClientConfig.terminalSort())
		{
			case NAME -> byName;
			case COUNT -> Comparator.<Map.Entry<ItemKey, Long>>comparingLong(Map.Entry::getValue).thenComparing(byName);
		};
		view.sort(Comparator.comparingInt(this::placement)
				.thenComparing(ECClientConfig.terminalDescending()? order.reversed(): order)
				.thenComparingInt(this::sortGroup)
				.thenComparing(e -> -e.getValue()));
		scrollRow = Math.min(scrollRow, maxScrollRow());
	}

	protected void collectEntries(List<Map.Entry<ItemKey, Long>> view, String query)
	{
		for(Map.Entry<ItemKey, Long> entry : menu.getClientContents().entrySet())
			if(matches(entry.getKey(), query)&&shows(entry.getKey()))
				view.add(entry);
	}

	protected boolean shows(ItemKey key)
	{
		return true;
	}

	/** Sorted before anything else: lower comes first, whatever the sort order. */
	protected int placement(Map.Entry<ItemKey, Long> entry)
	{
		return 0;
	}

	protected int sortGroup(Map.Entry<ItemKey, Long> entry)
	{
		return 0;
	}

	protected boolean matches(ItemKey key, String query)
	{
		return ItemSearch.matches(searchName(key), BuiltInRegistries.ITEM.getKey(key.getItem()), query);
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
	protected Map.Entry<ItemKey, Long> entryAt(double mouseX, double mouseY)
	{
		int index = gridIndexAt(mouseX, mouseY);
		return index>=0&&index<view.size()? view.get(index): null;
	}

	protected ItemStack shownStack(Map.Entry<ItemKey, Long> entry)
	{
		return entry.getKey().displayStack();
	}

	public record GridIngredient(ItemStack stack, Rect2i area)
	{
	}

	/** Empty while a panel covers the grid; lets JEI's recipe and use keys work on grid items. */
	public Optional<GridIngredient> gridIngredientAt(double mouseX, double mouseY)
	{
		Map.Entry<ItemKey, Long> entry = gridCovered(mouseX, mouseY)||statusCode()!=null? null: entryAt(mouseX, mouseY);
		if(entry==null)
			return Optional.empty();
		int cell = gridIndexAt(mouseX, mouseY)-scrollRow*TerminalMenu.COLUMNS;
		int x = leftPos+TerminalMenu.GRID_X+cell%TerminalMenu.COLUMNS*18+1;
		int y = topPos+layout.gridY()+cell/TerminalMenu.COLUMNS*18+1;
		return Optional.of(new GridIngredient(shownStack(entry), new Rect2i(x, y, 16, 16)));
	}

	protected boolean gridCovered(double mouseX, double mouseY)
	{
		return false;
	}

	public List<Rect2i> extraAreas()
	{
		return List.of();
	}

	private int gridIndexAt(double mouseX, double mouseY)
	{
		double x = mouseX-leftPos-TerminalMenu.GRID_X;
		double y = mouseY-topPos-layout.gridY();
		if(x<0||y<0||x>=GRID_WIDTH||y>=gridHeight)
			return -1;
		return (scrollRow+(int)(y/18))*TerminalMenu.COLUMNS+(int)(x/18);
	}

	protected boolean isOverGrid(double mouseX, double mouseY)
	{
		return gridIndexAt(mouseX, mouseY)>=0;
	}

	protected boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height)
	{
		double relX = mouseX-leftPos-x;
		double relY = mouseY-topPos-y;
		return relX>=0&&relY>=0&&relX<width&&relY<height;
	}

	protected boolean isOverGauge(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, TerminalMenu.GRID_X, gaugeY, GRID_WIDTH+SCROLLBAR_WIDTH+4, GAUGE_HEIGHT);
	}

	protected boolean isOverCorner(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, cornerX(), 4, BUTTON_SIZE, BUTTON_SIZE);
	}

	protected int cornerX()
	{
		return WIDTH-7-BUTTON_SIZE;
	}

	protected int processingX()
	{
		return cornerX()-4;
	}

	/** Terminals that autocraft get a button choosing whether craftable items are shown. */
	protected boolean filtersCraftables()
	{
		return false;
	}

	/** Terminals that list what Storage Analyzers report get a button showing or hiding it. */
	protected boolean listsRemote()
	{
		return false;
	}

	private int tools()
	{
		return 2+(filtersCraftables()? 1: 0)+(listsRemote()? 1: 0);
	}

	private int searchWidth()
	{
		return BAR_WIDTH-tools()*(BUTTON_SIZE+TOOL_GAP);
	}

	/** Beside the search field, or on the title row before the corner when there's no search. */
	private int toolX(int tool)
	{
		int first = menu.getFeatures().search()? TerminalMenu.GRID_X+searchWidth()+TOOL_GAP
				: processingX()-tools()*(BUTTON_SIZE+TOOL_GAP)-1;
		return first+tool*(BUTTON_SIZE+TOOL_GAP);
	}

	private int toolY()
	{
		return menu.getFeatures().search()? SEARCH_Y+1: 4;
	}

	private boolean isOverTool(double mouseX, double mouseY, int tool)
	{
		return isOver(mouseX, mouseY, toolX(tool), toolY(), BUTTON_SIZE, BUTTON_SIZE);
	}

	private boolean isOverCraftables(double mouseX, double mouseY)
	{
		return filtersCraftables()&&isOverTool(mouseX, mouseY, 0);
	}

	private boolean isOverRemote(double mouseX, double mouseY)
	{
		return listsRemote()&&isOverTool(mouseX, mouseY, filtersCraftables()? 1: 0);
	}

	private boolean isOverSortBy(double mouseX, double mouseY)
	{
		return isOverTool(mouseX, mouseY, tools()-2);
	}

	private boolean isOverSortOrder(double mouseX, double mouseY)
	{
		return isOverTool(mouseX, mouseY, tools()-1);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		renderPopups(graphics, mouseX, mouseY, partialTick);
	}

	protected void renderPopups(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		renderTooltips(graphics, mouseX, mouseY);
	}

	protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(!renderGridTooltip(graphics, mouseX, mouseY)&&!renderWidgetTooltips(graphics, mouseX, mouseY))
			renderTooltip(graphics, mouseX, mouseY);
	}

	private boolean renderGridTooltip(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(!isOverGrid(mouseX, mouseY)||!menu.getClientStats().conflictingEras().isEmpty())
			return false;
		if(!menu.getCarried().isEmpty())
		{
			graphics.renderComponentTooltip(font, carriedTooltip(), mouseX, mouseY);
			return true;
		}
		Map.Entry<ItemKey, Long> hovered = entryAt(mouseX, mouseY);
		if(hovered==null)
			return false;
		ItemStack stack = shownStack(hovered);
		List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
		addEntryLines(lines, hovered);
		graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
		return true;
	}

	protected List<Component> carriedTooltip()
	{
		return List.of(Component.translatable("gui.engineered_computation.terminal.store_all"),
				Component.translatable("gui.engineered_computation.terminal.store_one").withStyle(ChatFormatting.GRAY));
	}

	protected void addEntryLines(List<Component> lines, Map.Entry<ItemKey, Long> entry)
	{
		lines.add(Component.translatable("gui.engineered_computation.terminal.stored", number(entry.getValue()))
				.withStyle(ChatFormatting.GOLD));
	}

	protected boolean renderWidgetTooltips(GuiGraphics graphics, int mouseX, int mouseY)
	{
		List<Component> lines = widgetTooltip(mouseX, mouseY);
		if(lines==null)
			return false;
		graphics.renderTooltip(font, lines.stream().flatMap(line -> font.split(line, TOOLTIP_WIDTH).stream()).toList(),
				mouseX, mouseY);
		return true;
	}

	@Nullable
	protected List<Component> widgetTooltip(double mouseX, double mouseY)
	{
		if(isOverCraftables(mouseX, mouseY))
			return List.of(Component.translatable(switch(ECClientConfig.terminalCraftables())
			{
				case BOTH -> "gui.engineered_computation.terminal.craftables.both";
				case ONLY -> "gui.engineered_computation.terminal.craftables.only";
				case HIDDEN -> "gui.engineered_computation.terminal.craftables.hidden";
			}));
		if(isOverRemote(mouseX, mouseY))
			return List.of(Component.translatable("gui.engineered_computation.terminal.remote."
					+ECClientConfig.terminalRemote().name().toLowerCase(Locale.ROOT)));
		if(isOverSortBy(mouseX, mouseY))
			return List.of(Component.translatable(ECClientConfig.terminalSort()==ECClientConfig.TerminalSort.NAME
					? "gui.engineered_computation.terminal.sort.name": "gui.engineered_computation.terminal.sort.count"));
		if(isOverSortOrder(mouseX, mouseY))
			return List.of(Component.translatable(ECClientConfig.terminalDescending()
					? "gui.engineered_computation.terminal.sort.descending"
					: "gui.engineered_computation.terminal.sort.ascending"));
		// Only while hovering an empty field, as a focused field's tooltip covers the grid while typing
		if(search!=null&&search.isMouseOver(mouseX, mouseY)&&search.getValue().isEmpty())
			return List.of(Component.translatable("gui.engineered_computation.terminal.search.tooltip"));
		if(isOverGauge(mouseX, mouseY))
			return gaugeTooltip();
		if(showsProcessing()&&menu.getClientStats().processing()
				&&isOver(mouseX, mouseY, processingX(), 4, BUTTON_SIZE+4, BUTTON_SIZE))
			return List.of(StatusCode.PROCESSING.title().copy().withStyle(ChatFormatting.YELLOW),
					Component.translatable(processingDetail()).withStyle(ChatFormatting.GRAY));
		return null;
	}

	protected List<Component> gaugeTooltip()
	{
		TerminalStats stats = menu.getClientStats();
		List<Component> lines = new ArrayList<>(List.of(
				Component.translatable("gui.engineered_computation.terminal.capacity").withStyle(ChatFormatting.YELLOW),
				Component.translatable("gui.engineered_computation.terminal.capacity.bytes",
						ByteSize.format(stats.usedBytes()), ByteSize.format(stats.totalBytes()),
						(int)Math.floor(stats.byteFraction()*100)).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.engineered_computation.terminal.capacity.types",
						stats.usedTypes(), stats.totalTypes()).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.engineered_computation.terminal.capacity.cells",
						stats.cells()).withStyle(ChatFormatting.GRAY)));
		if(stats.powerEnabled())
		{
			lines.add(Component.translatable("gui.engineered_computation.terminal.power", number(stats.powerCost()))
					.withStyle(stats.unpowered()? ChatFormatting.RED: ChatFormatting.GRAY));
			lines.add(Component.translatable("gui.engineered_computation.terminal.power.stored",
					number(stats.powerStored()), number(stats.powerCapacity())).withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}

	protected boolean showsProcessing()
	{
		return true;
	}

	protected String processingDetail()
	{
		return "gui.engineered_computation.terminal.processing";
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
			graphics.fill(searchX, y+SEARCH_Y, searchX+searchWidth(), y+SEARCH_Y+13, 0xFFA0A0A0);
			graphics.fill(searchX+1, y+SEARCH_Y+1, searchX+searchWidth()-1, y+SEARCH_Y+12, 0xFF000000);
		}
		renderTools(graphics, mouseX, mouseY);
		renderCorner(graphics, mouseX, mouseY);

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
			renderSlotBackground(graphics, menu.slots.get(i), i);
		renderExtras(graphics, mouseX, mouseY);
		renderGrid(graphics, mouseX, mouseY);
	}

	private void renderTools(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(filtersCraftables())
			renderTool(graphics, 0, isOverCraftables(mouseX, mouseY), switch(ECClientConfig.terminalCraftables())
			{
				case BOTH -> GuiIcon.CRAFTS_BOTH;
				case ONLY -> GuiIcon.CRAFTS_ONLY;
				case HIDDEN -> GuiIcon.CRAFTS_HIDDEN;
			});
		if(listsRemote())
			renderTool(graphics, filtersCraftables()? 1: 0, isOverRemote(mouseX, mouseY),
					switch(ECClientConfig.terminalRemote())
					{
						case MIXED -> GuiIcon.REMOTE_SHOWN;
						case TOP -> GuiIcon.REMOTE_TOP;
						case BOTTOM -> GuiIcon.REMOTE_BOTTOM;
						case HIDDEN -> GuiIcon.REMOTE_HIDDEN;
					});
		renderTool(graphics, tools()-2, isOverSortBy(mouseX, mouseY),
				ECClientConfig.terminalSort()==ECClientConfig.TerminalSort.NAME? GuiIcon.SORT_NAME: GuiIcon.SORT_COUNT);
		renderTool(graphics, tools()-1, isOverSortOrder(mouseX, mouseY),
				ECClientConfig.terminalDescending()? GuiIcon.DESCENDING: GuiIcon.ASCENDING);
	}

	private void renderTool(GuiGraphics graphics, int tool, boolean hovered, GuiIcon icon)
	{
		int x = leftPos+toolX(tool);
		int y = topPos+toolY();
		ECGui.button(graphics, x, y, BUTTON_SIZE, BUTTON_SIZE, hovered);
		icon.draw(graphics, x+3, y+3);
	}

	protected void renderCorner(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(menu.getClientStats().processing())
			graphics.drawString(font, "102", leftPos+processingX(), topPos+6, StatusCode.PROCESSING.colour(), true);
	}

	protected void renderSlotBackground(GuiGraphics graphics, Slot slot, int index)
	{
		ECGui.slot(graphics, leftPos+slot.x-1, topPos+slot.y-1);
	}

	protected void renderExtras(GuiGraphics graphics, int mouseX, int mouseY)
	{
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
				ItemStack stack = shownStack(entry);
				graphics.renderItem(stack, sx, sy);
				graphics.renderItemDecorations(font, stack, sx, sy, "");
				renderEntry(graphics, entry, sx, sy);
			}
			if(first+i==hoveredIndex)
				graphics.fill(sx, sy, sx+16, sy+16, 300, HOVER);
		}
	}

	protected void renderEntry(GuiGraphics graphics, Map.Entry<ItemKey, Long> entry, int x, int y)
	{
		renderCount(graphics, entry.getValue(), x, y);
	}

	@Nullable
	protected StatusCode statusCode()
	{
		TerminalStats stats = menu.getClientStats();
		if(stats.tooLarge())
			return StatusCode.CONTENT_TOO_LARGE;
		if(stats.noOutlet())
			return StatusCode.FAILED_DEPENDENCY;
		if(!stats.conflictingEras().isEmpty())
			return StatusCode.CONFLICT;
		if(stats.unpowered())
			return StatusCode.PAYMENT_REQUIRED;
		if(!view.isEmpty())
			return null;
		if(!stats.erasWithoutProcessor().isEmpty())
			return StatusCode.FAILED_DEPENDENCY;
		if(stats.cells()==0)
			return StatusCode.SERVICE_UNAVAILABLE;
		return isQueryBlank()? StatusCode.NO_CONTENT: StatusCode.NOT_FOUND;
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

	protected void renderSmall(GuiGraphics graphics, String text, int right, int bottom, int colour)
	{
		float scale = Math.min(0.666F, 16F/font.width(text));
		graphics.pose().pushPose();
		graphics.pose().translate(right, bottom, 250);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, -font.width(text), -font.lineHeight+1, colour, true);
		graphics.pose().popPose();
	}

	protected void renderCount(GuiGraphics graphics, long count, int x, int y)
	{
		renderCount(graphics, count, x, y, 0xFFFFFF);
	}

	protected void renderCount(GuiGraphics graphics, long count, int x, int y, int colour)
	{
		String text = formatCount(count);
		float scale = 0.666F;
		graphics.pose().pushPose();
		graphics.pose().translate(x+16, y+16, 250);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, -font.width(text), -font.lineHeight+1, colour, true);
		graphics.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ECGui.LABEL, false);
		Component usage = usageLabel();
		graphics.drawString(font, usage, imageWidth-8-font.width(usage), inventoryLabelY, usageColour(mouseX, mouseY),
				false);
	}

	protected Component usageLabel()
	{
		TerminalStats stats = menu.getClientStats();
		return Component.translatable("gui.engineered_computation.terminal.bytes", ByteSize.format(stats.usedBytes()),
				ByteSize.format(stats.totalBytes()));
	}

	protected int usageColour(int mouseX, int mouseY)
	{
		return ECGui.LABEL;
	}

	protected static String number(long value)
	{
		return String.format(Locale.ROOT, "%,d", value);
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

	protected void pressButton(int id)
	{
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		clickSound();
	}

	protected void clickSound()
	{
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
	}

	protected void unfocusSearch()
	{
		if(search!=null)
			search.setFocused(false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(search!=null&&!search.isMouseOver(mouseX, mouseY))
			search.setFocused(false);
		if(search!=null&&search.isMouseOver(mouseX, mouseY)&&button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
		{
			search.setValue("");
			setFocused(search);
			return true;
		}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverCraftables(mouseX, mouseY))
		{
			ECClientConfig.CraftableFilter[] filters = ECClientConfig.CraftableFilter.values();
			ECClientConfig.setTerminalCraftables(filters[(ECClientConfig.terminalCraftables().ordinal()+1)%filters.length]);
			rebuildView();
			clickSound();
			return true;
		}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverRemote(mouseX, mouseY))
		{
			ECClientConfig.RemotePlacement[] placements = ECClientConfig.RemotePlacement.values();
			ECClientConfig.setTerminalRemote(placements[(ECClientConfig.terminalRemote().ordinal()+1)%placements.length]);
			rebuildView();
			clickSound();
			return true;
		}
		boolean sortBy = isOverSortBy(mouseX, mouseY);
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&(sortBy||isOverSortOrder(mouseX, mouseY)))
		{
			ECClientConfig.TerminalSort sort = ECClientConfig.terminalSort();
			boolean descending = ECClientConfig.terminalDescending();
			if(sortBy)
				sort = sort==ECClientConfig.TerminalSort.NAME? ECClientConfig.TerminalSort.COUNT
						: ECClientConfig.TerminalSort.NAME;
			else
				descending = !descending;
			ECClientConfig.setTerminalSort(sort, descending);
			rebuildView();
			clickSound();
			return true;
		}
		if(isOver(mouseX, mouseY, SCROLLBAR_X, layout.gridY(), SCROLLBAR_WIDTH, gridHeight))
		{
			draggingScrollbar = true;
			scrollToMouse(mouseY);
			return true;
		}
		if(isOverGrid(mouseX, mouseY)&&(button==GLFW.GLFW_MOUSE_BUTTON_LEFT||button==GLFW.GLFW_MOUSE_BUTTON_RIGHT))
		{
			boolean left = button==GLFW.GLFW_MOUSE_BUTTON_LEFT;
			Map.Entry<ItemKey, Long> entry = entryAt(mouseX, mouseY);
			TerminalAction action;
			if(!menu.getCarried().isEmpty())
				action = left? TerminalAction.INSERT_ALL: TerminalAction.INSERT_ONE;
			else if(entry==null||!canTake(entry))
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

	protected boolean canTake(Map.Entry<ItemKey, Long> entry)
	{
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(draggingScrollbar)
		{
			scrollToMouse(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		draggingScrollbar = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
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
