package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.DataAdapterMenu;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyMenu;
import com.muddykat.engineeredcomputation.catalogue.CardDeckMenu;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerMenu;
import com.muddykat.engineeredcomputation.crafting.DataProcessorMenu;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyMenu;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveMenu;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationMenu;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationMenu;
import com.muddykat.engineeredcomputation.power.EnergyAccepterMenu;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public final class ECMenuTypes
{
	public static final DeferredRegister<MenuType<?>> REGISTER =
			DeferredRegister.create(Registries.MENU, EngineeredComputation.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<EraDriveMenu>> ERA_DRIVE =
			REGISTER.register("era_drive", () -> IMenuTypeExtension.create(EraDriveMenu::fromNetwork));

	public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL =
			REGISTER.register("terminal", () -> IMenuTypeExtension.create(TerminalMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<EnergyAccepterMenu>> ENERGY_ACCEPTER =
			REGISTER.register("energy_accepter", () -> IMenuTypeExtension.create(EnergyAccepterMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<DataAdapterMenu>> DATA_ADAPTER =
			REGISTER.register("data_adapter", () -> IMenuTypeExtension.create(DataAdapterMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<ProgramFloppyMenu>> PROGRAM_FLOPPY =
			REGISTER.register("program_floppy", () -> IMenuTypeExtension.create(ProgramFloppyMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<PatternFloppyMenu>> PATTERN_FLOPPY =
			REGISTER.register("pattern_floppy", () -> IMenuTypeExtension.create(PatternFloppyMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<CraftingComputerMenu>> CRAFTING_COMPUTER =
			REGISTER.register("crafting_computer", () -> IMenuTypeExtension.create(CraftingComputerMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<DataProcessorMenu>> DATA_PROCESSOR =
			REGISTER.register("data_processor", () -> IMenuTypeExtension.create(DataProcessorMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<TubeStationMenu>> TUBE_STATION =
			REGISTER.register("tube_station", () -> IMenuTypeExtension.create(TubeStationMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ReceivingStationMenu>> RECEIVING_STATION =
			REGISTER.register("receiving_station", () -> IMenuTypeExtension.create(ReceivingStationMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<CardDeckMenu>> CARD_DECK =
			REGISTER.register("card_deck", () -> IMenuTypeExtension.create(CardDeckMenu::new));

	private ECMenuTypes()
	{
	}

	public static void init()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		REGISTER.register(modEventBus);
	}
}
