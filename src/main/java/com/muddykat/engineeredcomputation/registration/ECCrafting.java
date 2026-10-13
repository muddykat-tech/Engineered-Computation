package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.crafting.pattern.PatternFloppyItem;

public final class ECCrafting
{
	public static final DeferredItem<PatternFloppyItem> PATTERN_FLOPPY = ECRegistrationHolder.ITEMS.register(
			"pattern_floppy", () -> new PatternFloppyItem(new Item.Properties().stacksTo(1)));

	private ECCrafting()
	{
	}

	public static void init()
	{
	}

	public static void register(IEventBus modEventBus)
	{
	}
}
