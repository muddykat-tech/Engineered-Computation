package com.muddykat.engineeredcomputation.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.ECPayloads;

public class ProgramFloppyScreen extends AbstractContainerScreen<ProgramFloppyMenu>
{
	private static final int TAG_Y = 44;
	private static final int TAG_X = 32;
	private static final int SUGGESTIONS = 4;
	private static final int CYCLE_MILLIS = 1000;

	private EditBox tag;
	private int selected = 0;
	private List<TagKey<Item>> suggestions = List.of();

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
		tag = new EditBox(font, leftPos+TAG_X+2, topPos+TAG_Y+2, imageWidth-TAG_X-12, 10,
				Component.translatable("gui.engineered_computation.program.tag"));
		tag.setBordered(false);
		tag.setMaxLength(128);
		tag.setTextColor(0xFFFFFF);
		tag.setHint(Component.translatable("gui.engineered_computation.program.tag.hint").withStyle(ChatFormatting.GRAY));
		tag.setResponder(this::suggest);
		addRenderableWidget(tag);
		select(selected);
	}

	private void select(int index)
	{
		selected = index;
		tag.setValue(menu.program().get(index).tag().map(key -> key.location().toString()).orElse(""));
	}

	private void suggest(String text)
	{
		String query = text.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
		suggestions = query.isEmpty()? List.of(): BuiltInRegistries.ITEM.getTagNames()
				.filter(key -> key.location().toString().contains(query))
				.sorted((a, b) -> a.location().toString().compareTo(b.location().toString()))
				.limit(SUGGESTIONS).toList();
	}

	private void send(ProgramEditPayload.Action action, String text)
	{
		ECPayloads.sendToServer(new ProgramEditPayload(menu.containerId, selected, action, text));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderSuggestions(graphics, mouseX, mouseY);
		int entry = entryAt(mouseX, mouseY);
		if(entry>=0&&menu.getCarried().isEmpty())
			graphics.renderComponentTooltip(font, entryTooltip(menu.program().get(entry)), mouseX, mouseY);
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
		for(int i = 0; i<Program.SIZE; i++)
		{
			int entryX = x+ProgramFloppyMenu.ENTRIES_X+i*18, entryY = y+ProgramFloppyMenu.ENTRIES_Y;
			ECGui.slot(graphics, entryX, entryY);
			ItemStack shown = shown(program.get(i));
			graphics.renderItem(shown, entryX+1, entryY+1);
			if(program.get(i).tag().isPresent())
				graphics.drawString(font, "#", entryX+12, entryY+10, 0x55FFFF, true);
			if(i==selected)
				graphics.renderOutline(entryX, entryY, 18, 18, 0xFFFFFFFF);
			if(i==entryAt(mouseX, mouseY))
				graphics.fill(entryX+1, entryY+1, entryX+17, entryY+17, 0x80FFFFFF);
		}
		graphics.drawString(font, Component.translatable("gui.engineered_computation.program.tag"), x+8, y+TAG_Y+2,
				ECGui.LABEL, false);
		graphics.fill(x+TAG_X, y+TAG_Y, x+imageWidth-8, y+TAG_Y+13, 0xFFA0A0A0);
		graphics.fill(x+TAG_X+1, y+TAG_Y+1, x+imageWidth-9, y+TAG_Y+12, 0xFF000000);
		tag.setTextColor(tagExists(tag.getValue())? 0xFFFFFF: 0xFF7060);
	}

	private static ItemStack shown(ProgramEntry entry)
	{
		if(entry.item().isPresent())
			return new ItemStack(entry.item().get());
		Optional<HolderSet.Named<Item>> items = entry.tag().flatMap(BuiltInRegistries.ITEM::getTag);
		if(items.isEmpty()||items.get().size()==0)
			return entry.tag().isPresent()? new ItemStack(Items.BARRIER): ItemStack.EMPTY;
		long cycle = Util.getMillis()/CYCLE_MILLIS;
		return new ItemStack(items.get().get((int)(cycle%items.get().size())));
	}

	private static boolean tagExists(String text)
	{
		String id = text.trim().replaceFirst("^#", "");
		return id.isEmpty()||BuiltInRegistries.ITEM.getTagNames().anyMatch(key -> key.location().toString().equals(id));
	}

	private List<Component> entryTooltip(ProgramEntry entry)
	{
		List<Component> lines = new ArrayList<>();
		if(entry.item().isPresent())
			lines.add(entry.item().get().value().getDescription());
		else if(entry.tag().isPresent())
		{
			TagKey<Item> key = entry.tag().get();
			lines.add(Component.literal("#"+key.location()).withStyle(ChatFormatting.AQUA));
			int count = BuiltInRegistries.ITEM.getTag(key).map(HolderSet::size).orElse(0);
			lines.add(Component.translatable("gui.engineered_computation.program.matches", count)
					.withStyle(ChatFormatting.GRAY));
		}
		else
			lines.add(Component.translatable("gui.engineered_computation.program.empty").withStyle(ChatFormatting.GRAY));
		for(String key : List.of("set_item", "select", "clear"))
			lines.add(Component.translatable("gui.engineered_computation.program.help."+key)
					.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(!tag.isFocused()||suggestions.isEmpty())
			return;
		int x = leftPos+TAG_X, y = topPos+TAG_Y+13;
		int width = imageWidth-TAG_X-8;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.fill(x, y, x+width, y+suggestions.size()*11+2, 0xF0101010);
		for(int i = 0; i<suggestions.size(); i++)
		{
			boolean hovered = suggestionAt(mouseX, mouseY)==i;
			graphics.drawString(font, "#"+suggestions.get(i).location(), x+3, y+2+i*11, hovered? 0xFFFF80: 0xC0C0C0,
					false);
		}
		graphics.pose().popPose();
	}

	private int suggestionAt(double mouseX, double mouseY)
	{
		if(!tag.isFocused())
			return -1;
		double relX = mouseX-leftPos-TAG_X, relY = mouseY-topPos-TAG_Y-13-2;
		int index = (int)Math.floor(relY/11);
		return relX>=0&&relX<imageWidth-TAG_X-8&&relY>=0&&index<suggestions.size()? index: -1;
	}

	private int entryAt(double mouseX, double mouseY)
	{
		double relX = mouseX-leftPos-ProgramFloppyMenu.ENTRIES_X, relY = mouseY-topPos-ProgramFloppyMenu.ENTRIES_Y;
		if(relX<0||relY<0||relY>=18||relX>=Program.SIZE*18)
			return -1;
		return (int)(relX/18);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		int suggestion = suggestionAt(mouseX, mouseY);
		if(suggestion>=0)
		{
			tag.setValue(suggestions.get(suggestion).location().toString());
			send(ProgramEditPayload.Action.SET_TAG, tag.getValue());
			tag.setFocused(false);
			return true;
		}
		int entry = entryAt(mouseX, mouseY);
		if(entry>=0)
		{
			selected = entry;
			if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT)
			{
				send(ProgramEditPayload.Action.CLEAR, "");
				tag.setValue("");
			}
			else if(!menu.getCarried().isEmpty())
			{
				send(ProgramEditPayload.Action.SET_ITEM, "");
				tag.setValue("");
			}
			else
			{
				select(entry);
				setFocused(tag);
				tag.setFocused(true);
			}
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
				if(!tag.getValue().isBlank())
					send(ProgramEditPayload.Action.SET_TAG, tag.getValue());
				tag.setFocused(false);
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
}
