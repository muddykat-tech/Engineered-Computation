package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class ECRegistrationHolder
{
	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, EngineeredComputation.MODID);
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, EngineeredComputation.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registry.BLOCK_ENTITY_TYPE_REGISTRY, EngineeredComputation.MODID);

	private ECRegistrationHolder()
	{
	}

	/** A plain block item named after its block (NeoForge's {@code Items.registerSimpleBlockItem}). */
	public static RegistryObject<BlockItem> simpleBlockItem(RegistryObject<? extends Block> block)
	{
		return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(),
				new Item.Properties().tab(EngineeredComputation.TAB)));
	}

	/** A plain item (NeoForge's {@code Items.registerSimpleItem}). */
	public static RegistryObject<Item> simpleItem(String name)
	{
		return ITEMS.register(name, () -> new Item(new Item.Properties().tab(EngineeredComputation.TAB)));
	}

	public static void register(IEventBus modEventBus)
	{
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
	}
}
