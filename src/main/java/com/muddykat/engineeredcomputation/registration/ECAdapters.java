package com.muddykat.engineeredcomputation.registration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.AdapterSpeed;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyItem;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.NetworkMember;

public final class ECAdapters
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.requiresCorrectToolForDrops()
			.lightLevel(state -> state.getValue(DataAdapterBlock.ACTIVE)? 3: 0);

	private static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
		@Override
		public int powerCost(Level level, BlockPos pos)
		{
			return level.getBlockState(pos).getBlock() instanceof DataAdapterBlock adapter
					? ECConfig.cost(ECConfig.adapterKey(adapter.getEra(), adapter.getKind())): 0;
		}
	};

	private static final Map<ComputerEra, Map<AdapterKind, DeferredBlock<DataAdapterBlock>>> BLOCKS =
			new EnumMap<>(ComputerEra.class);
	private static final List<DeferredItem<BlockItem>> ITEMS = new ArrayList<>();

	static
	{
		for(ComputerEra era : ComputerEra.values())
		{
			if(AdapterSpeed.of(era).isEmpty())
				continue;
			Map<AdapterKind, DeferredBlock<DataAdapterBlock>> kinds = new EnumMap<>(AdapterKind.class);
			for(AdapterKind kind : AdapterKind.values())
			{
				String name = era.getSerializedName()+"_"+kind.getSerializedName();
				DeferredBlock<DataAdapterBlock> block = ECRegistrationHolder.BLOCKS.register(name,
						() -> new DataAdapterBlock(era, kind, PROPERTIES));
				kinds.put(kind, block);
				ITEMS.add(ECRegistrationHolder.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
			}
			BLOCKS.put(era, kinds);
		}
	}

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataAdapterBlockEntity>> BLOCK_ENTITY =
			ECRegistrationHolder.BLOCK_ENTITIES.register("data_adapter", () -> BlockEntityType.Builder.of(
					DataAdapterBlockEntity::new, BLOCKS.values().stream().flatMap(kinds -> kinds.values().stream())
							.map(DeferredBlock::get).toArray(DataAdapterBlock[]::new)).build(null));

	public static final DeferredItem<ProgramFloppyItem> PROGRAM_FLOPPY = ECRegistrationHolder.ITEMS.register(
			"program_floppy", () -> new ProgramFloppyItem(new Item.Properties().stacksTo(1)));

	private ECAdapters()
	{
	}

	public static void init()
	{
	}

	public static Map<AdapterKind, DeferredBlock<DataAdapterBlock>> of(ComputerEra era)
	{
		return BLOCKS.getOrDefault(era, Map.of());
	}

	public static List<DeferredItem<BlockItem>> items()
	{
		return ITEMS;
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(
				ECCapabilities.NETWORK_MEMBER,
				(level, pos, state, blockEntity, side) -> {
					Direction back = state.getValue(DataAdapterBlock.FACING).getOpposite();
					if(side==null)
						return NETWORK_MEMBER;
					return side==back&&level.getBlockState(pos.relative(back)).getBlock() instanceof NetworkCableBlock
							? NETWORK_MEMBER: null;
				},
				BLOCKS.values().stream().flatMap(kinds -> kinds.values().stream()).map(DeferredBlock::get)
						.toArray(DataAdapterBlock[]::new)));
	}
}
