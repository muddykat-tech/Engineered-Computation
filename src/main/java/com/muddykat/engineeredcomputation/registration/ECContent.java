package com.muddykat.engineeredcomputation.registration;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.guide.GuideItem;
import com.muddykat.engineeredcomputation.item.AssemblyWrenchItem;

public final class ECContent
{
	private static final BlockBehaviour.Properties CASING_PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	private static final Map<ComputerEra, DeferredBlock<Block>> CASINGS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, DeferredItem<BlockItem>> CASING_ITEMS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, DeferredItem<Item>> COMPONENTS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, DeferredItem<GuideItem>> GUIDES = new EnumMap<>(ComputerEra.class);

	static
	{
		for(ComputerEra era : ComputerEra.values())
		{
			DeferredBlock<Block> casing = ECRegistrationHolder.BLOCKS.register(
					era.casingName(), () -> new Block(CASING_PROPERTIES));
			CASINGS.put(era, casing);
			CASING_ITEMS.put(era, ECRegistrationHolder.ITEMS.registerSimpleBlockItem(casing));
			COMPONENTS.put(era, ECRegistrationHolder.ITEMS.registerSimpleItem(era.componentName()));
			GUIDES.put(era, ECRegistrationHolder.ITEMS.register(
					era.guideName(), () -> new GuideItem(era, new Item.Properties().stacksTo(1))));
		}
	}

	public static final DeferredBlock<Block> PROCESSOR_CASING = ECRegistrationHolder.BLOCKS.register(
			"processor_casing", () -> new Block(CASING_PROPERTIES));
	public static final DeferredItem<BlockItem> PROCESSOR_CASING_ITEM =
			ECRegistrationHolder.ITEMS.registerSimpleBlockItem(PROCESSOR_CASING);

	public static final DeferredItem<AssemblyWrenchItem> ASSEMBLY_WRENCH = ECRegistrationHolder.ITEMS.register(
			"assembly_wrench", () -> new AssemblyWrenchItem(new Item.Properties().stacksTo(1)));

	private ECContent()
	{
	}

	public static void init()
	{
	}

	public static DeferredBlock<Block> casing(ComputerEra era)
	{
		return CASINGS.get(era);
	}

	public static DeferredItem<BlockItem> casingItem(ComputerEra era)
	{
		return CASING_ITEMS.get(era);
	}

	public static DeferredItem<Item> component(ComputerEra era)
	{
		return COMPONENTS.get(era);
	}

	public static DeferredItem<GuideItem> guide(ComputerEra era)
	{
		return GUIDES.get(era);
	}
}
