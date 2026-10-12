package com.muddykat.engineeredcomputation.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.ECPayloads;

public class PatternFloppyScreen extends AbstractContainerScreen<PatternFloppyMenu>
{
	private static final int MODE_X = 120;
	private static final int MODE_Y = 4;
	private static final int MODE_WIDTH = 48;
	private static final int BUTTON_HEIGHT = 11;
	private static final int CLEAR_X = 14;
	private static final int CLEAR_SIZE = 11;
	private static final String[] CLEAR_ICON = {
			"#...#",
			".#.#.",
			"..#..",
			".#.#.",
			"#...#"};

	private static final int TAG_X = 32;
	private static final int SUGGESTIONS = 4;

	private EditBox tag;
	private int selected = -1;
	private List<TagKey<Item>> suggestions = List.of();
	private final Set<Integer> dragged = new HashSet<>();
	private boolean dragging;
	private int dragAmount;

	public PatternFloppyScreen(PatternFloppyMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = PatternFloppyMenu.INVENTORY_Y+58+18+6;
		this.inventoryLabelY = PatternFloppyMenu.INVENTORY_Y-11;
	}

	@Override
	protected void init()
	{
		super.init();
		tag = new EditBox(font, leftPos+TAG_X+2, topPos+PatternFloppyMenu.TAG_Y+2, imageWidth-TAG_X-12, 10,
				Component.translatable("gui.engineered_computation.program.tag"));
		tag.setBordered(false);
		tag.setMaxLength(128);
		tag.setTextColor(0xFFFFFF);
		tag.setHint(Component.translatable("gui.engineered_computation.pattern.tag.hint").withStyle(ChatFormatting.GRAY));
		tag.setResponder(this::suggest);
		addRenderableWidget(tag);
	}

	private boolean isDirect()
	{
		return menu.pattern().mode()==Pattern.Mode.DIRECT;
	}

	private void send(PatternEditPayload.Action action, int slot, int amount)
	{
		ECPayloads.sendToServer(new PatternEditPayload(menu.containerId, action, slot, amount));
	}

	private void select(int slot)
	{
		selected = slot;
		tag.setValue(tagOf(menu.pattern(), slot).map(key -> key.location().toString()).orElse(""));
		setFocused(tag);
		tag.setFocused(true);
	}

	private void suggest(String text)
	{
		String query = text.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
		suggestions = query.isEmpty()? List.of(): BuiltInRegistries.ITEM.getTagNames()
				.filter(key -> key.location().toString().contains(query))
				.sorted(Comparator.comparing(key -> key.location().toString()))
				.limit(SUGGESTIONS).toList();
	}

	private void applyTag()
	{
		if(selected>=0&&!tag.getValue().isBlank())
			ECPayloads.sendToServer(new PatternEditPayload(menu.containerId, PatternEditPayload.Action.SET_TAG,
					selected, 0, tag.getValue()));
		tag.setFocused(false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		tag.visible = !isDirect();
		if(isDirect())
			selected = -1;
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderSuggestions(graphics, mouseX, mouseY);
		int slot = slotAt(mouseX, mouseY);
		if(slot>=0&&menu.getCarried().isEmpty())
			graphics.renderComponentTooltip(font, slotTooltip(slot), mouseX, mouseY);
		else if(isOver(mouseX, mouseY, MODE_X, MODE_Y, MODE_WIDTH, BUTTON_HEIGHT))
			graphics.renderComponentTooltip(font, List.of(
					Component.translatable("gui.engineered_computation.pattern."+mode()).withStyle(ChatFormatting.YELLOW),
					Component.translatable("gui.engineered_computation.pattern."+mode()+".detail")
							.withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.engineered_computation.pattern.switch").withStyle(ChatFormatting.DARK_GRAY)),
					mouseX, mouseY);
		else if(isOver(mouseX, mouseY, CLEAR_X, clearY(), CLEAR_SIZE, CLEAR_SIZE))
			graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.engineered_computation.pattern.clear")),
					mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	private String mode()
	{
		return menu.pattern().mode().getSerializedName();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(graphics, x, y, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			if(slot.isActive())
				ECGui.slot(graphics, x+slot.x-1, y+slot.y-1);
		Pattern pattern = menu.pattern();
		int hovered = slotAt(mouseX, mouseY);
		for(int i = 0; i<Pattern.INPUTS+outputsShown(); i++)
		{
			int slotX = x+slotX(i), slotY = y+slotY(i);
			ECGui.slot(graphics, slotX, slotY);
			ItemStack stack = shown(pattern, i);
			graphics.renderItem(stack, slotX+1, slotY+1);
			graphics.renderItemDecorations(font, stack, slotX+1, slotY+1);
			if(tagOf(pattern, i).isPresent())
			{
				ECGui.tagBadge(graphics, font, slotX+1, slotY+1);
				if(i<Pattern.INPUTS&&pattern.warnsAt(i))
				{
					graphics.pose().pushPose();
					graphics.pose().translate(0, 0, 200);
					graphics.drawString(font, "!", slotX+13, slotY+1, 0xFF5A4A, true);
					graphics.pose().popPose();
				}
			}
			if(i==selected)
				graphics.renderOutline(slotX, slotY, 18, 18, 0xFFFFD24A);
			if(i==hovered)
				graphics.fill(slotX+1, slotY+1, slotX+17, slotY+17, 0x80FFFFFF);
		}
		if(!isDirect())
		{
			graphics.drawString(font, Component.translatable("gui.engineered_computation.program.tag"), x+8,
					y+PatternFloppyMenu.TAG_Y+2, ECGui.LABEL, false);
			graphics.fill(x+TAG_X, y+PatternFloppyMenu.TAG_Y, x+imageWidth-8, y+PatternFloppyMenu.TAG_Y+13, 0xFFA0A0A0);
			graphics.fill(x+TAG_X+1, y+PatternFloppyMenu.TAG_Y+1, x+imageWidth-9, y+PatternFloppyMenu.TAG_Y+12,
					0xFF000000);
			tag.setTextColor(tagExists(tag.getValue())? 0xFFFFFF: 0xFF7060);
		}
		int arrowX = x+PatternFloppyMenu.INPUTS_X+3*18+8, arrowY = y+PatternFloppyMenu.INPUTS_Y+27;
		graphics.fill(arrowX, arrowY-2, arrowX+12, arrowY+2, 0xFF8B8B8B);
		for(int step = 0; step<6; step++)
			graphics.fill(arrowX+12+step, arrowY-6+step, arrowX+13+step, arrowY+6-step, 0xFF8B8B8B);
		if(isDirect()&&pattern.output(0).isEmpty()&&!pattern.isEmpty())
			graphics.drawString(font, Component.translatable("gui.engineered_computation.pattern.no_recipe"),
					x+slotX(Pattern.INPUTS)-8, y+PatternFloppyMenu.INPUTS_Y+3*18+3, 0xFF7060, false);
		ECGui.button(graphics, x+MODE_X, y+MODE_Y, MODE_WIDTH, BUTTON_HEIGHT,
				isOver(mouseX, mouseY, MODE_X, MODE_Y, MODE_WIDTH, BUTTON_HEIGHT));
		graphics.drawCenteredString(font, Component.translatable("gui.engineered_computation.pattern."+mode()+".short"),
				x+MODE_X+MODE_WIDTH/2+1, y+MODE_Y+2, isDirect()? 0x8CC8FF: 0xFFB050);
		ECGui.button(graphics, x+CLEAR_X, y+clearY(), CLEAR_SIZE, CLEAR_SIZE,
				isOver(mouseX, mouseY, CLEAR_X, clearY(), CLEAR_SIZE, CLEAR_SIZE));
		ECGui.icon(graphics, x+CLEAR_X+3, y+clearY()+3, CLEAR_ICON, 0xFFFFFFFF);
	}

	private static int clearY()
	{
		return PatternFloppyMenu.INPUTS_Y;
	}

	private static Optional<TagKey<Item>> tagOf(Pattern pattern, int slot)
	{
		return slot<Pattern.INPUTS? pattern.inputTag(slot): pattern.outputTag(slot-Pattern.INPUTS);
	}

	private static ItemStack shown(Pattern pattern, int slot)
	{
		ItemStack stack = slot<Pattern.INPUTS? pattern.input(slot): pattern.output(slot-Pattern.INPUTS);
		Optional<TagKey<Item>> tag = tagOf(pattern, slot);
		if(tag.isEmpty())
			return stack;
		List<Item> items = Pattern.itemsOf(tag.get());
		return items.isEmpty()? stack: new ItemStack(items.get((int)(Util.getMillis()/1000%items.size())),
				stack.getCount());
	}

	private static boolean tagExists(String text)
	{
		String id = text.trim().replaceFirst("^#", "");
		return id.isEmpty()||BuiltInRegistries.ITEM.getTagNames().anyMatch(key -> key.location().toString().equals(id));
	}

	private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(!tag.isFocused()||suggestions.isEmpty())
			return;
		int x = leftPos+TAG_X, y = topPos+PatternFloppyMenu.TAG_Y+13;
		int width = imageWidth-TAG_X-8;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.fill(x, y, x+width, y+suggestions.size()*11+2, 0xF0101010);
		for(int i = 0; i<suggestions.size(); i++)
			graphics.drawString(font, "#"+suggestions.get(i).location(), x+3, y+2+i*11,
					suggestionAt(mouseX, mouseY)==i? 0xFFFF80: 0xC0C0C0, false);
		graphics.pose().popPose();
	}

	private int suggestionAt(double mouseX, double mouseY)
	{
		if(!tag.isFocused())
			return -1;
		double relX = mouseX-leftPos-TAG_X, relY = mouseY-topPos-PatternFloppyMenu.TAG_Y-13-2;
		int index = (int)Math.floor(relY/11);
		return relX>=0&&relX<imageWidth-TAG_X-8&&relY>=0&&index<suggestions.size()? index: -1;
	}

	private int outputsShown()
	{
		return isDirect()? 1: Pattern.OUTPUTS;
	}

	/**
	 * Where pattern slot {@code slot} (the inputs, then the outputs) is on screen, if it can be set by hand - a
	 * crafting pattern works out its own output.
	 */
	public Optional<Rect2i> settableSlotArea(int slot)
	{
		if(slot<0||slot>=Pattern.INPUTS+outputsShown()||slot>=Pattern.INPUTS&&isDirect())
			return Optional.empty();
		return Optional.of(new Rect2i(leftPos+slotX(slot), topPos+slotY(slot), 18, 18));
	}

	private int slotX(int slot)
	{
		return slot<Pattern.INPUTS? PatternFloppyMenu.INPUTS_X+slot%3*18: PatternFloppyMenu.OUTPUTS_X;
	}

	private int slotY(int slot)
	{
		if(slot<Pattern.INPUTS)
			return PatternFloppyMenu.INPUTS_Y+slot/3*18;
		return isDirect()? PatternFloppyMenu.OUTPUTS_Y+18: PatternFloppyMenu.OUTPUTS_Y+(slot-Pattern.INPUTS)*18;
	}

	private int slotAt(double mouseX, double mouseY)
	{
		for(int i = 0; i<Pattern.INPUTS+outputsShown(); i++)
			if(isOver(mouseX, mouseY, slotX(i), slotY(i), 18, 18))
				return i;
		return -1;
	}

	private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height)
	{
		double relX = mouseX-leftPos, relY = mouseY-topPos;
		return relX>=x&&relX<x+width&&relY>=y&&relY<y+height;
	}

	private List<Component> slotTooltip(int slot)
	{
		Pattern pattern = menu.pattern();
		boolean output = slot>=Pattern.INPUTS;
		ItemStack stack = output? pattern.output(slot-Pattern.INPUTS): pattern.input(slot);
		List<Component> lines = new ArrayList<>();
		Optional<TagKey<Item>> tag = tagOf(pattern, slot);
		if(tag.isPresent())
		{
			lines.add(Component.literal(stack.getCount()+" x #"+tag.get().location()).withStyle(ChatFormatting.AQUA));
			lines.add(Component.translatable("gui.engineered_computation.program.matches",
					Pattern.itemsOf(tag.get()).size()).withStyle(ChatFormatting.GRAY));
			if(!output&&pattern.warnsAt(slot))
				lines.add(Component.translatable("gui.engineered_computation.pattern.tag.warning",
						Pattern.TAG_WARNING_SIZE).withStyle(ChatFormatting.RED));
		}
		else if(!stack.isEmpty())
			lines.addAll(getTooltipFromContainerItem(stack));
		else
			lines.add(Component.translatable("gui.engineered_computation.program.empty").withStyle(ChatFormatting.GRAY));
		List<String> help = isDirect()? output? List.of("crafted"): List.of("set", "clear")
				: List.of("set", "more", "less", "tag");
		for(String key : help)
			lines.add(Component.translatable("gui.engineered_computation.pattern.help."+key)
					.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		int suggestion = suggestionAt(mouseX, mouseY);
		if(suggestion>=0)
		{
			tag.setValue(suggestions.get(suggestion).location().toString());
			applyTag();
			return true;
		}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOver(mouseX, mouseY, MODE_X, MODE_Y, MODE_WIDTH, BUTTON_HEIGHT))
			return click(PatternEditPayload.Action.TOGGLE_MODE, 0, 0);
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOver(mouseX, mouseY, CLEAR_X, clearY(), CLEAR_SIZE, CLEAR_SIZE))
			return click(PatternEditPayload.Action.CLEAR_ALL, 0, 0);
		int slot = slotAt(mouseX, mouseY);
		if(slot>=0)
		{
			int step = hasShiftDown()? 10: 1;
			boolean left = button==GLFW.GLFW_MOUSE_BUTTON_LEFT;
			if(!menu.getCarried().isEmpty())
			{
				dragging = true;
				dragged.clear();
				dragAmount = left? 0: 1;
				setAt(slot);
			}
			else if(isDirect())
			{
				if(!left)
					send(PatternEditPayload.Action.CLEAR, slot, 0);
			}
			else if(left&&(hasControlDown()||shown(menu.pattern(), slot).isEmpty()))
				select(slot);
			else
				send(PatternEditPayload.Action.ADJUST, slot, left? step: -step);
			return true;
		}
		if(!tag.isMouseOver(mouseX, mouseY))
			tag.setFocused(false);
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(tag.isFocused())
		{
			if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER)
			{
				applyTag();
				return true;
			}
			if(keyCode!=GLFW.GLFW_KEY_ESCAPE)
			{
				tag.keyPressed(keyCode, scanCode, modifiers);
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(dragging)
		{
			int slot = slotAt(mouseX, mouseY);
			if(slot>=0&&!menu.getCarried().isEmpty())
				setAt(slot);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(dragging)
		{
			dragging = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void setAt(int slot)
	{
		if(dragged.add(slot))
			send(PatternEditPayload.Action.SET, slot, dragAmount);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY)
	{
		int slot = slotAt(mouseX, mouseY);
		if(slot>=0&&!isDirect()&&scrollY!=0)
		{
			send(PatternEditPayload.Action.ADJUST, slot, (int)Math.signum(scrollY)*(hasShiftDown()? 10: 1));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollY);
	}

	private boolean click(PatternEditPayload.Action action, int slot, int amount)
	{
		send(action, slot, amount);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
		return true;
	}
}
