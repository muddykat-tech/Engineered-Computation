package com.muddykat.engineeredcomputation.client.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.client.guide.page.ContentsPage;
import com.muddykat.engineeredcomputation.client.guide.page.GuidePage;
import com.muddykat.engineeredcomputation.client.guide.page.RecipePage;
import com.muddykat.engineeredcomputation.client.guide.page.StructurePage;
import com.muddykat.engineeredcomputation.client.guide.page.TextPage;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.guide.GuideOutline;
import com.muddykat.engineeredcomputation.guide.GuideTexts;
import com.muddykat.engineeredcomputation.network.StatusCode;

final class GuideBook
{
	private static final String KEY = "guide.engineered_computation.";

	private final Font font;
	private final ComputerEra era;
	private final List<GuidePage> pages = new ArrayList<>();
	private final List<ContentsPage.Entry> chapters = new ArrayList<>();
	private final Map<Item, Integer> recipePages = new HashMap<>();

	private GuideBook(Font font, ComputerEra era)
	{
		this.font = font;
		this.era = era;
	}

	static Guide build(Font font, ComputerEra era)
	{
		return new GuideBook(font, era).buildGuide();
	}

	private Guide buildGuide()
	{
		Level level = Minecraft.getInstance().level;
		for(GuideOutline.Step step : GuideOutline.of(era, level==null? null: level.getRecipeManager(),
				level==null? null: level.registryAccess()))
			switch(step)
			{
				case GuideOutline.Heading heading -> chapters.add(new ContentsPage.Entry(heading.page()!=null
						? GuideText.contents(heading.page()): heading.label(), pages.size(), heading.section()));
				case GuideOutline.Text text -> {
					Map<String, Component> values = new HashMap<>();
					text.values().forEach((name, value) -> values.put(name, value.now().get()));
					text(GuideText.title(text.page()), GuideText.body(text.page(), values));
				}
				case GuideOutline.Recipe recipe -> recipe(recipe.id());
				case GuideOutline.Structure structure -> {
					Component caption = structure.slots()==null? structure.hint()
							: structure.slots().copy().append("\n").append(structure.hint());
					pages.add(new StructurePage(structure.name(), structure.casing(), structure.geometry(),
							font.split(caption, GuidePage.WIDTH)));
				}
				case GuideOutline.Codes codes -> errorCodes();
			}

		int contentsPages = Math.max(1, (chapters.size()+ContentsPage.ENTRIES-1)/ContentsPage.ENTRIES);
		List<GuidePage> all = new ArrayList<>();
		for(int start = 0; start<chapters.size(); start += ContentsPage.ENTRIES)
		{
			List<ContentsPage.Entry> entries = new ArrayList<>();
			for(ContentsPage.Entry chapter : chapters.subList(start, Math.min(chapters.size(), start+ContentsPage.ENTRIES)))
				entries.add(new ContentsPage.Entry(chapter.label(), chapter.page()+contentsPages, chapter.section()));
			all.add(new ContentsPage(Component.translatable(KEY+"contents"), entries));
		}
		all.addAll(pages);
		Map<Item, Integer> recipes = new HashMap<>();
		recipePages.forEach((item, page) -> recipes.put(item, page+contentsPages));
		return new Guide(era, List.copyOf(all), Map.copyOf(recipes));
	}

	private void errorCodes()
	{
		Map<Integer, String> meanings = GuideTexts.codes(GuideText.page(GuideTexts.CODES).body());
		List<Component> entries = new ArrayList<>();
		for(StatusCode code : StatusCode.values())
			entries.add(Component.empty().append(code.title().copy().withStyle(ChatFormatting.BOLD)).append("\n")
					.append(meanings.getOrDefault(code.code(), "")));
		entries(GuideText.title(GuideTexts.CODES), entries);
	}

	private void entries(Component title, List<Component> entries)
	{
		List<FormattedCharSequence> page = new ArrayList<>();
		int capacity = TextPage.LINES-TextPage.TITLE_LINES;
		boolean first = true;
		for(Component entry : entries)
		{
			List<FormattedCharSequence> lines = font.split(entry, GuidePage.WIDTH);
			int needed = lines.size()+(page.isEmpty()? 0: 1);
			if(!page.isEmpty()&&page.size()+needed>capacity)
			{
				pages.add(new TextPage(first? title: null, List.copyOf(page)));
				page.clear();
				first = false;
				capacity = TextPage.LINES;
			}
			if(!page.isEmpty())
				page.add(FormattedCharSequence.EMPTY);
			page.addAll(lines);
		}
		pages.add(new TextPage(first? title: null, List.copyOf(page)));
	}

	private void text(Component title, Component body)
	{
		List<FormattedCharSequence> lines = font.split(body, GuidePage.WIDTH);
		int capacity = TextPage.LINES-TextPage.TITLE_LINES;
		int start = 0;
		boolean first = true;
		do
		{
			int end = Math.min(lines.size(), start+capacity);
			pages.add(new TextPage(first? title: null, lines.subList(start, end)));
			start = end;
			first = false;
			capacity = TextPage.LINES;
		}
		while(start<lines.size());
	}

	private void recipe(ResourceLocation id)
	{
		ItemStack result = RecipePage.result(id);
		if(!result.isEmpty())
			recipePages.putIfAbsent(result.getItem(), pages.size());
		pages.add(new RecipePage(id));
	}
}
