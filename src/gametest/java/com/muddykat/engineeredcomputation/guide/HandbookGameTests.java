package com.muddykat.engineeredcomputation.guide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.HandbookLayout;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.network.NetworkStressGameTests;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;

@TestCategory("guide")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class HandbookGameTests
{
	private static final String BOOK = "assets/"+EngineeredComputation.MODID+"/patchouli_books/handbook/en_us/";

	private HandbookGameTests()
	{
	}

	/** The handbook is exported from the guides by hand, so this catches a guide change made since. */
	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void handbookMatchesTheGuides(GameTestHelper helper) throws IOException
	{
		List<String> problems = new ArrayList<>();
		Set<String> expected = new TreeSet<>();
		for(Map.Entry<String, List<GuideOutline.Step>> category : HandbookLayout.of(helper.getLevel()).entrySet())
		{
			if(read("categories/"+category.getKey()+".json")==null)
				problems.add("no category "+category.getKey());
			Expected entry = null;
			for(GuideOutline.Step step : category.getValue())
				switch(step)
				{
					case GuideOutline.Heading heading -> {
						if(entry!=null)
							entry.check(problems, expected);
						entry = heading.page()!=null&&HandbookLayout.EDITION_ONLY.contains(heading.page())? null
								: new Expected(category.getKey()+"/"+heading.id());
					}
					case GuideOutline.Recipe recipe -> {
						if(entry!=null)
							entry.recipes.add(recipe.id().toString());
					}
					case GuideOutline.Structure structure -> {
						if(entry!=null)
							entry.multiblocks.add(id(structure.id()).toString());
					}
					default -> {
						if(entry!=null)
							entry.pages = true;
					}
				}
			if(entry!=null)
				entry.check(problems, expected);
		}
		Path entries = ModList.get().getModFileById(EngineeredComputation.MODID).getFile().findResource(BOOK+"entries");
		if(Files.isDirectory(entries))
			try(Stream<Path> files = Files.walk(entries))
			{
				files.filter(Files::isRegularFile)
						.map(file -> entries.relativize(file).toString().replace('\\', '/').replace(".json", ""))
						.filter(name -> !expected.contains(name))
						.forEach(name -> problems.add("entry "+name+" isn't in the guides any more"));
			}
		StorageGameTests.check(helper, problems.isEmpty(),
				"The handbook is out of date; run ./gradlew runClient -Pvisuals=handbook to export it again: "+problems);
		helper.succeed();
	}

	/** The in-world preview anchors on a structure's own corner, so casings built there have to match. */
	@GameTest(template = NetworkStressGameTests.BIG)
	public static void multiblocksMatchTheirDrives(GameTestHelper helper)
	{
		if(!ModList.get().isLoaded("patchouli"))
		{
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		for(EraDrive drive : List.of(EraDrive.DRUM_COMPUTER, EraDrive.DISK_MAINFRAME, EraDrive.FLOPPY_PROCESSOR))
		{
			IMultiblock multiblock = PatchouliAPI.get().getMultiblock(id(drive.getSerializedName()));
			StorageGameTests.check(helper, multiblock!=null, drive+" has no handbook multiblock");
			for(BlockPos pos : drive.geometry().shapes().keySet())
				level.setBlockAndUpdate(origin.offset(pos), ECContent.casing(drive.era()).get().defaultBlockState());
			StorageGameTests.check(helper, multiblock.validate(level, origin)!=null,
					drive+"'s handbook multiblock doesn't match casings built from its corner");
			for(BlockPos pos : drive.geometry().shapes().keySet())
				level.removeBlock(origin.offset(pos), false);
		}
		helper.succeed();
	}

	private static JsonObject read(String file) throws IOException
	{
		Path path = ModList.get().getModFileById(EngineeredComputation.MODID).getFile().findResource(BOOK+file);
		return Files.isRegularFile(path)
				? JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject(): null;
	}

	private static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}

	private static final class Expected
	{
		final String name;
		final List<String> recipes = new ArrayList<>();
		final List<String> multiblocks = new ArrayList<>();
		boolean pages;

		Expected(String name)
		{
			this.name = name;
		}

		void check(List<String> problems, Set<String> expected) throws IOException
		{
			if(!pages&&recipes.isEmpty()&&multiblocks.isEmpty())
				return;
			expected.add(name);
			JsonObject entry = read("entries/"+name+".json");
			if(entry==null)
			{
				problems.add("no entry "+name);
				return;
			}
			List<String> bookRecipes = new ArrayList<>(), bookMultiblocks = new ArrayList<>();
			for(JsonElement element : entry.getAsJsonArray("pages"))
			{
				JsonObject page = element.getAsJsonObject();
				for(String key : List.of("recipe", "recipe2"))
					if(page.has(key))
						bookRecipes.add(page.get(key).getAsString());
				if(page.has("multiblock_id"))
					bookMultiblocks.add(page.get("multiblock_id").getAsString());
			}
			if(!bookRecipes.equals(recipes))
				problems.add(name+" has recipes "+bookRecipes+", the guides "+recipes);
			if(!bookMultiblocks.equals(multiblocks))
				problems.add(name+" has multiblocks "+bookMultiblocks+", the guides "+multiblocks);
		}
	}
}
