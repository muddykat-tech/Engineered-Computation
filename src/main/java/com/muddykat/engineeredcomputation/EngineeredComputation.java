package com.muddykat.engineeredcomputation;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.ECPayloads;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECRegistrationHolder;
import com.muddykat.engineeredcomputation.registration.ECSounds;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

@Mod(EngineeredComputation.MODID)
public class EngineeredComputation {
    public static final String MODID = "engineered_computation";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> ENGINEERED_COMPUTATION_TAB = CREATIVE_MODE_TABS.register(
            "engineered_computation_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.engineered_computation"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ECDataCells.DRUM_1K.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ECContent.ASSEMBLY_WRENCH.get());
                output.accept(ECCables.item().get());
                output.accept(ECBridges.item().get());
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

    public EngineeredComputation(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.SERVER, ECConfig.SPEC);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        ECContent.init();
        ECDataCells.init();
        ECMenuTypes.init();
        ECMultiblocks.init();
        ECTerminals.init();
        ECCables.init();
        ECBridges.init();
        ECAdapters.init();
        ECCrafting.init();
        ECPneumatics.init();
        ECCatalogue.init();
        ECRegistrationHolder.register(modEventBus);
        ECMenuTypes.register(modEventBus);
        ECMultiblocks.register(modEventBus);
        ECTerminals.register(modEventBus);
        ECCables.register(modEventBus);
        ECBridges.register(modEventBus);
        ECAdapters.register(modEventBus);
        ECCrafting.register(modEventBus);
        ECPneumatics.register(modEventBus);
        ECSounds.register(modEventBus);
        ECPayloads.register(modEventBus);
        StorageNetwork.register();
        if (FMLEnvironment.dist == Dist.CLIENT)
            EngineeredComputationClient.init(modEventBus, context);
    }
}
