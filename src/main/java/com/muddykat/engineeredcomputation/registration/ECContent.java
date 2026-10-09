package com.muddykat.engineeredcomputation.registration;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.item.AssemblyWrenchItem;
import com.muddykat.engineeredcomputation.item.GuideItem;

public final class ECContent
{
	private static final BlockBehaviour.Properties CASING_PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	private static final Map<ComputerEra, RegistryObject<Block>> CASINGS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, RegistryObject<BlockItem>> CASING_ITEMS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, RegistryObject<Item>> COMPONENTS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, RegistryObject<GuideItem>> GUIDES = new EnumMap<>(ComputerEra.class);

	static
	{
		for(ComputerEra era : ComputerEra.values())
		{
			RegistryObject<Block> casing = ECRegistrationHolder.BLOCKS.register(
					era.casingName(), () -> new Block(CASING_PROPERTIES));
			CASINGS.put(era, casing);
			CASING_ITEMS.put(era, ECRegistrationHolder.simpleBlockItem(casing));
			COMPONENTS.put(era, ECRegistrationHolder.simpleItem(era.componentName()));
			GUIDES.put(era, ECRegistrationHolder.ITEMS.register(
					era.guideName(), () -> new GuideItem(era, new Item.Properties().stacksTo(1))));
		}
	}

	public static final RegistryObject<Block> PROCESSOR_CASING = ECRegistrationHolder.BLOCKS.register(
			"processor_casing", () -> new Block(CASING_PROPERTIES));
	public static final RegistryObject<BlockItem> PROCESSOR_CASING_ITEM =
			ECRegistrationHolder.simpleBlockItem(PROCESSOR_CASING);

	public static final RegistryObject<AssemblyWrenchItem> ASSEMBLY_WRENCH = ECRegistrationHolder.ITEMS.register(
			"assembly_wrench", () -> new AssemblyWrenchItem(new Item.Properties().stacksTo(1)));

	private ECContent()
	{
	}

	public static void init()
	{
	}

	public static RegistryObject<Block> casing(ComputerEra era)
	{
		return CASINGS.get(era);
	}

	public static RegistryObject<BlockItem> casingItem(ComputerEra era)
	{
		return CASING_ITEMS.get(era);
	}

	public static RegistryObject<Item> component(ComputerEra era)
	{
		return COMPONENTS.get(era);
	}

	public static RegistryObject<GuideItem> guide(ComputerEra era)
	{
		return GUIDES.get(era);
	}
}
