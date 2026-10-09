package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.muddykat.engineeredcomputation.EngineeredComputation;

public final class ECRegistrationHolder
{
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EngineeredComputation.MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EngineeredComputation.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EngineeredComputation.MODID);

	private ECRegistrationHolder()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
	}
}
