package com.muddykat.engineeredcomputation.registration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.terminal.CraftingTerminalBlock;
import com.muddykat.engineeredcomputation.terminal.CraftingTerminalBlockEntity;
import com.muddykat.engineeredcomputation.terminal.EraTerminalBlock;
import com.muddykat.engineeredcomputation.terminal.FloppyTerminalBlock;
import com.muddykat.engineeredcomputation.terminal.TerminalDiskBlockEntity;
import com.muddykat.engineeredcomputation.terminal.TeletypeBlock;
import com.muddykat.engineeredcomputation.terminal.TeletypeBlockEntity;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;

public final class ECTerminals
{
	private static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.2F, 11.0F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.requiresCorrectToolForDrops();

	private static final Map<ComputerEra, DeferredBlock<EraTerminalBlock>> BLOCKS = new EnumMap<>(ComputerEra.class);
	private static final Map<ComputerEra, DeferredItem<BlockItem>> ITEMS = new EnumMap<>(ComputerEra.class);

	static
	{
		for(ComputerEra era : ComputerEra.values())
		{
			if(!era.hasTerminal())
				continue;
			DeferredBlock<EraTerminalBlock> block = ECRegistrationHolder.BLOCKS.register(era.terminalName(),
					() -> era==ComputerEra.FLOPPY? new FloppyTerminalBlock(PROPERTIES)
							: era==ComputerEra.PUNCHCARD? new TeletypeBlock(PROPERTIES)
							: TerminalFeatures.of(era).crafting()? new CraftingTerminalBlock(era, PROPERTIES)
							: new EraTerminalBlock(era, PROPERTIES));
			BLOCKS.put(era, block);
			ITEMS.put(era, ECRegistrationHolder.ITEMS.register(
					era.terminalName(), () -> new BlockItem(block.get(), new Item.Properties())));
		}
	}

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalDiskBlockEntity>> TERMINAL_DISKS =
			ECRegistrationHolder.BLOCK_ENTITIES.register("terminal_disks", () -> BlockEntityType.Builder.of(
					TerminalDiskBlockEntity::new, BLOCKS.get(ComputerEra.FLOPPY).get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeletypeBlockEntity>> TELETYPE =
			ECRegistrationHolder.BLOCK_ENTITIES.register("teletype", () -> BlockEntityType.Builder.of(
					TeletypeBlockEntity::new, BLOCKS.get(ComputerEra.PUNCHCARD).get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingTerminalBlockEntity>> CRAFTING_TERMINAL =
			ECRegistrationHolder.BLOCK_ENTITIES.register("crafting_terminal", () -> BlockEntityType.Builder.of(
					CraftingTerminalBlockEntity::new, BLOCKS.entrySet().stream()
							.filter(entry -> TerminalFeatures.of(entry.getKey()).crafting()&&entry.getKey()!=ComputerEra.FLOPPY)
							.map(entry -> entry.getValue().get()).toArray(EraTerminalBlock[]::new)).build(null));

	private ECTerminals()
	{
	}

	public static void init()
	{
	}

	public static DeferredBlock<EraTerminalBlock> block(ComputerEra era)
	{
		return BLOCKS.get(era);
	}

	public static Map<ComputerEra, DeferredItem<BlockItem>> items()
	{
		return Collections.unmodifiableMap(ITEMS);
	}

	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlock(
				ECCapabilities.NETWORK_MEMBER,
				(level, pos, state, blockEntity, context) -> blockEntity instanceof TerminalDiskBlockEntity bays
						? bays: EraTerminalBlock.NETWORK_MEMBER,
				BLOCKS.entrySet().stream().filter(entry -> !TerminalFeatures.of(entry.getKey()).requests())
						.map(entry -> entry.getValue().get()).toArray(EraTerminalBlock[]::new)));
	}
}
