package com.muddykat.engineeredcomputation.registration;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.cable.NetworkCableBlock;
import com.muddykat.engineeredcomputation.remote.RemoteLinkBlock;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlock;
import com.muddykat.engineeredcomputation.remote.StorageAnalyzerBlockEntity;

/** The era 5 machine that links a data network to storage it doesn't hold. */
public final class ECRemote
{
	public static final String ANALYZER_COST = "storageAnalyzer";

	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(RemoteLinkBlock.ACTIVE)? 3: 0);

	public static final DeferredBlock<StorageAnalyzerBlock> ANALYZER = ECRegistrationHolder.BLOCKS.register(
			"storage_analyzer", () -> new StorageAnalyzerBlock(PROPERTIES));
	public static final DeferredItem<BlockItem> ANALYZER_ITEM = ECRegistrationHolder.ITEMS.register(
			"storage_analyzer", () -> new BlockItem(ANALYZER.get(), new Item.Properties()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageAnalyzerBlockEntity>> ANALYZER_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("storage_analyzer", () -> BlockEntityType.Builder.of(
					StorageAnalyzerBlockEntity::new, ANALYZER.get()).build(null));

	private ECRemote()
	{
	}

	public static void init()
	{
	}

	public static List<DeferredItem<BlockItem>> items()
	{
		return List.of(ANALYZER_ITEM);
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(
				ECCapabilities.NETWORK_MEMBER,
				(level, pos, state, blockEntity, side) -> {
					NetworkMember member = blockEntity instanceof StorageAnalyzerBlockEntity analyzer? analyzer.member()
							: null;
					if(member==null||side==null)
						return member;
					Direction back = state.getValue(RemoteLinkBlock.FACING).getOpposite();
					return side==back&&level.getBlockState(pos.relative(back)).getBlock() instanceof NetworkCableBlock
							? member: null;
				},
				ANALYZER.get()));
	}
}
