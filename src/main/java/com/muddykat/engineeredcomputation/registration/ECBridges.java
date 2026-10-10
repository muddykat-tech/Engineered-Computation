package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.StorageBridgeBlock;

public final class ECBridges
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties
			.of(Material.METAL, MaterialColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	private static final RegistryObject<StorageBridgeBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"storage_bridge", () -> new StorageBridgeBlock(PROPERTIES));
	private static final RegistryObject<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"storage_bridge", () -> new BlockItem(BLOCK.get(), new Item.Properties().tab(EngineeredComputation.TAB)));

	private ECBridges()
	{
	}

	public static void init()
	{
	}

	public static RegistryObject<StorageBridgeBlock> block()
	{
		return BLOCK;
	}

	public static RegistryObject<BlockItem> item()
	{
		return ITEM;
	}

	public static void register(IEventBus modEventBus)
	{
	}
}
