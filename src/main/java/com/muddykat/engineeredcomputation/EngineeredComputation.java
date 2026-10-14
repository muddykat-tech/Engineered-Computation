package com.muddykat.engineeredcomputation;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.muddykat.engineeredcomputation.compat.patchouli.PatchouliCompat;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECPayloads;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECPower;
import com.muddykat.engineeredcomputation.registration.ECRegistrationHolder;
import com.muddykat.engineeredcomputation.registration.ECSounds;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.storage.cell.CellStorage;

@Mod(EngineeredComputation.MODID)
public class EngineeredComputation {
    public static final String MODID = "engineered_computation";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENGINEERED_COMPUTATION_TAB = CREATIVE_MODE_TABS.register(
            "engineered_computation_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.engineered_computation"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ECTerminals.items().get(ComputerEra.FLOPPY).get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ECContent.ASSEMBLY_WRENCH.get());
                output.accept(ECCables.item().get());
                output.accept(ECBridges.item().get());
                output.accept(ECPower.ITEM.get());
                for (ComputerEra era : ComputerEra.values()) {
                    output.accept(ECContent.guide(era).get());
                    output.accept(ECContent.casingItem(era).get());
                    output.accept(ECContent.component(era).get());
                    if (era.hasTerminal())
                        output.accept(ECTerminals.items().get(era).get());
                    ECAdapters.of(era).values().forEach(adapter -> output.accept(adapter.get()));
                    if (era == ComputerEra.PUNCHCARD) {
                        ECPneumatics.items().forEach(item -> output.accept(item.get()));
                        output.accept(ECCatalogue.CARD.get());
                        ECCatalogue.decks().forEach(deck -> output.accept(deck.get()));
                        output.accept(ECCatalogue.FILTER_CARD.get());
                        output.accept(ECCatalogue.PROGRAM_DECK.get());
                    }
                    ECDataCells.cellsOf(era).forEach(cell -> output.accept(cell.get()));
                    if (era == ComputerEra.FLOPPY) {
                        ECDataCells.terminalDisks().forEach(disk -> output.accept(disk.get()));
                        output.accept(ECAdapters.PROGRAM_FLOPPY.get());
                        output.accept(ECCrafting.PATTERN_FLOPPY.get());
                        output.accept(ECContent.PROCESSOR_CASING_ITEM.get());
                    }
                }
            }).build());

    public EngineeredComputation(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Engineered Computation Starting");
        modContainer.registerConfig(ModConfig.Type.SERVER, ECConfig.SPEC);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        LOGGER.info("EC Initialization");

        ECContent.init();
        ECDataCells.init();
        ECMenuTypes.init();
        ECMultiblocks.init();
        ECTerminals.init();
        ECCables.init();
        ECBridges.init();
        ECPower.init();
        ECAdapters.init();
        ECCrafting.init();
        ECPneumatics.init();
        ECCatalogue.init();

        LOGGER.info("EC Registration");

        ECRegistrationHolder.register(modEventBus);
        ECDataComponents.register(modEventBus);
        ECMenuTypes.register(modEventBus);
        ECMultiblocks.register(modEventBus);
        ECTerminals.register(modEventBus);
        ECCables.register(modEventBus);
        ECBridges.register(modEventBus);
        ECPower.register(modEventBus);
        ECAdapters.register(modEventBus);
        ECCrafting.register(modEventBus);
        ECPneumatics.register(modEventBus);
        ECPayloads.register(modEventBus);
        ECSounds.register(modEventBus);
        StorageNetwork.register();
        if (ModList.get().isLoaded("patchouli"))
            PatchouliCompat.register(modEventBus);
        CellStorage.register();

        LOGGER.info("EC Loaded");
    }
}
