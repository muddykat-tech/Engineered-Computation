package com.muddykat.engineeredcomputation.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.compat.patchouli.PatchouliCompat;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.guide.GuideOutline;
import com.muddykat.engineeredcomputation.guide.GuideTexts;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.registration.ECContent;

final class HandbookExport
{
	private static final String LANGUAGE = GuideTexts.FALLBACK_LANGUAGE;
	private static final String BOOK = "patchouli_books/handbook/";
    // Patchouli's page is 116 px wide with 9 px lines; these leave a line spare for the page number.
	private static final int WIDTH = 116;
	private static final int FIRST_PAGE_LINES = 13;
	private static final int TITLED_PAGE_LINES = 15;
	private static final int PAGE_LINES = 16;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Minecraft mc;
	private final Level level;
	private final Path guides;
	private final Map<String, JsonObject> files = new LinkedHashMap<>();

	private HandbookExport(Minecraft mc, Path project)
	{
		this.mc = mc;
		this.level = mc.level;
		this.guides = project.resolve("src/main/resources/assets").resolve(EngineeredComputation.MODID)
				.resolve(GuideTexts.FOLDER).resolve(LANGUAGE);
	}

	/** Writes into the source tree and the build's resources, so the running game can reload them straight away. */
	static void export(Minecraft mc) throws IOException
	{
		Path project = projectRoot();
		HandbookExport export = new HandbookExport(mc, project);
		export.build();
		for(Path resources : List.of(project.resolve("src/main/resources"), project.resolve("build/resources/main")))
		{
			Path book = resources.resolve("assets").resolve(EngineeredComputation.MODID).resolve(BOOK).resolve(LANGUAGE);
			if(!Files.isDirectory(resources))
				continue;
			if(Files.isDirectory(book))
				try(Stream<Path> old = Files.walk(book))
				{
					for(Path path : old.sorted(Comparator.reverseOrder()).toList())
						Files.delete(path);
				}
			for(Map.Entry<String, JsonObject> file : export.files.entrySet())
			{
				Path path = book.resolve(file.getKey());
				Files.createDirectories(path.getParent());
				Files.writeString(path, GSON.toJson(file.getValue())+"\n", StandardCharsets.UTF_8);
			}
		}
		EngineeredComputation.LOGGER.info("Handbook export: wrote {} files", export.files.size());
	}

	private static Path projectRoot()
	{
		Path path = Path.of("").toAbsolutePath();
		while(path!=null&&!Files.exists(path.resolve("settings.gradle")))
			path = path.getParent();
		if(path==null)
			throw new IllegalStateException("The handbook export has to run inside the project");
		return path;
	}

	private void build() throws IOException
	{
		Map<String, List<GuideOutline.Step>> layout = HandbookLayout.of(level);
		category(HandbookLayout.BASICS, Component.translatable("book.engineered_computation.handbook.basics"),
				"book.engineered_computation.handbook.basics.description", "engineered_computation:assembly_wrench", 0);
		entries(HandbookLayout.BASICS, layout.get(HandbookLayout.BASICS));
		for(ComputerEra era : ComputerEra.values())
		{
			String name = GuideTexts.era(era);
			String tagline = paragraphs(page(name).body()).getFirst();
			category(name, era.displayName(), tagline, itemId(ECContent.component(era).get()), era.ordinal()+1);
			entries(name, layout.get(name));
		}
	}

	private void category(String id, Component name, String description, String icon, int sortnum)
	{
		JsonObject category = new JsonObject();
		category.addProperty("name", key(name));
		category.addProperty("description", description);
		category.addProperty("icon", icon);
		category.addProperty("sortnum", sortnum);
		files.put("categories/"+id+".json", category);
	}

	private void entries(String category, List<GuideOutline.Step> steps) throws IOException
	{
		Entry entry = null;
		int sortnum = 0;
		for(GuideOutline.Step step : steps)
		{
			if(step instanceof GuideOutline.Heading heading)
			{
				if(entry!=null)
					entry.write();
				if(heading.page()!=null&&HandbookLayout.EDITION_ONLY.contains(heading.page()))
				{
					entry = null;
					continue;
				}
				entry = new Entry(category, heading, sortnum++);
				continue;
			}
			if(entry!=null)
				entry.add(step);
		}
		if(entry!=null)
			entry.write();
	}

	private final class Entry
	{
		private final String category;
		private final String id;
		private final String name;
		private final int sortnum;
		private final JsonArray pages = new JsonArray();
		private final List<ResourceLocation> recipes = new ArrayList<>();
		private String icon;

		Entry(String category, GuideOutline.Heading heading, int sortnum) throws IOException
		{
			this.category = category;
			this.id = heading.id();
			this.name = heading.page()!=null? contents(heading.page()): key(heading.label());
			this.sortnum = sortnum;
		}

		void add(GuideOutline.Step step) throws IOException
		{
			if(step instanceof GuideOutline.Recipe recipe)
			{
				recipes.add(recipe.id());
				if(icon==null)
					icon = resultOf(recipe.id());
				return;
			}
			flushRecipes();
			switch(step)
			{
				case GuideOutline.Text text -> text(text);
				case GuideOutline.Structure structure -> structure(structure);
				case GuideOutline.Codes codes -> codes();
				default -> throw new IllegalStateException("Unexpected step "+step);
			}
		}

		private void text(GuideOutline.Text text) throws IOException
		{
			GuideTexts.Page page = page(text.page());
			List<Word> words = new ArrayList<>();
			String body = page.body();
			for(Map.Entry<String, GuideOutline.Value> value : text.values().entrySet())
				body = body.replace("{"+value.getKey()+"}", "\u0000"+value.getKey()+"\u0000");
			List<List<Word>> paragraphs = new ArrayList<>();
			for(String paragraph : paragraphs(body))
			{
				List<Word> line = new ArrayList<>();
				for(String raw : paragraph.split("((?<=\n)|(?=\n))| "))
					if(raw.equals("\n"))
						line.add(Word.BREAK);
					else if(!raw.isEmpty())
						line.add(word(raw, text.values()));
				paragraphs.add(line);
			}
			String title = pages.isEmpty()? null: page.title();
			paginate(title, paragraphs);
		}

		private Word word(String raw, Map<String, GuideOutline.Value> values)
		{
			StringBuilder written = new StringBuilder(), shown = new StringBuilder();
			String[] parts = raw.split("\u0000", -1);
			for(int i = 0; i<parts.length; i++)
				if(i%2==0)
				{
					written.append(parts[i]);
					shown.append(parts[i]);
				}
				else
				{
					GuideOutline.Value value = values.get(parts[i]);
					written.append("$(").append(value.patchouli()).append(")");
					shown.append(value.now().get().getString());
				}
			return new Word(written.toString(), shown.toString());
		}

		/** Fills pages line by line as Patchouli wraps them, starting a new text page when one is full. */
		private void paginate(String title, List<List<Word>> paragraphs)
		{
			int capacity = pages.isEmpty()? FIRST_PAGE_LINES: title!=null? TITLED_PAGE_LINES: PAGE_LINES;
			StringBuilder text = new StringBuilder();
			int lines = 0, lineWidth = 0;
			boolean pageStart = true;
			// Patchouli wraps the text itself, so every space is kept, even where this count wraps a line.
			boolean spaced = false;
			for(int p = 0; p<paragraphs.size(); p++)
			{
				if(!pageStart)
				{
					if(lines+2>capacity)
					{
						textPage(title, text);
						title = null;
						capacity = PAGE_LINES;
						text.setLength(0);
						lines = 0;
					}
					else
					{
						text.append("$(br2)");
						lines += 1;
					}
				}
				spaced = false;
				lineWidth = 0;
				lines += 1;
				pageStart = false;
				for(Word word : paragraphs.get(p))
				{
					if(word==Word.BREAK)
					{
						text.append("$(br)");
						lines += 1;
						lineWidth = 0;
						spaced = false;
						continue;
					}
					int width = width(word.shown());
					int space = lineWidth==0? 0: width(" ");
					if(lineWidth>0&&lineWidth+space+width>WIDTH)
					{
						lines += 1;
						lineWidth = 0;
						space = 0;
					}
					if(lines>capacity)
					{
						textPage(title, text);
						title = null;
						capacity = PAGE_LINES;
						text.setLength(0);
						lines = 1;
						lineWidth = 0;
						space = 0;
						spaced = false;
					}
					if(spaced)
						text.append(' ');
					spaced = true;
					text.append(word.written());
					lineWidth += space+width;
				}
			}
			if(!text.isEmpty())
				textPage(title, text);
		}

		private void textPage(String title, StringBuilder text)
		{
			JsonObject page = new JsonObject();
			page.addProperty("type", "patchouli:text");
			if(title!=null)
				page.addProperty("title", title);
			page.addProperty("text", text.toString());
			pages.add(page);
		}

		private void structure(GuideOutline.Structure structure)
		{
			JsonObject page = new JsonObject();
			page.addProperty("type", "patchouli:multiblock");
			page.addProperty("name", key(structure.name()));
			page.addProperty("multiblock_id", PatchouliCompat.multiblockId(structure.id()).toString());
			page.addProperty("enable_visualize", true);
			String hint = Component.translatable("guide.engineered_computation.structure.handbook_hint",
					trigger(structure.geometry())).getString();
			page.addProperty("text", structure.slots()==null? hint: structure.slots().getString()+"$(br2)"+hint);
			pages.add(page);
			if(icon==null)
				icon = itemId(structure.casing().asItem());
		}

		private void codes() throws IOException
		{
			Map<Integer, String> meanings = GuideTexts.codes(page(GuideTexts.CODES).body());
			List<List<Word>> paragraphs = new ArrayList<>();
			for(StatusCode code : StatusCode.values())
			{
				String title = "$(l)$("+GuideOutline.CODE_FUNCTION+":"+code.code()+")$()";
				List<Word> paragraph = new ArrayList<>();
				paragraph.add(new Word(title, code.title().getString()));
				paragraph.add(Word.BREAK);
				for(String word : meanings.getOrDefault(code.code(), "").split(" "))
					if(!word.isEmpty())
						paragraph.add(new Word(word, word));
				paragraphs.add(paragraph);
			}
			paginate(null, paragraphs);
		}

		private void flushRecipes()
		{
			for(int i = 0; i<recipes.size(); i += 2)
			{
				JsonObject page = new JsonObject();
				page.addProperty("type", "patchouli:crafting");
				page.addProperty("recipe", recipes.get(i).toString());
				if(i+1<recipes.size())
					page.addProperty("recipe2", recipes.get(i+1).toString());
				pages.add(page);
			}
			recipes.clear();
		}

		void write()
		{
			flushRecipes();
			if(pages.isEmpty())
				return;
			JsonObject entry = new JsonObject();
			entry.addProperty("name", name);
			entry.addProperty("icon", icon!=null? icon: itemId(ECContent.guide(ComputerEra.PUNCHCARD).get()));
			entry.addProperty("category", EngineeredComputation.MODID+":"+category);
			entry.addProperty("sortnum", sortnum);
			entry.addProperty("read_by_default", true);
			entry.add("pages", pages);
			files.put("entries/"+category+"/"+id+".json", entry);
		}
	}

	/** Where the casing to wrench is, seen from the front, from the geometry's trigger. */
	static String trigger(EraDriveGeometry geometry)
	{
		BlockPos size = geometry.size(), at = geometry.trigger();
		String column = size.getX()==1? null: at.getX()==0? "left": at.getX()==size.getX()-1? "right"
				: size.getX()%2==1&&at.getX()==size.getX()/2? "middle": ordinal(at.getX()+1)+" from the left";
		String row = size.getY()==1? null: at.getY()==0? "bottom row": at.getY()==size.getY()-1? "top row"
				: ordinal(at.getY()+1)+" row from the bottom";
		String casing = column==null? "casing": column.contains(" ")? column.replace(" from", " casing from"): column+" casing";
		return row==null? casing: casing+" on the "+row;
	}

	private static String ordinal(int n)
	{
		return switch(n)
		{
			case 2 -> "second";
			case 3 -> "third";
			case 4 -> "fourth";
			default -> n+"th";
		};
	}

	private GuideTexts.Page page(String page) throws IOException
	{
		return GuideTexts.parse(Files.readString(guides.resolve(page+".txt"), StandardCharsets.UTF_8));
	}

	private String contents(String page) throws IOException
	{
		GuideTexts.Page text = page(page);
		return text.contents().isBlank()? text.title(): text.contents();
	}

	/** Paragraphs split on blank lines, keeping single line breaks inside them. */
	private static List<String> paragraphs(String body)
	{
		List<String> paragraphs = new ArrayList<>();
		for(String paragraph : body.strip().split("\n\\s*\n"))
			if(!paragraph.isBlank())
				paragraphs.add(paragraph.strip());
		return paragraphs;
	}

	private int width(String text)
	{
		return mc.font.width(FormattedText.of(text, Style.EMPTY.withFont(Minecraft.UNIFORM_FONT)));
	}

	/** A translation key Patchouli can look up in the reader's language, or the English text when it has arguments. */
	private static String key(Component component)
	{
		return component.getContents() instanceof TranslatableContents translatable&&translatable.getArgs().length==0
				? translatable.getKey(): component.getString();
	}

	private String resultOf(ResourceLocation recipe)
	{
		RecipeHolder<?> holder = level.getRecipeManager().byKey(recipe).orElse(null);
		ItemStack result = holder==null? ItemStack.EMPTY: holder.value().getResultItem(level.registryAccess());
		return result.isEmpty()? null: itemId(result.getItem());
	}

	private static String itemId(Item item)
	{
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	private record Word(String written, String shown)
	{
		static final Word BREAK = new Word("\n", "");
	}
}
