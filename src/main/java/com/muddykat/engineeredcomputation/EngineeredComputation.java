package com.muddykat.engineeredcomputation;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

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
    /** The mod's creative tab, listing its items era by era (items also name it, for the search tab). */
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {
        @Override
        public ItemStack makeIcon() {
            return ECDataCells.DRUM_1K.get().getDefaultInstance();
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            items.add(new ItemStack(ECContent.ASSEMBLY_WRENCH.get()));
            items.add(new ItemStack(ECCables.item().get()));
            items.add(new ItemStack(ECBridges.item().get()));
            for (ComputerEra era : ComputerEra.values()) {
                items.add(new ItemStack(ECContent.guide(era).get()));
                items.add(new ItemStack(ECContent.casingItem(era).get()));
                items.add(new ItemStack(ECContent.component(era).get()));
                if (era.hasTerminal())
                    items.add(new ItemStack(ECTerminals.items().get(era).get()));
                ECAdapters.of(era).values().forEach(adapter -> items.add(new ItemStack(adapter.get())));
                if (era == ComputerEra.PUNCHCARD) {
                    ECPneumatics.items().forEach(item -> items.add(new ItemStack(item.get())));
                    items.add(new ItemStack(ECCatalogue.CARD.get()));
                    ECCatalogue.decks().forEach(deck -> items.add(new ItemStack(deck.get())));
                }
                ECDataCells.cellsOf(era).forEach(cell -> items.add(new ItemStack(cell.get())));
                if (era == ComputerEra.FLOPPY) {
                    ECDataCells.terminalDisks().forEach(disk -> items.add(new ItemStack(disk.get())));
                    items.add(new ItemStack(ECAdapters.PROGRAM_FLOPPY.get()));
                    items.add(new ItemStack(ECCrafting.PATTERN_FLOPPY.get()));
                    items.add(new ItemStack(ECContent.PROCESSOR_CASING_ITEM.get()));
                }
            }
        }
    };

    public EngineeredComputation() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ECConfig.SPEC);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

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
            EngineeredComputationClient.init(modEventBus);
    }
}
