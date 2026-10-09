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

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.item.GuideRecipes;
import com.muddykat.engineeredcomputation.item.GuideTexts;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

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
		basics();
		eraChapter();
		otherParts();
		if(era.ordinal()+1<ComputerEra.values().length)
		{
			ComputerEra next = ComputerEra.values()[era.ordinal()+1];
			chapter(GuideTexts.NEXT);
			text(GuideTexts.NEXT, Map.of(GuideTexts.NEXT_GUIDE, ECContent.guide(next).get().getDescription(),
					GuideTexts.NEXT_ERA, next.displayName()));
			GuideRecipes.nextGuide(era).ifPresent(this::recipe);
		}
		else
		{
			chapter(GuideTexts.PALM);
			text(GuideTexts.PALM);
		}
		errorCodes();

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

	private void basics()
	{
		chapter(GuideTexts.INTRO);
		text(GuideTexts.INTRO, Map.of(GuideTexts.ERA, era.displayName()));

		chapter(GuideTexts.WRENCH);
		text(GuideTexts.WRENCH);
		recipe(GuideRecipes.WRENCH);

		chapter(GuideTexts.COMPONENTS);
		text(GuideTexts.COMPONENTS);

		if(!era.onDataNetwork())
			return;

		chapter(GuideTexts.NETWORK);
		text(GuideTexts.NETWORK);
		text(GuideTexts.TERMINAL);

		chapter(GuideTexts.CABLES);
		text(GuideTexts.CABLES);
		recipe(GuideRecipes.CABLE);

		chapter(GuideTexts.BRIDGE);
		text(GuideTexts.BRIDGE);
		recipe(GuideRecipes.BRIDGE);

		chapter(GuideTexts.CELLS);
		text(GuideTexts.CELLS);
		section(GuideTexts.PLACEMENT);
		text(GuideTexts.PLACEMENT);
	}

	private void eraChapter()
	{
		chapter(GuideTexts.era(era));
		text(GuideTexts.era(era), Map.of(GuideTexts.MEMORY, era.memoryDisplayName(),
				GuideTexts.PART, ECContent.component(era).get().getDescription()));
		recipe(GuideRecipes.component(era));
		recipe(GuideRecipes.casing(era));

		for(EraDrive drive : era.drives())
		{
			section(ECMultiblocks.ERA_DRIVES.get(drive).partBlock().getName());
			String slots = drive.era().onDataNetwork()? "structure.slots": "structure.decks";
			pages.add(new StructurePage(drive, font.split(Component.translatable(KEY+slots, drive.slots())
					.append("\n").append(Component.translatable(KEY+"structure.hint")), GuidePage.WIDTH)));
			if(!drive.geometry().terminalZones().isEmpty())
				text(GuideTexts.BUILTIN_TERMINAL);
			if(!drive.geometry().storageZones().isEmpty())
				text(GuideTexts.STORAGE_BAY);
		}

		if(!GuideRecipes.tubes(era).isEmpty())
		{
			section(GuideTexts.TUBES);
			text(GuideTexts.TUBES);
			GuideRecipes.tubes(era).forEach(this::recipe);
		}
		GuideRecipes.catalogueCard(era).ifPresent(card -> {
			section(GuideTexts.CATALOGUE_CARDS);
			text(GuideTexts.CATALOGUE_CARDS);
			recipe(card);
		});

		if(era.hasTerminal())
		{
			section(GuideTexts.eraTerminal(era));
			text(GuideTexts.eraTerminal(era));
		}
		GuideRecipes.terminal(era).ifPresent(this::recipe);
		if(!GuideRecipes.terminalDisks(era).isEmpty())
		{
			text(GuideTexts.TERMINAL_DISKS);
			GuideRecipes.terminalDisks(era).forEach(this::recipe);
		}

		if(!GuideRecipes.adapters(era).isEmpty())
		{
			section(GuideTexts.ADAPTERS);
			text(GuideTexts.ADAPTERS);
			GuideRecipes.adapters(era).forEach(this::recipe);
		}
		GuideRecipes.programFloppy(era).ifPresent(floppy -> {
			section(GuideTexts.PROGRAM_FLOPPY);
			text(GuideTexts.PROGRAM_FLOPPY);
			recipe(floppy);
		});
		GuideRecipes.patternFloppy(era).ifPresent(floppy -> {
			section(GuideTexts.CRAFTING_COMPUTER);
			text(GuideTexts.CRAFTING_COMPUTER);
			EraMachine computer = EraMachine.CRAFTING_COMPUTER;
			pages.add(new StructurePage(ECMultiblocks.CRAFTING_COMPUTER.partBlock().getName(),
					ECContent.casing(computer.era()).get(), computer.geometry(),
					font.split(Component.translatable(KEY+"structure.hint"), GuidePage.WIDTH)));
			section(GuideTexts.DATA_PROCESSOR);
			text(GuideTexts.DATA_PROCESSOR);
			pages.add(new StructurePage(ECMultiblocks.DATA_PROCESSOR.partBlock().getName(),
					ECContent.PROCESSOR_CASING.get(), EraMachine.DATA_PROCESSOR.geometry(),
					font.split(Component.translatable(KEY+"structure.hint"), GuidePage.WIDTH)));
			recipe(GuideRecipes.PROCESSOR_CASING);
			section(GuideTexts.PATTERN_FLOPPY);
			text(GuideTexts.PATTERN_FLOPPY);
			recipe(floppy);
			section(GuideTexts.ORDERING);
			text(GuideTexts.ORDERING);
		});

		section(era.onDataNetwork()? Component.translatable(KEY+"section.memory", era.memoryDisplayName())
				: Component.translatable(KEY+"section.decks"));
		GuideRecipes.cells(era).forEach(this::recipe);
	}

	private void otherParts()
	{
		Level level = Minecraft.getInstance().level;
		if(level==null)
			return;
		List<ResourceLocation> others = GuideRecipes.prerequisites(GuideRecipes.shown(era), level.getRecipeManager(),
				level.registryAccess());
		if(others.isEmpty())
			return;
		chapter(GuideTexts.OTHER_PARTS);
		text(GuideTexts.OTHER_PARTS);
		others.forEach(this::recipe);
	}

	private void errorCodes()
	{
		chapter(GuideTexts.CODES);
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

	private void chapter(String page)
	{
		chapters.add(new ContentsPage.Entry(GuideText.contents(page), pages.size(), false));
	}

	private void section(String page)
	{
		chapters.add(new ContentsPage.Entry(GuideText.contents(page), pages.size(), true));
	}

	private void text(String page)
	{
		text(page, Map.of());
	}

	private void text(String page, Map<String, Component> values)
	{
		text(GuideText.title(page), GuideText.body(page, values));
	}

	private void section(Component label)
	{
		chapters.add(new ContentsPage.Entry(label, pages.size(), true));
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
