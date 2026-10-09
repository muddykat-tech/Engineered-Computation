package com.muddykat.engineeredcomputation.registration;

import java.util.List;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.TubeMember;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.WindowedTubeItem;

public final class ECPneumatics
{
	private static final BlockBehaviour.Properties TUBE_PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.6F)
			.sound(SoundType.COPPER)
			.noOcclusion();
	private static final BlockBehaviour.Properties STATION_PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	public static final DeferredBlock<PneumaticTubeBlock> TUBE = ECRegistrationHolder.BLOCKS.register(
			"pneumatic_tube", () -> new PneumaticTubeBlock(TUBE_PROPERTIES));
	public static final DeferredBlock<TubeStationBlock> TUBE_STATION = ECRegistrationHolder.BLOCKS.register(
			"tube_station", () -> new TubeStationBlock(STATION_PROPERTIES));
	public static final DeferredBlock<ReceivingStationBlock> RECEIVING_STATION = ECRegistrationHolder.BLOCKS.register(
			"receiving_station", () -> new ReceivingStationBlock(STATION_PROPERTIES));

	public static final DeferredBlock<CoveredTubeBlock> COVERED_TUBE = ECRegistrationHolder.BLOCKS.register(
			"covered_pneumatic_tube", () -> new CoveredTubeBlock(false, BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE).strength(0.6F).sound(SoundType.COPPER)));
	public static final DeferredBlock<CoveredTubeBlock> CLEAR_COVERED_TUBE = ECRegistrationHolder.BLOCKS.register(
			"clear_covered_pneumatic_tube", () -> new CoveredTubeBlock(true, BlockBehaviour.Properties.of()
					.mapColor(MapColor.NONE).strength(0.6F).sound(SoundType.COPPER).noOcclusion()
					.isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

	private static final DeferredItem<BlockItem> TUBE_ITEM = ECRegistrationHolder.ITEMS.registerSimpleBlockItem(TUBE);
	public static final DeferredItem<WindowedTubeItem> WINDOWED_TUBE_ITEM = ECRegistrationHolder.ITEMS.register(
			"windowed_pneumatic_tube", () -> new WindowedTubeItem(TUBE.get(), new Item.Properties()));

	private static final List<DeferredItem<? extends BlockItem>> ITEMS = List.of(TUBE_ITEM, WINDOWED_TUBE_ITEM,
			ECRegistrationHolder.ITEMS.registerSimpleBlockItem(TUBE_STATION),
			ECRegistrationHolder.ITEMS.registerSimpleBlockItem(RECEIVING_STATION));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeStationBlockEntity>> TUBE_STATION_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("tube_station", () -> BlockEntityType.Builder.of(
					TubeStationBlockEntity::new, TUBE_STATION.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReceivingStationBlockEntity>>
			RECEIVING_STATION_ENTITY = ECRegistrationHolder.BLOCK_ENTITIES.register("receiving_station",
			() -> BlockEntityType.Builder.of(ReceivingStationBlockEntity::new, RECEIVING_STATION.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoveredTubeBlockEntity>> COVERED_TUBE_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("covered_pneumatic_tube", () -> BlockEntityType.Builder.of(
					CoveredTubeBlockEntity::new, COVERED_TUBE.get(), CLEAR_COVERED_TUBE.get()).build(null));

	private ECPneumatics()
	{
	}

	public static void init()
	{
	}

	public static List<DeferredItem<? extends BlockItem>> items()
	{
		return ITEMS;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> {
			event.registerBlock(ECCapabilities.TUBE_MEMBER, (level, pos, state, blockEntity, side) -> TubeMember.TUBE,
					TUBE.get(), COVERED_TUBE.get(), CLEAR_COVERED_TUBE.get());
			event.registerBlock(ECCapabilities.TUBE_MEMBER,
					(level, pos, state, blockEntity, side) -> TubeStationBlock.connectsOn(state, side)
							? TubeMember.END: null, TUBE_STATION.get());
			event.registerBlock(ECCapabilities.TUBE_MEMBER,
					(level, pos, state, blockEntity, side) -> TubeMember.END, RECEIVING_STATION.get());
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RECEIVING_STATION_ENTITY.get(),
					(receiver, side) -> receiver.getOutput());
		});
		NeoForge.EVENT_BUS.addListener(CoveredTubeBlock::onRightClickBlock);
		TubeNetwork.register();
		TubeCarriers.register();
	}
}
