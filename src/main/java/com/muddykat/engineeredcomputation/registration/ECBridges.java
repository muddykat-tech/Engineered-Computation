package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.network.StorageBridgeBlock;

public final class ECBridges
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	private static final DeferredBlock<StorageBridgeBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"storage_bridge", () -> new StorageBridgeBlock(PROPERTIES));
	private static final DeferredItem<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"storage_bridge", () -> new BlockItem(BLOCK.get(), new Item.Properties()));

	private ECBridges()
	{
	}

	public static void init()
	{
	}

	public static DeferredBlock<StorageBridgeBlock> block()
	{
		return BLOCK;
	}

	public static DeferredItem<BlockItem> item()
	{
		return ITEM;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(
				ECCapabilities.NETWORK_MEMBER,
				(level, pos, state, blockEntity, side) -> StorageBridgeBlock.connectsOn(state, side)
						? StorageBridgeBlock.NETWORK_MEMBER: null,
				BLOCK.get()));
	}
}
