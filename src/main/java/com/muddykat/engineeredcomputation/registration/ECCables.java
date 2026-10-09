package com.muddykat.engineeredcomputation.registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.network.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;

public final class ECCables
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(0.6F)
			.sound(SoundType.COPPER)
			.noOcclusion();

	private static final RegistryObject<NetworkCableBlock> BLOCK = ECRegistrationHolder.BLOCKS.register(
			"network_cable", () -> new NetworkCableBlock(PROPERTIES));
	private static final RegistryObject<BlockItem> ITEM = ECRegistrationHolder.ITEMS.register(
			"network_cable", () -> new BlockItem(BLOCK.get(), new Item.Properties()));

	private static final RegistryObject<CoveredCableBlock> COVERED = ECRegistrationHolder.BLOCKS.register(
			"covered_network_cable", () -> new CoveredCableBlock(false, BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE).strength(0.6F).sound(SoundType.COPPER)));
	private static final RegistryObject<CoveredCableBlock> CLEAR_COVERED = ECRegistrationHolder.BLOCKS.register(
			"clear_covered_network_cable", () -> new CoveredCableBlock(true, BlockBehaviour.Properties.of()
					.mapColor(MapColor.NONE).strength(0.6F).sound(SoundType.COPPER).noOcclusion()
					.isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
	public static final RegistryObject<BlockEntityType<CoveredCableBlockEntity>> COVERED_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("covered_network_cable", () -> BlockEntityType.Builder.of(
					CoveredCableBlockEntity::new, COVERED.get(), CLEAR_COVERED.get()).build(null));

	private ECCables()
	{
	}

	public static void init()
	{
	}

	public static RegistryObject<NetworkCableBlock> block()
	{
		return BLOCK;
	}

	public static RegistryObject<BlockItem> item()
	{
		return ITEM;
	}

	public static RegistryObject<CoveredCableBlock> covered()
	{
		return COVERED;
	}

	public static RegistryObject<CoveredCableBlock> clearCovered()
	{
		return CLEAR_COVERED;
	}

	public static void register(IEventBus modEventBus)
	{
		MinecraftForge.EVENT_BUS.addListener(CoveredCableBlock::onRightClickBlock);
	}
}
