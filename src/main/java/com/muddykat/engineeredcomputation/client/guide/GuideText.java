package com.muddykat.engineeredcomputation.client.guide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.item.GuideTexts;

public final class GuideText
{
	private static final Map<String, GuideTexts.Page> PAGES = new HashMap<>();

	public static final ResourceManagerReloadListener RELOAD = manager -> PAGES.clear();

	private GuideText()
	{
	}

	static GuideTexts.Page page(String page)
	{
		String language = Minecraft.getInstance().getLanguageManager().getSelected().getCode();
		return PAGES.computeIfAbsent(language+"/"+page, key -> read(language, page)
				.or(() -> read(GuideTexts.FALLBACK_LANGUAGE, page))
				.orElseGet(() -> {
					EngineeredComputation.LOGGER.warn("No guide page {}", GuideTexts.path(GuideTexts.FALLBACK_LANGUAGE, page));
					return new GuideTexts.Page(page, page, "");
				}));
	}

	static Component title(String page)
	{
		return new TextComponent(page(page).title());
	}

	static Component contents(String page)
	{
		return new TextComponent(page(page).contents());
	}

	static Component body(String page, Map<String, Component> values)
	{
		return fill(page(page).body(), values);
	}

	static Component body(String page)
	{
		return body(page, Map.of());
	}

	static Component fill(String text, Map<String, Component> values)
	{
		MutableComponent filled = new TextComponent("");
		List<String> parts = GuideTexts.split(text);
		for(int i = 0; i<parts.size(); i++)
		{
			String part = parts.get(i);
			if(i%2==0)
				filled.append(part);
			else
				filled.append(values.getOrDefault(part, new TextComponent("{"+part+"}")));
		}
		return filled;
	}

	private static Optional<GuideTexts.Page> read(String language, String page)
	{
		ResourceManager resources = Minecraft.getInstance().getResourceManager();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID,
				GuideTexts.path(language, page));
		if(!resources.hasResource(id))
			return Optional.empty();
		try(Resource resource = resources.getResource(id);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)))
		{
			return Optional.of(GuideTexts.parse(reader.lines().collect(Collectors.joining("\n"))));
		}
		catch(IOException e)
		{
			EngineeredComputation.LOGGER.warn("Couldn't read guide page {}", id, e);
			return Optional.empty();
		}
	}
}
