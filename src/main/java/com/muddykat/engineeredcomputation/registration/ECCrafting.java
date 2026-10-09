package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.crafting.PatternFloppyItem;

public final class ECCrafting
{
	public static final RegistryObject<PatternFloppyItem> PATTERN_FLOPPY = ECRegistrationHolder.ITEMS.register(
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
