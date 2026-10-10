package com.muddykat.engineeredcomputation.registration;

import java.util.List;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.WindowedTubeItem;

public final class ECPneumatics
{
	private static final BlockBehaviour.Properties TUBE_PROPERTIES = BlockBehaviour.Properties
			.of(Material.METAL, MaterialColor.COLOR_YELLOW)
			.strength(0.6F)
			.sound(SoundType.COPPER)
			.noOcclusion();
	private static final BlockBehaviour.Properties STATION_PROPERTIES = BlockBehaviour.Properties
			.of(Material.METAL, MaterialColor.COLOR_YELLOW)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops();

	public static final RegistryObject<PneumaticTubeBlock> TUBE = ECRegistrationHolder.BLOCKS.register(
			"pneumatic_tube", () -> new PneumaticTubeBlock(TUBE_PROPERTIES));
	public static final RegistryObject<TubeStationBlock> TUBE_STATION = ECRegistrationHolder.BLOCKS.register(
			"tube_station", () -> new TubeStationBlock(STATION_PROPERTIES));
	public static final RegistryObject<ReceivingStationBlock> RECEIVING_STATION = ECRegistrationHolder.BLOCKS.register(
			"receiving_station", () -> new ReceivingStationBlock(STATION_PROPERTIES));

	public static final RegistryObject<CoveredTubeBlock> COVERED_TUBE = ECRegistrationHolder.BLOCKS.register(
			"covered_pneumatic_tube", () -> new CoveredTubeBlock(false, BlockBehaviour.Properties
					.of(Material.METAL, MaterialColor.STONE).strength(0.6F).sound(SoundType.COPPER)));
	public static final RegistryObject<CoveredTubeBlock> CLEAR_COVERED_TUBE = ECRegistrationHolder.BLOCKS.register(
			"clear_covered_pneumatic_tube", () -> new CoveredTubeBlock(true, BlockBehaviour.Properties
					.of(Material.METAL, MaterialColor.NONE).strength(0.6F).sound(SoundType.COPPER).noOcclusion()
					.isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

	private static final RegistryObject<BlockItem> TUBE_ITEM = ECRegistrationHolder.simpleBlockItem(TUBE);
	public static final RegistryObject<WindowedTubeItem> WINDOWED_TUBE_ITEM = ECRegistrationHolder.ITEMS.register(
			"windowed_pneumatic_tube", () -> new WindowedTubeItem(TUBE.get(),
					new Item.Properties().tab(EngineeredComputation.TAB)));

	private static final List<RegistryObject<? extends BlockItem>> ITEMS = List.of(TUBE_ITEM, WINDOWED_TUBE_ITEM,
			ECRegistrationHolder.simpleBlockItem(TUBE_STATION),
			ECRegistrationHolder.simpleBlockItem(RECEIVING_STATION));

	public static final RegistryObject<BlockEntityType<TubeStationBlockEntity>> TUBE_STATION_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("tube_station", () -> BlockEntityType.Builder.of(
					TubeStationBlockEntity::new, TUBE_STATION.get()).build(null));
	public static final RegistryObject<BlockEntityType<ReceivingStationBlockEntity>>
			RECEIVING_STATION_ENTITY = ECRegistrationHolder.BLOCK_ENTITIES.register("receiving_station",
			() -> BlockEntityType.Builder.of(ReceivingStationBlockEntity::new, RECEIVING_STATION.get()).build(null));

	public static final RegistryObject<BlockEntityType<CoveredTubeBlockEntity>> COVERED_TUBE_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("covered_pneumatic_tube", () -> BlockEntityType.Builder.of(
					CoveredTubeBlockEntity::new, COVERED_TUBE.get(), CLEAR_COVERED_TUBE.get()).build(null));

	private ECPneumatics()
	{
	}

	public static void init()
	{
	}

	public static List<RegistryObject<? extends BlockItem>> items()
	{
		return ITEMS;
	}

	public static void register(IEventBus modEventBus)
	{
		MinecraftForge.EVENT_BUS.addListener(CoveredTubeBlock::onRightClickBlock);
		TubeNetwork.register();
		TubeCarriers.register();
	}
}
