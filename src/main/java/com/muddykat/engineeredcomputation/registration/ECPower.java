package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlock;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlockEntity;

public final class ECPower
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	private static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
		@Override
		public boolean isEnergyAccepter()
		{
			return true;
		}
	};

	public static final DeferredBlock<EnergyAccepterBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"energy_accepter", () -> new EnergyAccepterBlock(PROPERTIES));
	public static final DeferredItem<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"energy_accepter", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyAccepterBlockEntity>> BLOCK_ENTITY =
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
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> {
			event.registerBlock(ECCapabilities.NETWORK_MEMBER, (level, pos, state, blockEntity, side) -> NETWORK_MEMBER,
					BLOCK.get());
			event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BLOCK_ENTITY.get(),
					(accepter, side) -> accepter.getEnergyStorage());
		});
	}
}
