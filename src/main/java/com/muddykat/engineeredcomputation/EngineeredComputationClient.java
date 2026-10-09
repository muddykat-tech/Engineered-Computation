package com.muddykat.engineeredcomputation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.muddykat.engineeredcomputation.adapter.DataAdapterScreen;
import com.muddykat.engineeredcomputation.adapter.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.catalogue.CardDeckScreen;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;
import com.muddykat.engineeredcomputation.client.ECConfigScreen;
import com.muddykat.engineeredcomputation.client.guide.GuideText;
import com.muddykat.engineeredcomputation.client.model.CoveredCableModel;
import com.muddykat.engineeredcomputation.client.model.CoveredTubeModel;
import com.muddykat.engineeredcomputation.client.model.SplitModelLoader;
import com.muddykat.engineeredcomputation.client.render.CarrierRenderer;
import com.muddykat.engineeredcomputation.client.render.DriveRenderer;
import com.muddykat.engineeredcomputation.client.render.TerminalDiskRenderer;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerScreen;
import com.muddykat.engineeredcomputation.crafting.DataProcessorScreen;
import com.muddykat.engineeredcomputation.crafting.Pattern;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyItem;
import com.muddykat.engineeredcomputation.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveScreen;
import com.muddykat.engineeredcomputation.network.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationScreen;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationScreen;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.terminal.TerminalScreen;

/** Client-only setup, called from the mod's constructor on the client (Forge 1.20.1 has no client-only @Mod). */
public final class EngineeredComputationClient {
    private static final int TUBE_GLASS = 0xF2DDB0;
    private static final int TUBE_GLASS_TINT = 7;

    private EngineeredComputationClient() {
    }

    public static void init(IEventBus modEventBus, FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ECConfigScreen(parent)));
        modEventBus.addListener(EngineeredComputationClient::registerScreens);
        modEventBus.addListener(EngineeredComputationClient::registerItemProperties);
        modEventBus.addListener(EngineeredComputationClient::registerGeometryLoaders);
        modEventBus.addListener(EngineeredComputationClient::registerAdditionalModels);
        modEventBus.addListener(EngineeredComputationClient::registerRenderers);
        modEventBus.addListener(EngineeredComputationClient::registerReloadListeners);
        modEventBus.addListener(EngineeredComputationClient::wrapCoveredCableModels);
        modEventBus.addListener(EngineeredComputationClient::registerBlockColours);
        modEventBus.addListener(EngineeredComputationClient::registerTubeColours);
        modEventBus.addListener(EngineeredComputationClient::registerTubeItemColours);
        CarrierRenderer.register();
    }

    private static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ECCrafting.PATTERN_FLOPPY.get(),
                    ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "mode"), (stack, level, entity, seed) -> {
                        Pattern pattern = PatternFloppyItem.pattern(stack);
                        return pattern.isEmpty()? 0: pattern.mode() == Pattern.Mode.DIRECT? 0.5F: 1;
                    });
            ItemProperties.register(ECCatalogue.CARD.get(),
                    ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "punched"),
                    (stack, level, entity, seed) -> CatalogueCardItem.card(stack) != null? 1: 0);
        });
    }

    private static void wrapCoveredCableModels(ModelEvent.ModifyBakingResult event) {
        for (CoveredCableBlock block : List.of(ECCables.covered().get(), ECCables.clearCovered().get()))
            for (BlockState state : block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new CoveredCableModel(model, block.isClear()));
        for (CoveredTubeBlock block : List.of(ECPneumatics.COVERED_TUBE.get(), ECPneumatics.CLEAR_COVERED_TUBE.get()))
            for (BlockState state : block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new CoveredTubeModel(model, block.isClear()));
    }

    private static void registerTubeColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == TUBE_GLASS_TINT
                        ? tubeGlass(PneumaticTubeBlock.tintColour(state))
                        : coverColour(level, pos, tintIndex),
                ECPneumatics.TUBE.get(), ECPneumatics.COVERED_TUBE.get(), ECPneumatics.CLEAR_COVERED_TUBE.get());
    }

    private static void registerTubeItemColours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == TUBE_GLASS_TINT ? 0xFF000000 | tubeGlass(null) : -1,
                ECPneumatics.WINDOWED_TUBE_ITEM.get());
    }

    private static int coverColour(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return level != null && pos != null
                && level.getBlockEntity(pos) instanceof CoveredCableBlockEntity covered && !covered.getCover().isAir()
                ? Minecraft.getInstance().getBlockColors().getColor(covered.getCover(), level, pos, tintIndex)
                : -1;
    }

    private static int tubeGlass(@Nullable DyeColor colour) {
        return colour == null ? TUBE_GLASS : diffuseColour(colour);
    }

    /** A dye's texture colour as RGB; DyeColor only keeps it as float components. */
    private static int diffuseColour(DyeColor colour) {
        float[] rgb = colour.getTextureDiffuseColors();
        return Math.round(rgb[0] * 255) << 16 | Math.round(rgb[1] * 255) << 8 | Math.round(rgb[2] * 255);
    }

    private static void registerBlockColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> coverColour(level, pos, tintIndex),
                ECCables.covered().get(), ECCables.clearCovered().get());
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuideText.RELOAD);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        DriveRenderer.models().forEach(event::register);
        event.register(CarrierRenderer.MODEL);
        CoveredTubeModel.models().forEach(event::register);
        TerminalDiskRenderer.models().forEach(event::register);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ECMultiblocks.PART_BLOCK_ENTITY.get(), DriveRenderer::new);
        event.registerBlockEntityRenderer(ECTerminals.TERMINAL_DISKS.get(), TerminalDiskRenderer::new);
    }

    private static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(SplitModelLoader.ID.getPath(), SplitModelLoader.INSTANCE);
    }

    private static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ECMenuTypes.ERA_DRIVE.get(), EraDriveScreen::new);
            MenuScreens.register(ECMenuTypes.TERMINAL.get(), TerminalScreen::new);
            MenuScreens.register(ECMenuTypes.DATA_ADAPTER.get(), DataAdapterScreen::new);
            MenuScreens.register(ECMenuTypes.PROGRAM_FLOPPY.get(), ProgramFloppyScreen::new);
            MenuScreens.register(ECMenuTypes.PATTERN_FLOPPY.get(), PatternFloppyScreen::new);
            MenuScreens.register(ECMenuTypes.CRAFTING_COMPUTER.get(), CraftingComputerScreen::new);
            MenuScreens.register(ECMenuTypes.DATA_PROCESSOR.get(), DataProcessorScreen::new);
            MenuScreens.register(ECMenuTypes.TUBE_STATION.get(), TubeStationScreen::new);
            MenuScreens.register(ECMenuTypes.CARD_DECK.get(), CardDeckScreen::new);
            MenuScreens.register(ECMenuTypes.RECEIVING_STATION.get(), ReceivingStationScreen::new);
        });
    }
}
