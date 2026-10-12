package com.muddykat.engineeredcomputation.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.ECPayloads;

public class ProgramFloppyScreen extends AbstractContainerScreen<ProgramFloppyMenu>
{
	private static final int TAG_Y = 44;
	private static final int TAG_X = 32;
	private static final int SUGGESTIONS = 4;

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
				new TranslatableComponent("gui.engineered_computation.program.tag"));
		tag.setBordered(false);
		tag.setMaxLength(128);
		tag.setTextColor(0xFFFFFF);
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
		suggestions = query.isEmpty()? List.of(): Registry.ITEM.getTagNames()
				.filter(key -> key.location().toString().contains(query))
				.sorted((a, b) -> a.location().toString().compareTo(b.location().toString()))
				.limit(SUGGESTIONS).toList();
	}

	private void send(ProgramEditPayload.Action action, String text)
	{
		ECPayloads.sendToServer(new ProgramEditPayload(menu.containerId, selected, action, text));
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		ECGui.hint(pose, font, tag, new TranslatableComponent("gui.engineered_computation.program.tag.hint")
				.withStyle(ChatFormatting.GRAY), false);
		renderSuggestions(pose, mouseX, mouseY);
		int entry = entryAt(mouseX, mouseY);
		if(entry>=0&&menu.getCarried().isEmpty())
			renderComponentTooltip(pose, entryTooltip(menu.program().get(entry)), mouseX, mouseY);
		else
			renderTooltip(pose, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(pose, x, y, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			if(slot.isActive())
				ECGui.slot(pose, x+slot.x-1, y+slot.y-1);
		Program program = menu.program();
		for(int i = 0; i<menu.size(); i++)
		{
			int entryX = x+ProgramFloppyMenu.ENTRIES_X+i*18, entryY = y+ProgramFloppyMenu.ENTRIES_Y;
			ECGui.slot(pose, entryX, entryY);
			ItemStack shown = ECGui.entryIcon(program.get(i));
			ECGui.renderItem(pose, shown, entryX+1, entryY+1);
			if(program.get(i).tag().isPresent())
				font.drawShadow(pose, "#", entryX+12, entryY+10, 0x55FFFF);
			if(i==selected)
				ECGui.outline(pose, entryX, entryY, 18, 18, 0xFFFFFFFF);
			if(i==entryAt(mouseX, mouseY))
				GuiComponent.fill(pose, entryX+1, entryY+1, entryX+17, entryY+17, 0x80FFFFFF);
		}
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.program.tag"), x+8, y+TAG_Y+2,
				ECGui.LABEL);
		GuiComponent.fill(pose, x+TAG_X, y+TAG_Y, x+imageWidth-8, y+TAG_Y+13, 0xFFA0A0A0);
		GuiComponent.fill(pose, x+TAG_X+1, y+TAG_Y+1, x+imageWidth-9, y+TAG_Y+12, 0xFF000000);
		tag.setTextColor(tagExists(tag.getValue())? 0xFFFFFF: 0xFF7060);
	}

	private static boolean tagExists(String text)
	{
		String id = text.trim().replaceFirst("^#", "");
		return id.isEmpty()||Registry.ITEM.getTagNames().anyMatch(key -> key.location().toString().equals(id));
	}

	private List<Component> entryTooltip(ProgramEntry entry)
	{
		List<Component> lines = new ArrayList<>();
		if(entry.item().isPresent())
			lines.add(entry.item().get().value().getDescription());
		else if(entry.tag().isPresent())
		{
			TagKey<Item> key = entry.tag().get();
			lines.add(new TextComponent("#"+key.location()).withStyle(ChatFormatting.AQUA));
			int count = Registry.ITEM.getTag(key).map(HolderSet::size).orElse(0);
			lines.add(new TranslatableComponent("gui.engineered_computation.program.matches", count)
					.withStyle(ChatFormatting.GRAY));
		}
		else
			lines.add(new TranslatableComponent("gui.engineered_computation.program.empty")
					.withStyle(ChatFormatting.GRAY));
		for(String key : List.of("set_item", "select", "clear"))
			lines.add(new TranslatableComponent("gui.engineered_computation.program.help."+key)
					.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private void renderSuggestions(PoseStack pose, int mouseX, int mouseY)
	{
		if(!tag.isFocused()||suggestions.isEmpty())
			return;
		int x = leftPos+TAG_X, y = topPos+TAG_Y+13;
		int width = imageWidth-TAG_X-8;
		pose.pushPose();
		pose.translate(0, 0, 400);
		GuiComponent.fill(pose, x, y, x+width, y+suggestions.size()*11+2, 0xF0101010);
		for(int i = 0; i<suggestions.size(); i++)
		{
			boolean hovered = suggestionAt(mouseX, mouseY)==i;
			font.draw(pose, "#"+suggestions.get(i).location(), x+3, y+2+i*11, hovered? 0xFFFF80: 0xC0C0C0);
		}
		pose.popPose();
	}

	private int suggestionAt(double mouseX, double mouseY)
	{
		if(!tag.isFocused())
			return -1;
		double relX = mouseX-leftPos-TAG_X, relY = mouseY-topPos-TAG_Y-13-2;
		int index = (int)Math.floor(relY/11);
		return relX>=0&&relX<imageWidth-TAG_X-8&&relY>=0&&index<suggestions.size()? index: -1;
	}

	/** Where program entry {@code index} is on screen. */
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
		int suggestion = suggestionAt(mouseX, mouseY);
		if(suggestion>=0)
		{
			tag.setValue(suggestions.get(suggestion).location().toString());
			send(ProgramEditPayload.Action.SET_TAG, tag.getValue());
			tag.setFocus(false);
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
				tag.setFocus(true);
			}
			return true;
		}
		if(!tag.isMouseOver(mouseX, mouseY))
			tag.setFocus(false);
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
				tag.setFocus(false);
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
