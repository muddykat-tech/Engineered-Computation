package com.muddykat.engineeredcomputation.guide;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("guide")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class GuideTextGameTests
{
	private GuideTextGameTests()
	{
	}

	private static Path guideFolder()
	{
		return ModList.get().getModFileById(EngineeredComputation.MODID).getFile()
				.findResource("assets", EngineeredComputation.MODID, GuideTexts.FOLDER);
	}

	private static Path english(String id)
	{
		return guideFolder().resolve(GuideTexts.FALLBACK_LANGUAGE).resolve(id+".txt");
	}

	private static List<Path> languages()
	{
		try(Stream<Path> files = Files.list(guideFolder()))
		{
			return files.filter(Files::isDirectory).toList();
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static List<Path> pageFiles(Path language)
	{
		try(Stream<Path> files = Files.list(language))
		{
			return files.filter(file -> file.getFileName().toString().endsWith(".txt")).toList();
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static GuideTexts.Page read(Path file)
	{
		try
		{
			return GuideTexts.parse(Files.readString(file, StandardCharsets.UTF_8));
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyPageHasAnEnglishFile(GameTestHelper helper)
	{
		for(String id : GuideTexts.pages().keySet())
			StorageGameTests.check(helper, Files.isRegularFile(english(id)), "no English guide page "+id);
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyPageHasATitleAndText(GameTestHelper helper)
	{
		for(String id : GuideTexts.pages().keySet())
		{
			GuideTexts.Page page = read(english(id));
			StorageGameTests.check(helper, !page.title().isBlank(), id+": no title");
			StorageGameTests.check(helper, !page.body().isBlank(), id+": no text");
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void placeholdersAreOnesThePageFillsIn(GameTestHelper helper)
	{
		for(Path language : languages())
			for(Path file : pageFiles(language))
			{
				String name = file.getFileName().toString();
				String id = name.substring(0, name.length()-".txt".length());
				String where = language.getFileName()+"/"+id;
				StorageGameTests.check(helper, GuideTexts.pages().containsKey(id), where+": not a guide page");
				GuideTexts.Page page = read(file);
				for(String placeholder : GuideTexts.placeholders(page.title()+"\n"+page.body()))
					StorageGameTests.check(helper, GuideTexts.pages().get(id).contains(placeholder),
							where+": unknown placeholder {"+placeholder+"}");
			}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyStatusCodeIsExplained(GameTestHelper helper)
	{
		Map<Integer, String> codes = GuideTexts.codes(read(english(GuideTexts.CODES)).body());
		for(StatusCode code : StatusCode.values())
		{
			String meaning = codes.get(code.code());
			StorageGameTests.check(helper, meaning!=null&&!meaning.isBlank(), "codes: no meaning for "+code.code());
		}
		helper.succeed();
	}
}
