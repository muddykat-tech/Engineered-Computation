package com.muddykat.engineeredcomputation.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public final class ECMenuTypes
{
	public static final DeferredRegister<MenuType<?>> REGISTER =
			DeferredRegister.create(Registries.MENU, EngineeredComputation.MODID);

	public static final RegistryObject<MenuType<EraDriveMenu>> ERA_DRIVE =
			REGISTER.register("era_drive", () -> IForgeMenuType.create(EraDriveMenu::fromNetwork));

	public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL =
			REGISTER.register("terminal", () -> IForgeMenuType.create(TerminalMenu::new));

	public static final RegistryObject<MenuType<DataAdapterMenu>> DATA_ADAPTER =
			REGISTER.register("data_adapter", () -> IForgeMenuType.create(DataAdapterMenu::new));

	public static final RegistryObject<MenuType<ProgramFloppyMenu>> PROGRAM_FLOPPY =
			REGISTER.register("program_floppy", () -> IForgeMenuType.create(ProgramFloppyMenu::new));

	public static final RegistryObject<MenuType<PatternFloppyMenu>> PATTERN_FLOPPY =
			REGISTER.register("pattern_floppy", () -> IForgeMenuType.create(PatternFloppyMenu::new));

	public static final RegistryObject<MenuType<CraftingComputerMenu>> CRAFTING_COMPUTER =
			REGISTER.register("crafting_computer", () -> IForgeMenuType.create(CraftingComputerMenu::new));
	public static final RegistryObject<MenuType<DataProcessorMenu>> DATA_PROCESSOR =
			REGISTER.register("data_processor", () -> IForgeMenuType.create(DataProcessorMenu::new));

	public static final RegistryObject<MenuType<TubeStationMenu>> TUBE_STATION =
			REGISTER.register("tube_station", () -> IForgeMenuType.create(TubeStationMenu::new));
	public static final RegistryObject<MenuType<ReceivingStationMenu>> RECEIVING_STATION =
			REGISTER.register("receiving_station", () -> IForgeMenuType.create(ReceivingStationMenu::new));

	public static final RegistryObject<MenuType<CardDeckMenu>> CARD_DECK =
			REGISTER.register("card_deck", () -> IForgeMenuType.create(CardDeckMenu::new));

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
