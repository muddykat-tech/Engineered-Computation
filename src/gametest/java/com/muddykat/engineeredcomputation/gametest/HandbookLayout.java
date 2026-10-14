package com.muddykat.engineeredcomputation.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.guide.GuideOutline;
import com.muddykat.engineeredcomputation.guide.GuideTexts;

/** Which parts of the guides' outline go in which handbook category; shared by the export and its tests. */
public final class HandbookLayout
{
	public static final String BASICS = "basics";
	/** Guide pages the per-era editions have but a single handbook doesn't need. */
	public static final Set<String> EDITION_ONLY = Set.of(GuideTexts.INTRO, GuideTexts.NEXT, GuideTexts.PALM);

	private HandbookLayout()
	{
	}

	/**
	 * Each handbook category's part of the guides' outline: the chapters every era shares, taken from the last era's
	 * guide, which has them all, then each era's own chapters.
	 */
	public static Map<String, List<GuideOutline.Step>> of(Level level)
	{
		Map<String, List<GuideOutline.Step>> layout = new LinkedHashMap<>();
		List<GuideOutline.Step> last = GuideOutline.of(ComputerEra.FLOPPY, level.getRecipeManager(),
				level.registryAccess());
		List<GuideOutline.Step> basics = new ArrayList<>();
		basics.addAll(between(last, GuideTexts.WRENCH, GuideTexts.era(ComputerEra.FLOPPY)));
		basics.addAll(between(last, GuideTexts.CODES, null));
		layout.put(BASICS, basics);
		for(ComputerEra era : ComputerEra.values())
		{
			String name = GuideTexts.era(era);
			List<GuideOutline.Step> steps = GuideOutline.of(era, level.getRecipeManager(), level.registryAccess());
			List<GuideOutline.Step> own = new ArrayList<>();
			for(String stop : List.of(GuideTexts.NEXT, GuideTexts.PALM, GuideTexts.CODES))
				if(own.isEmpty())
					own.addAll(between(steps, name, stop));
			layout.put(name, own);
		}
		return layout;
	}

	/** The steps from the heading {@code from} up to the heading {@code to}, or the end; empty if either is missing. */
	private static List<GuideOutline.Step> between(List<GuideOutline.Step> steps, String from, String to)
	{
		int start = -1, end = steps.size();
		for(int i = 0; i<steps.size(); i++)
			if(steps.get(i) instanceof GuideOutline.Heading heading&&!heading.section())
				if(heading.id().equals(from)&&start<0)
					start = i;
				else if(start>=0&&heading.id().equals(to))
				{
					end = i;
					break;
				}
		return start<0||(to!=null&&end==steps.size())? List.of(): steps.subList(start, end);
	}
}
