package com.muddykat.engineeredcomputation.registration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterSpeed;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyItem;
import com.muddykat.engineeredcomputation.era.ComputerEra;

public final class ECAdapters
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(DataAdapterBlock.ACTIVE)? 3: 0);


	private static final Map<ComputerEra, Map<AdapterKind, RegistryObject<DataAdapterBlock>>> BLOCKS =
			new EnumMap<>(ComputerEra.class);
	private static final List<RegistryObject<BlockItem>> ITEMS = new ArrayList<>();

	static
	{
		for(ComputerEra era : ComputerEra.values())
		{
			if(AdapterSpeed.of(era).isEmpty())
				continue;
			Map<AdapterKind, RegistryObject<DataAdapterBlock>> kinds = new EnumMap<>(AdapterKind.class);
			for(AdapterKind kind : AdapterKind.values())
			{
				String name = era.getSerializedName()+"_"+kind.getSerializedName();
				RegistryObject<DataAdapterBlock> block = ECRegistrationHolder.BLOCKS.register(name,
						() -> new DataAdapterBlock(era, kind, PROPERTIES));
				kinds.put(kind, block);
				ITEMS.add(ECRegistrationHolder.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
			}
			BLOCKS.put(era, kinds);
		}
	}

	public static final RegistryObject<BlockEntityType<DataAdapterBlockEntity>> BLOCK_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("data_adapter", () -> BlockEntityType.Builder.of(
					DataAdapterBlockEntity::new, BLOCKS.values().stream().flatMap(kinds -> kinds.values().stream())
							.map(RegistryObject::get).toArray(DataAdapterBlock[]::new)).build(null));

	public static final RegistryObject<ProgramFloppyItem> PROGRAM_FLOPPY = ECRegistrationHolder.ITEMS.register(
			"program_floppy", () -> new ProgramFloppyItem(new Item.Properties().stacksTo(1)));

	private ECAdapters()
	{
	}

	public static void init()
	{
	}

	public static Map<AdapterKind, RegistryObject<DataAdapterBlock>> of(ComputerEra era)
	{
		return BLOCKS.getOrDefault(era, Map.of());
	}

	public static List<RegistryObject<BlockItem>> items()
	{
		return ITEMS;
	}

	public static void register(IEventBus modEventBus)
	{
	}
}
