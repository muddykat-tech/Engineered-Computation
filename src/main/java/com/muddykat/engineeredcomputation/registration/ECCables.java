package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;

public final class ECCables
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(0.6F)
			.sound(SoundType.COPPER)
			.noOcclusion();

	private static final DeferredBlock<NetworkCableBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"network_cable", () -> new NetworkCableBlock(PROPERTIES));
	private static final DeferredItem<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"network_cable", () -> new BlockItem(BLOCK.get(), new Item.Properties()));

	private static final DeferredBlock<CoveredCableBlock> COVERED = ECRegistrationHolder.BLOCKS.register(
			"covered_network_cable", () -> new CoveredCableBlock(false, BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE).strength(0.6F).sound(SoundType.COPPER)));
	private static final DeferredBlock<CoveredCableBlock> CLEAR_COVERED = ECRegistrationHolder.BLOCKS.register(
			"clear_covered_network_cable", () -> new CoveredCableBlock(true, BlockBehaviour.Properties.of()
					.mapColor(MapColor.NONE).strength(0.6F).sound(SoundType.COPPER).noOcclusion()
					.isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConnectorBlockEntity>> CABLE_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("network_cable", () -> BlockEntityType.Builder.of(
					(pos, state) -> new ConnectorBlockEntity(ECCables.CABLE_ENTITY.get(), pos, state), BLOCK.get())
					.build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoveredCableBlockEntity>> COVERED_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("covered_network_cable", () -> BlockEntityType.Builder.of(
					CoveredCableBlockEntity::new, COVERED.get(), CLEAR_COVERED.get()).build(null));

	private ECCables()
	{
	}

	public static void init()
	{
	}

	public static DeferredBlock<NetworkCableBlock> block()
	{
		return BLOCK;
	}

	public static DeferredItem<BlockItem> item()
	{
		return ITEM;
	}

	public static DeferredBlock<CoveredCableBlock> covered()
	{
		return COVERED;
	}

	public static DeferredBlock<CoveredCableBlock> clearCovered()
	{
		return CLEAR_COVERED;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(
				ECCapabilities.NETWORK_MEMBER,
				(level, pos, state, blockEntity, side) ->
						side!=null&&blockEntity instanceof ConnectorBlockEntity connector&&connector.isCut(side)
								? null: NetworkCableBlock.NETWORK_MEMBER,
				BLOCK.get(), COVERED.get(), CLEAR_COVERED.get()));
		NeoForge.EVENT_BUS.addListener(CoveredCableBlock::onRightClickBlock);
	}
}
