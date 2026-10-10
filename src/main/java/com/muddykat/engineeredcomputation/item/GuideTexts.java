package com.muddykat.engineeredcomputation.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class GuideTexts
{
	public static final String FOLDER = "guide";
	public static final String FALLBACK_LANGUAGE = "en_us";

	public static final String INTRO = "intro";
	public static final String WRENCH = "wrench";
	public static final String COMPONENTS = "components";
	public static final String NETWORK = "network";
	public static final String TERMINAL = "terminal";
	public static final String CABLES = "cables";
	public static final String BRIDGE = "bridge";
	public static final String CELLS = "cells";
	public static final String PLACEMENT = "placement";
	public static final String BUILTIN_TERMINAL = "builtin_terminal";
	public static final String STORAGE_BAY = "storage_bay";
	public static final String TUBES = "tubes";
	public static final String CATALOGUE_CARDS = "catalogue_cards";
	public static final String ADVANCED_STATION = "advanced_station";
	public static final String FILTER_CARDS = "filter_cards";
	public static final String PROGRAM_DECK = "program_deck";
	public static final String TERMINAL_DISKS = "terminal_disks";
	public static final String ADAPTERS = "adapters";
	public static final String PROGRAM_FLOPPY = "program_floppy";
	public static final String CRAFTING_COMPUTER = "crafting_computer";
	public static final String DATA_PROCESSOR = "data_processor";
	public static final String PATTERN_FLOPPY = "pattern_floppy";
	public static final String ORDERING = "ordering";
	public static final String OTHER_PARTS = "other_parts";
	public static final String NEXT = "next";
	public static final String PALM = "palm";
	public static final String CODES = "codes";

	public static final String ERA = "era";
	public static final String MEMORY = "memory";
	public static final String PART = "part";
	public static final String NEXT_GUIDE = "next_guide";
	public static final String NEXT_ERA = "next_era";

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");
	private static final Pattern CODE_LINE = Pattern.compile("(\\d{3}):\\s*(.*)");

	private GuideTexts()
	{
	}

	public static String era(ComputerEra era)
	{
		return era.getSerializedName();
	}

	public static String eraTerminal(ComputerEra era)
	{
		return era.getSerializedName()+"_terminal";
	}

	public static Map<String, Set<String>> pages()
	{
		Map<String, Set<String>> pages = new LinkedHashMap<>();
		pages.put(INTRO, Set.of(ERA));
		for(String page : List.of(WRENCH, COMPONENTS, NETWORK, TERMINAL, CABLES, BRIDGE, CELLS, PLACEMENT))
			pages.put(page, Set.of());
		for(ComputerEra era : ComputerEra.values())
		{
			pages.put(era(era), Set.of(MEMORY, PART));
			if(era.hasTerminal())
				pages.put(eraTerminal(era), Set.of());
		}
		for(String page : List.of(BUILTIN_TERMINAL, STORAGE_BAY, TUBES, CATALOGUE_CARDS, ADVANCED_STATION, FILTER_CARDS,
				PROGRAM_DECK, TERMINAL_DISKS, ADAPTERS, PROGRAM_FLOPPY, CRAFTING_COMPUTER, DATA_PROCESSOR,
				PATTERN_FLOPPY, ORDERING, OTHER_PARTS))
			pages.put(page, Set.of());
		pages.put(NEXT, Set.of(NEXT_GUIDE, NEXT_ERA));
		pages.put(PALM, Set.of());
		pages.put(CODES, Set.of());
		return pages;
	}

	public static String path(String language, String page)
	{
		return FOLDER+"/"+language+"/"+page+".txt";
	}

	public record Page(String title, String contents, String body)
	{
	}

	public static Page parse(String file)
	{
		String[] lines = file.replace("\r", "").split("\n", 3);
		String title = lines[0].strip();
		String contents = lines.length>1? lines[1].strip(): "";
		String body = lines.length>2? lines[2].stripTrailing(): "";
		return new Page(title, contents.isEmpty()? title: contents, body);
	}

	public static Set<String> placeholders(String text)
	{
		Set<String> found = new LinkedHashSet<>();
		Matcher matcher = PLACEHOLDER.matcher(text);
		while(matcher.find())
			found.add(matcher.group(1));
		return found;
	}

	public static List<String> split(String text)
	{
		List<String> parts = new ArrayList<>();
		Matcher matcher = PLACEHOLDER.matcher(text);
		int start = 0;
		while(matcher.find())
		{
			parts.add(text.substring(start, matcher.start()));
			parts.add(matcher.group(1));
			start = matcher.end();
		}
		parts.add(text.substring(start));
		return parts;
	}

	public static Map<Integer, String> codes(String body)
	{
		Map<Integer, String> codes = new LinkedHashMap<>();
		for(String line : body.split("\n"))
		{
			Matcher matcher = CODE_LINE.matcher(line.strip());
			if(matcher.matches())
				codes.put(Integer.parseInt(matcher.group(1)), matcher.group(2));
		}
		return codes;
	}
}
