package com.muddykat.engineeredcomputation.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.program.ProgramEntry;

public final class FilterTextField
{
	public static final int HEIGHT = 13;
	private static final int MODE_WIDTH = 28;
	private static final int GAP = 3;
	private static final int SUGGESTIONS = 4;

	private final Font font;
	private final int width;
	private final BiConsumer<ProgramEditPayload.Action, String> submit;
	private final EditBox box;
	private int x;
	private int y;
	private boolean nameMode;
	private List<TagKey<Item>> suggestions = List.of();
	/** True while the box is filled in from the entry, so that isn't sent back as an edit. */
	private boolean showing;

	public FilterTextField(Font font, int x, int y, int width, BiConsumer<ProgramEditPayload.Action, String> submit)
	{
		this.font = font;
		this.width = width;
		this.submit = submit;
		box = new EditBox(font, 0, 0, width-MODE_WIDTH-GAP-4, 10, Component.translatable("gui.engineered_computation.program.tag"));
		box.setBordered(false);
		box.setMaxLength(128);
		box.setTextColor(0xFFFFFF);
		box.setResponder(text -> {
			suggest(text);
			if(!showing)
				submitText();
		});
		setPosition(x, y);
		setMode(false);
	}

	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
		box.setX(boxX()+2);
		box.setY(y+2);
	}

	private int boxX()
	{
		return x+MODE_WIDTH+GAP;
	}

	public void show(ProgramEntry entry)
	{
		if(!entry.isEmpty())
			nameMode = entry.name().isPresent();
		showing = true;
		box.setValue(entry.name().orElseGet(() -> entry.tag().map(key -> key.location().toString()).orElse("")));
		showing = false;
		setMode(nameMode);
	}

	public void clear()
	{
		showing = true;
		box.setValue("");
		showing = false;
	}

	private void submitText()
	{
		String text = box.getValue();
		if(text.isBlank())
			submit.accept(ProgramEditPayload.Action.CLEAR, "");
		else
			submit.accept(nameMode? ProgramEditPayload.Action.SET_NAME: ProgramEditPayload.Action.SET_TAG, text);
	}

	private void setMode(boolean name)
	{
		nameMode = name;
		String hint = Component.translatable(name? "gui.engineered_computation.program.name.hint"
				: "gui.engineered_computation.program.tag.hint").getString();
		if(font.width(hint)>box.getInnerWidth())
			hint = font.plainSubstrByWidth(hint, box.getInnerWidth()-font.width("…"))+"…";
		box.setHint(Component.literal(hint).withStyle(ChatFormatting.GRAY));
		suggest(box.getValue());
	}

	private void suggest(String text)
	{
		if(nameMode)
		{
			suggestions = List.of();
			return;
		}
		String query = text.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
		suggestions = query.isEmpty()? List.of(): BuiltInRegistries.ITEM.getTagNames()
				.filter(key -> key.location().toString().contains(query))
				.sorted((a, b) -> a.location().toString().compareTo(b.location().toString()))
				.limit(SUGGESTIONS).toList();
	}

	public boolean isFocused()
	{
		return box.isFocused();
	}

	public void setFocused(boolean focused)
	{
		box.setFocused(focused);
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		ECGui.button(graphics, x, y, MODE_WIDTH, HEIGHT, isOverMode(mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable(nameMode? "gui.engineered_computation.program.name"
				: "gui.engineered_computation.program.tag"), x+MODE_WIDTH/2+1, y+3, 0xFFFFFF);
		graphics.fill(boxX(), y, x+width, y+HEIGHT, 0xFFA0A0A0);
		graphics.fill(boxX()+1, y+1, x+width-1, y+HEIGHT-1, 0xFF000000);
		box.setTextColor(nameMode||tagExists(box.getValue())? 0xFFFFFF: 0xFF7060);
		box.render(graphics, mouseX, mouseY, partialTick);
		renderSuggestions(graphics, mouseX, mouseY);
	}

	private static boolean tagExists(String text)
	{
		String id = text.trim().replaceFirst("^#", "");
		return id.isEmpty()||BuiltInRegistries.ITEM.getTagNames().anyMatch(key -> key.location().toString().equals(id));
	}

	private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY)
	{
		if(!box.isFocused()||suggestions.isEmpty())
			return;
		int listY = y+HEIGHT;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.fill(boxX(), listY, x+width, listY+suggestions.size()*11+2, 0xF0101010);
		for(int i = 0; i<suggestions.size(); i++)
		{
			boolean hovered = suggestionAt(mouseX, mouseY)==i;
			graphics.drawString(font, "#"+suggestions.get(i).location(), boxX()+3, listY+2+i*11,
					hovered? 0xFFFF80: 0xC0C0C0, false);
		}
		graphics.pose().popPose();
	}

	private int suggestionAt(double mouseX, double mouseY)
	{
		if(!box.isFocused())
			return -1;
		double relX = mouseX-boxX(), relY = mouseY-y-HEIGHT-2;
		int index = (int)Math.floor(relY/11);
		return relX>=0&&relX<x+width-boxX()&&relY>=0&&index<suggestions.size()? index: -1;
	}

	private boolean isOverMode(double mouseX, double mouseY)
	{
		return mouseX>=x&&mouseX<x+MODE_WIDTH&&mouseY>=y&&mouseY<y+HEIGHT;
	}

	public boolean isOver(double mouseX, double mouseY)
	{
		return mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+HEIGHT||suggestionAt(mouseX, mouseY)>=0;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(isOverMode(mouseX, mouseY)&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			setMode(!nameMode);
			if(!box.getValue().isBlank())
				submitText();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		int suggestion = suggestionAt(mouseX, mouseY);
		if(suggestion>=0)
		{
			box.setValue(suggestions.get(suggestion).location().toString());
			box.setFocused(false);
			return true;
		}
		if(mouseX>=boxX()&&mouseX<x+width&&mouseY>=y&&mouseY<y+HEIGHT)
		{
			box.setFocused(true);
			return true;
		}
		box.setFocused(false);
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(!box.isFocused())
			return false;
		if(keyCode==GLFW.GLFW_KEY_ENTER||keyCode==GLFW.GLFW_KEY_KP_ENTER)
		{
			box.setFocused(false);
			return true;
		}
		if(keyCode==GLFW.GLFW_KEY_ESCAPE)
			return false;
		box.keyPressed(keyCode, scanCode, modifiers);
		return true;
	}

	public boolean charTyped(char codePoint, int modifiers)
	{
		return box.isFocused()&&box.charTyped(codePoint, modifiers);
	}

	public List<Component> tooltip(double mouseX, double mouseY)
	{
		if(!isOverMode(mouseX, mouseY))
			return List.of();
		String mode = nameMode? "name": "tag";
		return List.of(
				Component.translatable("gui.engineered_computation.program.mode."+mode).withStyle(ChatFormatting.YELLOW),
				Component.translatable("gui.engineered_computation.program.mode."+mode+".detail")
						.withStyle(ChatFormatting.GRAY),
				Component.translatable(nameMode? "gui.engineered_computation.program.mode.to_tag"
						: "gui.engineered_computation.program.mode.to_name").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static List<Component> entryTooltip(ProgramEntry entry)
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
		else if(entry.name().isPresent())
			lines.add(Component.translatable("tooltip.engineered_computation.filter.name", entry.name().get())
					.withStyle(ChatFormatting.GOLD));
		else
			lines.add(Component.translatable("gui.engineered_computation.program.empty").withStyle(ChatFormatting.GRAY));
		for(String key : List.of("set_item", "select", "clear"))
			lines.add(Component.translatable("gui.engineered_computation.program.help."+key)
					.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}
}
