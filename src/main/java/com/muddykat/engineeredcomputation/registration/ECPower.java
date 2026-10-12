package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlock;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlockEntity;

public final class ECPower
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties
			.of(Material.METAL, MaterialColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	public static final RegistryObject<EnergyAccepterBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"energy_accepter", () -> new EnergyAccepterBlock(PROPERTIES));
	public static final RegistryObject<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"energy_accepter", () -> new BlockItem(BLOCK.get(), new Item.Properties().tab(EngineeredComputation.TAB)));
	public static final RegistryObject<BlockEntityType<EnergyAccepterBlockEntity>> BLOCK_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("energy_accepter", () -> BlockEntityType.Builder.of(
					EnergyAccepterBlockEntity::new, BLOCK.get()).build(null));

	private ECPower()
	{
	}

	public static void init()
	{
	}

	public static void register(IEventBus modEventBus)
	{
	}
}
