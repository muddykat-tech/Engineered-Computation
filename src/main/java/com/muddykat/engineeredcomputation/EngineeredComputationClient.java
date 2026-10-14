package com.muddykat.engineeredcomputation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.client.gui.FilterCardDecorator;
import com.muddykat.engineeredcomputation.client.guide.GuideText;
import com.muddykat.engineeredcomputation.client.model.CappedModel;
import com.muddykat.engineeredcomputation.client.model.CoveredCableModel;
import com.muddykat.engineeredcomputation.client.model.CoveredTubeModel;
import com.muddykat.engineeredcomputation.client.model.split.SplitModelLoader;
import com.muddykat.engineeredcomputation.client.render.AnimationClock;
import com.muddykat.engineeredcomputation.client.render.CarrierRenderer;
import com.muddykat.engineeredcomputation.client.render.WrenchPortOutline;
import com.muddykat.engineeredcomputation.client.render.drive.DriveRenderer;
import com.muddykat.engineeredcomputation.client.render.drive.TerminalDiskRenderer;
import com.muddykat.engineeredcomputation.client.screen.adapter.DataAdapterScreen;
import com.muddykat.engineeredcomputation.client.screen.catalogue.CardDeckScreen;
import com.muddykat.engineeredcomputation.client.screen.crafting.CraftingComputerScreen;
import com.muddykat.engineeredcomputation.client.screen.crafting.DataProcessorScreen;
import com.muddykat.engineeredcomputation.client.screen.crafting.PatternFloppyScreen;
import com.muddykat.engineeredcomputation.client.screen.drive.EraDriveScreen;
import com.muddykat.engineeredcomputation.client.screen.pneumatic.AutotubeScreen;
import com.muddykat.engineeredcomputation.client.screen.pneumatic.PressurizerScreen;
import com.muddykat.engineeredcomputation.client.screen.pneumatic.ReceivingStationScreen;
import com.muddykat.engineeredcomputation.client.screen.pneumatic.TubeStationScreen;
import com.muddykat.engineeredcomputation.client.screen.power.EnergyAccepterScreen;
import com.muddykat.engineeredcomputation.client.screen.program.ProgramFloppyScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.FloppyTerminalScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.TeletypeScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.TypewriterScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.UniscopeScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.VideoTerminalScreen;
import com.muddykat.engineeredcomputation.compat.ponder.PonderCompat;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.pattern.PatternFloppyItem;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.tube.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;

@Mod(value = EngineeredComputation.MODID, dist = Dist.CLIENT)
public class EngineeredComputationClient {
    private static final int TUBE_GLASS = 0xF2DDB0;
    private static final int TUBE_GLASS_TINT = 7;

    public EngineeredComputationClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerItemProperties);
        modEventBus.addListener(this::registerGeometryLoaders);
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerReloadListeners);
        modEventBus.addListener(this::wrapCoveredCableModels);
        modEventBus.addListener(this::registerBlockColours);
        modEventBus.addListener(this::registerTubeColours);
        modEventBus.addListener(this::registerTubeItemColours);
        modEventBus.addListener(RegisterItemDecorationsEvent.class,
                event -> event.register(ECCatalogue.FILTER_CARD.get(), new FilterCardDecorator()));
        CarrierRenderer.register();
        AnimationClock.register();
        WrenchPortOutline.register();

        if (ModList.get().isLoaded("ponder")) {
            PonderCompat.register(modEventBus);
        }
    }

    private void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ECCrafting.PATTERN_FLOPPY.get(),
                    ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "mode"), (stack, level, entity, seed) -> {
                        Pattern pattern = PatternFloppyItem.pattern(stack);
                        return pattern.isEmpty()? 0: pattern.mode() == Pattern.Mode.DIRECT? 0.5F: 1;
                    });
            ItemProperties.register(ECCatalogue.CARD.get(),
                    ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "punched"),
                    (stack, level, entity, seed) -> CatalogueCardItem.card(stack) != null? 1: 0);
            ItemProperties.register(ECCatalogue.FILTER_CARD.get(),
                    ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "punched"),
                    (stack, level, entity, seed) -> FilterCardItem.filter(stack).isEmpty()? 0: 1);
        });
    }

    private void wrapCoveredCableModels(ModelEvent.ModifyBakingResult event) {
        // Caps go on first so a clear cover shows them.
        for (Block block : List.of(ECCables.block().get(), ECCables.covered().get(), ECCables.clearCovered().get(),
                ECPneumatics.TUBE.get(), ECPneumatics.COVERED_TUBE.get(), ECPneumatics.CLEAR_COVERED_TUBE.get())) {
            String caps = block instanceof PneumaticTubeBlock ? CappedModel.TUBE : CappedModel.CABLE;
            for (BlockState state : block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new CappedModel(model, caps));
        }
        for (CoveredCableBlock block : List.of(ECCables.covered().get(), ECCables.clearCovered().get()))
            for (BlockState state : block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new CoveredCableModel(model, block.isClear()));
        for (CoveredTubeBlock block : List.of(ECPneumatics.COVERED_TUBE.get(), ECPneumatics.CLEAR_COVERED_TUBE.get()))
            for (BlockState state : block.getStateDefinition().getPossibleStates())
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new CoveredTubeModel(model, block.isClear()));
    }

    private void registerTubeColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == TUBE_GLASS_TINT
                        ? tubeGlass(PneumaticTubeBlock.tintColour(state))
                        : coverColour(level, pos, tintIndex),
                ECPneumatics.TUBE.get(), ECPneumatics.COVERED_TUBE.get(), ECPneumatics.CLEAR_COVERED_TUBE.get());
    }

    private void registerTubeItemColours(RegisterColorHandlersEvent.Item event) {
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
        return colour == null ? TUBE_GLASS : colour.getTextureDiffuseColor() & 0xFFFFFF;
    }

    private void registerBlockColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> coverColour(level, pos, tintIndex),
                ECCables.covered().get(), ECCables.clearCovered().get());
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuideText.RELOAD);
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        DriveRenderer.models().forEach(event::register);
        event.register(ModelResourceLocation.standalone(CarrierRenderer.MODEL));
        CoveredTubeModel.models().forEach(event::register);
        CappedModel.models().forEach(event::register);
        TerminalDiskRenderer.models().forEach(event::register);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ECMultiblocks.PART_BLOCK_ENTITY.get(), DriveRenderer::new);
        event.registerBlockEntityRenderer(ECTerminals.TERMINAL_DISKS.get(), TerminalDiskRenderer::new);
    }

    private void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(SplitModelLoader.ID, SplitModelLoader.INSTANCE);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ECMenuTypes.ERA_DRIVE.get(), EraDriveScreen::new);
        event.register(ECMenuTypes.TELETYPE.get(), TeletypeScreen::new);
        event.register(ECMenuTypes.TYPEWRITER.get(), TypewriterScreen::new);
        event.register(ECMenuTypes.UNISCOPE.get(), UniscopeScreen::new);
        event.register(ECMenuTypes.VIDEO_TERMINAL.get(), VideoTerminalScreen<VideoTerminalMenu>::new);
        event.register(ECMenuTypes.FLOPPY_TERMINAL.get(), FloppyTerminalScreen::new);
        event.register(ECMenuTypes.DATA_ADAPTER.get(), DataAdapterScreen::new);
        event.register(ECMenuTypes.ENERGY_ACCEPTER.get(), EnergyAccepterScreen::new);
        event.register(ECMenuTypes.PROGRAM_FLOPPY.get(), ProgramFloppyScreen::new);
        event.register(ECMenuTypes.PATTERN_FLOPPY.get(), PatternFloppyScreen::new);
        event.register(ECMenuTypes.CRAFTING_COMPUTER.get(), CraftingComputerScreen::new);
        event.register(ECMenuTypes.DATA_PROCESSOR.get(), DataProcessorScreen::new);
        event.register(ECMenuTypes.TUBE_STATION.get(), TubeStationScreen::new);
        event.register(ECMenuTypes.AUTOTUBE.get(), AutotubeScreen::new);
        event.register(ECMenuTypes.PRESSURIZER.get(), PressurizerScreen::new);
        event.register(ECMenuTypes.CARD_DECK.get(), CardDeckScreen::new);
        event.register(ECMenuTypes.RECEIVING_STATION.get(), ReceivingStationScreen::new);
    }
}
