package com.muddykat.engineeredcomputation.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.AdapterKind;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlock;
import com.muddykat.engineeredcomputation.adapter.DataAdapterBlockEntity;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.deck.CardDeckMenu;
import com.muddykat.engineeredcomputation.catalogue.deck.ProgramDeckItem;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.client.guide.GuideScreen;
import com.muddykat.engineeredcomputation.client.screen.terminal.TerminalScreen;
import com.muddykat.engineeredcomputation.compat.patchouli.PatchouliCompat;
import com.muddykat.engineeredcomputation.connector.ConnectorPorts;
import com.muddykat.engineeredcomputation.crafting.CraftingNetwork;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerMenu;
import com.muddykat.engineeredcomputation.crafting.computer.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.pattern.Pattern;
import com.muddykat.engineeredcomputation.crafting.processor.DataProcessorState;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.EraDriveGeometry;
import com.muddykat.engineeredcomputation.era.EraMachine;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveMenu;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.carrier.TubeCarriers;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerBlock;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.station.TubeStationMenu;
import com.muddykat.engineeredcomputation.pneumatic.tube.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.power.EnergyAccepterBlockEntity;
import com.muddykat.engineeredcomputation.program.Program;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.program.ProgrammableItem;
import com.muddykat.engineeredcomputation.registration.ECAdapters;
import com.muddykat.engineeredcomputation.registration.ECBridges;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECCrafting;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECDataComponents;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.registration.ECPower;
import com.muddykat.engineeredcomputation.registration.ECTerminals;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.storage.cell.CellContents;
import com.muddykat.engineeredcomputation.storage.cell.CellInventory;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;
import com.muddykat.engineeredcomputation.terminal.crafting.VideoTerminalMenu;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeBlockEntity;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeMenu;

@EventBusSubscriber(modid = EngineeredComputation.MODID, value = Dist.CLIENT)
public final class VisualCheck
{
	private static final boolean ENABLED = Boolean.getBoolean("ec.visualcheck");
	private static final List<String> CATEGORIES = List.of("cables", "pneumatics", "drives", "angles", "world", "guide",
			"handbook", "terminal", "adapters", "crafting", "power", "config");
	/** Handbook entries and the page each shot opens at: plain text, long text, multiblocks, recipes and the codes. */
	private static final List<Map.Entry<String, Integer>> HANDBOOK_SHOTS = List.of(Map.entry("basics/wrench", 0),
			Map.entry("basics/network", 0), Map.entry("basics/cells", 0), Map.entry("basics/codes", 0),
			Map.entry("basics/codes", 2), Map.entry("era1/era1", 0), Map.entry("era1/era1", 2),
			Map.entry("era1/pressurizer", 0), Map.entry("era2/era2", 0), Map.entry("era2/era2_drive", 0),
			Map.entry("era4/era4_drive", 0), Map.entry("era5/era5", 0), Map.entry("era5/crafting_computer", 0),
			Map.entry("era5/memory", 0));
	/** Set with -Pvisuals; ec.visualcheck.angles=true is the older way to run only the angles. */
	private static final Set<String> ONLY = categoriesToRun();
	private static final List<String> ANGLE_DRIVES = List.of(System.getProperty("ec.visualcheck.drives", "").split(","))
			.stream().filter(name -> !name.isBlank()).toList();
	private static final String WORLD = "ec_visualcheck";
	private static final int BOOK_VIEWS = 24;
	private static final int PALM_VIEWS = 64;
	private static final int FLOAT_HEIGHT = 3;

	private static final Deque<Step> STEPS = new ArrayDeque<>();
	private static boolean worldRequested;
	private static boolean queued;
	private static int wait;
	private static ItemStack removedDiskPack = ItemStack.EMPTY;
	private static BlockPos base;

	private VisualCheck()
	{
	}

	private record Step(int waitAfter, Runnable action)
	{
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event)
	{
		if(!ENABLED)
			return;
		Minecraft mc = Minecraft.getInstance();
		if(!worldRequested)
		{
			if(mc.screen instanceof TitleScreen)
			{
				worldRequested = true;
				mc.options.pauseOnLostFocus = false;
				// With vsync on, a hidden window can drop to about 1 FPS and take stale screenshots.
				mc.getWindow().updateVsync(false);
				createWorld(mc);
			}
			return;
		}
		if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)
			return;
		if(!queued)
		{
			queued = true;
			wait = 60;
			queueSteps(mc);
		}
		// Flight switches off when a teleported player touches the ground, so it's turned back on every tick.
		if(mc.player.getAbilities().mayfly)
			mc.player.getAbilities().flying = true;
		if(--wait>0||STEPS.isEmpty())
			return;
		Step step = STEPS.poll();
		try
		{
			step.action().run();
		}
		catch(RuntimeException e)
		{
			EngineeredComputation.LOGGER.error("Visual check step failed", e);
		}
		wait = step.waitAfter();
	}

	private static void createWorld(Minecraft mc)
	{
		if(mc.getLevelSource().levelExists(WORLD))
			try(LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(WORLD))
			{
				access.deleteLevel();
			}
			catch(IOException e)
			{
				throw new UncheckedIOException("Couldn't delete the old visual check world", e);
			}
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
				WorldDataConfiguration.DEFAULT);
		mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
				registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
						.getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
				new TitleScreen());
	}

	private static void queueSteps(Minecraft mc)
	{
		List<String> unknown = ONLY.stream().filter(name -> !CATEGORIES.contains(name)).toList();
		if(!unknown.isEmpty())
		{
			EngineeredComputation.LOGGER.error("Visual check: unknown categories {} - the categories are {}", unknown,
					CATEGORIES);
			STEPS.add(new Step(5, mc::stop));
			return;
		}
		EngineeredComputation.LOGGER.info("Visual check: categories {}, running {}", CATEGORIES,
				ONLY.isEmpty()? "all": ONLY);
		onServer(mc, VisualCheck::buildScene, 120);
		// Each section sets the HUD itself, so any of them can be skipped.
		section(mc, "cables", false, VisualCheck::queueCoveredCables);
		section(mc, "pneumatics", false, VisualCheck::queuePneumatics);
		section(mc, "drives", false, VisualCheck::queueCloseups);
		section(mc, "drives", false, VisualCheck::queueFloppyBays);
		section(mc, "drives", false, VisualCheck::queueDiskPack);
		section(mc, "drives", false, VisualCheck::queueTapeReels);
		section(mc, "drives", false, VisualCheck::queueBanks);
		section(mc, "drives", false, VisualCheck::queueCardVault);
		section(mc, "drives", false, VisualCheck::queueStandaloneDrives);
		section(mc, "drives", false, VisualCheck::queueDrumEra);
		section(mc, "angles", false, VisualCheck::queueModelAngles);
		section(mc, "world", false, VisualCheck::queueWorld);
		section(mc, "guide", true, VisualCheck::queueGuides);
		section(mc, "handbook", true, VisualCheck::queueHandbook);
		section(mc, "terminal", true, VisualCheck::queueTerminals);
		section(mc, "drives", true, VisualCheck::queueDriveScreens);
		section(mc, "terminal", true, VisualCheck::queueConsoleClicks);
		section(mc, "cables", true, VisualCheck::queueCables);
		section(mc, "adapters", true, VisualCheck::queueAdapters);
		section(mc, "crafting", true, VisualCheck::queueCrafting);
		section(mc, "power", true, VisualCheck::queuePower);
		section(mc, "config", true, VisualCheck::queueConfig);
		STEPS.add(new Step(10, mc::stop));
	}

	private static Set<String> categoriesToRun()
	{
		if(Boolean.getBoolean("ec.visualcheck.angles"))
			return Set.of("angles");
		Set<String> names = Set.copyOf(List.of(System.getProperty("ec.visualcheck.only", "").split(",")).stream()
				.map(String::trim).filter(name -> !name.isEmpty()).toList());
		return names.contains("all")? Set.of(): names;
	}

	private static void section(Minecraft mc, String category, boolean hud, Consumer<Minecraft> queue)
	{
		if(!CATEGORIES.contains(category))
			throw new IllegalArgumentException("not a visual check category: "+category);
		if(!ONLY.isEmpty()&&!ONLY.contains(category))
			return;
		STEPS.add(new Step(5, () -> mc.options.hideGui = !hud));
		queue.accept(mc);
	}

	private static void queueWorld(Minecraft mc)
	{
		onServer(mc, (level, player) -> overview(player), 20);
		screenshot(mc, "ec_01_world_day", 5);
		onServer(mc, (level, player) -> {
			level.setDayTime(18000);
			overview(player);
		}, 80);
		screenshot(mc, "ec_02_world_night", 5);
		onServer(mc, (level, player) -> {
			placeLights(level);
			overview(player);
		}, 80);
		screenshot(mc, "ec_03_world_night_torches", 5);
		onServer(mc, (level, player) -> level.setDayTime(6000), 20);
	}

	/** Exports the Patchouli handbook from the guides, reloads it, then pictures it. */
	private static void queueHandbook(Minecraft mc)
	{
		STEPS.add(new Step(5, () -> {
			try
			{
				HandbookExport.export(mc);
			}
			catch(IOException e)
			{
				throw new UncheckedIOException(e);
			}
			mc.reloadResourcePacks();
		}));
		STEPS.add(new Step(200, () -> {
		}));
		STEPS.add(new Step(20, PatchouliCompat::openLanding));
		screenshot(mc, "ec_handbook_landing", 2);
		for(Map.Entry<String, Integer> shot : HANDBOOK_SHOTS)
		{
			STEPS.add(new Step(20, () -> PatchouliCompat.openEntry(
					ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, shot.getKey()), shot.getValue())));
			screenshot(mc, "ec_handbook_"+shot.getKey().replace('/', '_')+"_"+shot.getValue(), 2);
		}
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueGuides(Minecraft mc)
	{
		for(ComputerEra era : ComputerEra.values())
		{
			STEPS.add(new Step(10, () -> {
				mc.setScreen(GuideScreen.open(era));
				hoverAbsolute(mc, 0, 0);
			}));
			int views = era==ComputerEra.FLOPPY? PALM_VIEWS: BOOK_VIEWS;
			for(int view = 0; view<views; view++)
			{
				screenshot(mc, String.format("ec_guide_%s_%02d", era.getSerializedName(), view), 1);
				STEPS.add(new Step(3, () -> mc.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0)));
			}
		}
		// The smallest machines' structure pages (40 and 44), where the wrench is closest to the view's edges.
		STEPS.add(new Step(10, () -> {
			mc.setScreen(GuideScreen.open(ComputerEra.FLOPPY));
			for(int i = 0; i<39; i++)
				mc.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
			hoverAbsolute(mc, 0, 0);
		}));
		for(int page = 40; page<=44; page++)
		{
			screenshot(mc, "ec_guide_era5_page_"+page, 1);
			STEPS.add(new Step(3, () -> mc.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0)));
		}
		STEPS.add(new Step(10, () -> {
			mc.setScreen(GuideScreen.open(ComputerEra.DRUM));
			for(int i = 0; i<13; i++)
				mc.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
		}));
		for(int row = 0; row<2; row++)
		{
			int slotRow = row;
			STEPS.add(new Step(5, () -> hoverAbsolute(mc, guideLeft(mc)+16+9, 32+24+slotRow*18+9)));
			screenshot(mc, "ec_guide_link_hover_"+row, 2);
		}
		STEPS.add(new Step(5, () -> {
			mc.screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
			mc.screen.mouseClicked(guideLeft(mc)+16+20, 32+18+3*11+3, GLFW.GLFW_MOUSE_BUTTON_LEFT);
			hoverAbsolute(mc, 0, 0);
		}));
		screenshot(mc, "ec_guide_link_contents_click", 2);
		STEPS.add(new Step(5, () -> mc.screen.mouseClicked(guideLeft(mc)+16+5, 2+16+3, GLFW.GLFW_MOUSE_BUTTON_LEFT)));
		screenshot(mc, "ec_guide_link_back", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
	}

	private static void queueTerminals(Minecraft mc)
	{
		TerminalMenu.Layout crafting = TerminalMenu.Layout.of(TerminalFeatures.CRAFTING);
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DISK)), 30);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 8+8, 34+8)));
		screenshot(mc, "ec_terminal_item_tooltip", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 90, crafting.gridY()+crafting.rows()*18+5)));
		screenshot(mc, "ec_terminal_capacity_tooltip", 2);
		onServer(mc, (level, player) -> {
			if(player.containerMenu instanceof TerminalMenu menu)
			{
				for(int slot : new int[]{0, 1, 3, 4})
					menu.getSlot(VideoTerminalMenu.CRAFT_GRID_FIRST+slot).set(new ItemStack(Items.STONE));
				menu.broadcastChanges();
			}
		}, 5);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal_crafting", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, VideoTerminalMenu.CRAFT_X+9, crafting.craftingY()+9)));
		screenshot(mc, "ec_terminal_linked_slot_tooltip", 2);
		onServer(mc, (level, player) -> {
			if(player.containerMenu instanceof TerminalMenu menu)
			{
				menu.getSlot(VideoTerminalMenu.CRAFT_GRID_FIRST+8).set(new ItemStack(Items.DIAMOND));
				menu.broadcastChanges();
			}
		}, 5);
		STEPS.add(new Step(5, () -> hoverGui(mc, VideoTerminalMenu.CRAFT_X+2*18+9, crafting.craftingY()+2*18+9)));
		screenshot(mc, "ec_terminal_linked_missing", 2);
		onServer(mc, (level, player) -> {
			if(player.containerMenu instanceof TerminalMenu menu)
			{
				menu.clickMenuButton(player, VideoTerminalMenu.CLEAR_GRID);
				menu.setCarried(new ItemStack(Items.STONE, 64));
				menu.broadcastChanges();
			}
		}, 5);
		STEPS.add(new Step(5, () -> {
			if(mc.screen instanceof AbstractContainerScreen<?> screen)
			{
				int x = screen.getGuiLeft()+VideoTerminalMenu.CRAFT_X+9, y = screen.getGuiTop()+crafting.craftingY()+9;
				screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
				for(int column = 1; column<3; column++)
					screen.mouseDragged(x+column*18, y, GLFW.GLFW_MOUSE_BUTTON_RIGHT, 18, 0);
				screen.mouseReleased(x+36, y, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			}
		}));
		STEPS.add(new Step(5, () -> hoverGui(mc, VideoTerminalMenu.CRAFT_X+3*18+30, crafting.craftingY()+40)));
		screenshot(mc, "ec_terminal_linked_drag", 2);
		onServer(mc, (level, player) -> {
			if(player.containerMenu instanceof TerminalMenu menu)
			{
				menu.setCarried(ItemStack.EMPTY);
				menu.clickMenuButton(player, VideoTerminalMenu.CLEAR_GRID);
				for(int slot : new int[]{0, 1, 3, 4})
					menu.getSlot(VideoTerminalMenu.CRAFT_GRID_FIRST+slot).set(new ItemStack(Items.STONE));
				menu.broadcastChanges();
			}
		}, 5);
		onServer(mc, (level, player) -> {
			if(player.containerMenu instanceof TerminalMenu menu)
			{
				menu.getSlot(VideoTerminalMenu.CRAFT_GRID_FIRST+8).set(ItemStack.EMPTY);
				menu.broadcastChanges();
			}
		}, 5);
		STEPS.add(new Step(5, () -> hoverGui(mc, VideoTerminalMenu.LINK_X+8, crafting.craftingY()+27)));
		screenshot(mc, "ec_terminal_link_tooltip", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, VideoTerminalMenu.LINK_X+8, crafting.craftingY()+27)));
		screenshot(mc, "ec_terminal_link_off", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, VideoTerminalMenu.CLEAR_X+5, crafting.craftingY()+5)));
		screenshot(mc, "ec_terminal_clear_tooltip", 2);
		STEPS.add(new Step(5, () -> {
			hoverGui(mc, 60, 22);
			clickGui(mc, 60, 22);
			for(char c : "sword".toCharArray())
				mc.screen.charTyped(c, 0);
		}));
		screenshot(mc, "ec_terminal_search", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));

		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DRUM)), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal_typewriter", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, terminalRowPos(ComputerEra.TAPE)), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal_uniscope", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.FLOPPY)), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal_floppy", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			if(level.getBlockEntity(terminalPos(ComputerEra.PUNCHCARD)) instanceof TeletypeBlockEntity teletype)
			{
				teletype.getSlip().add(ItemKey.of(new ItemStack(Items.IRON_INGOT)), 9, 9);
				teletype.getSlip().add(ItemKey.of(new ItemStack(Items.REDSTONE)), 24, 40);
			}
			use(level, player, terminalPos(ComputerEra.PUNCHCARD));
		}, 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, 182, 9)));
		screenshot(mc, "ec_terminal_teletype", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 16, 26)));
		screenshot(mc, "ec_terminal_teletype_tooltip", 2);
		STEPS.add(new Step(5, () -> {
			if(mc.screen instanceof AbstractContainerScreen<?> screen)
				screen.mouseClicked(screen.getGuiLeft()+screen.getXSize()-12, screen.getGuiTop()+144, 0);
		}));
		screenshot(mc, "ec_teletype_outlets", 2);
		STEPS.add(new Step(5, () -> "pack".chars().forEach(c -> mc.screen.charTyped((char)c, 0))));
		screenshot(mc, "ec_teletype_outlets_search", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		queueTeletypeCard(mc);
	}

	private static void queueTeletypeCard(Minecraft mc)
	{
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.PUNCHCARD)), 20);
		STEPS.add(new Step(5, () -> hoverCardSlot(mc)));
		screenshot(mc, "ec_teletype_card_slot", 2);
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			if(level.getBlockEntity(terminalPos(ComputerEra.PUNCHCARD)) instanceof TeletypeBlockEntity teletype)
				teletype.cardContainer().setItem(0, FilterCardItem.of(ProgramEntry.ofName("iron")));
			use(level, player, terminalPos(ComputerEra.PUNCHCARD));
		}, 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_filtered", 2);
		STEPS.add(new Step(5, () -> hoverCardSlot(mc)));
		screenshot(mc, "ec_teletype_card_tooltip", 2);
		STEPS.add(new Step(5, () -> {
			if(mc.screen instanceof AbstractContainerScreen<?> screen)
			{
				Slot card = screen.getMenu().getSlot(TeletypeMenu.CARD_SLOT);
				clickGui(mc, card.x+8, card.y+8);
			}
		}));
		STEPS.add(new Step(10, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_popup", 2);
		STEPS.add(new Step(5, () -> {
			var area = ((TerminalScreen<?>)mc.screen).extraAreas().stream().findFirst().orElse(null);
			if(area!=null)
				mc.screen.mouseClicked(area.getX()+100, area.getY()+26, GLFW.GLFW_MOUSE_BUTTON_LEFT);
			for(int i = 0; i<4; i++)
				mc.screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
			"red".chars().forEach(c -> mc.screen.charTyped((char)c, 0));
		}));
		STEPS.add(new Step(15, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_popup_typed", 2);
		STEPS.add(new Step(5, () -> {
			for(int i = 0; i<3; i++)
				mc.screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
			mc.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
		}));
		STEPS.add(new Step(15, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_popup_empty", 2);
		STEPS.add(new Step(5, () -> dragCardPopup(mc, -60, -90)));
		STEPS.add(new Step(10, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_popup_moved", 2);
		STEPS.add(new Step(5, () -> mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0)));
		STEPS.add(new Step(10, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_teletype_card_popup_closed", 2);
		// Redstone on the slip should be faded, with the amount the slip will leave in red.
		TerminalMenu.Layout requests = TerminalMenu.Layout.of(TerminalFeatures.REQUESTS);
		STEPS.add(new Step(5, () -> hoverGui(mc, TerminalMenu.GRID_X+3*18+9, requests.gridY()+9)));
		screenshot(mc, "ec_teletype_slipped_tooltip", 2);
		// The insert only station's items should show separately, greyed out with a padlock.
		STEPS.add(new Step(5, () -> hoverGui(mc, TerminalMenu.GRID_X+1*18+9, requests.gridY()+9)));
		screenshot(mc, "ec_teletype_locked_tooltip", 2);
		int lockButtonX = 195-7-11+5;
		STEPS.add(new Step(5, () -> clickGui(mc, lockButtonX, 9)));
		STEPS.add(new Step(10, () -> hoverGui(mc, lockButtonX, 9)));
		screenshot(mc, "ec_teletype_locked_hidden", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, lockButtonX, 9)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		STEPS.add(new Step(10, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			if(level.getBlockEntity(terminalPos(ComputerEra.PUNCHCARD)) instanceof TeletypeBlockEntity teletype)
				teletype.cardContainer().setItem(0, ItemStack.EMPTY);
		}, 5);
	}

	private static void dragCardPopup(Minecraft mc, int byX, int byY)
	{
		if(!(mc.screen instanceof AbstractContainerScreen<?> screen))
			return;
		var area = ((TerminalScreen<?>)screen).extraAreas().stream().findFirst().orElse(null);
		if(area==null)
			return;
		double fromX = area.getX()+20, fromY = area.getY()+5;
		screen.mouseClicked(fromX, fromY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
		screen.mouseDragged(fromX+byX, fromY+byY, GLFW.GLFW_MOUSE_BUTTON_LEFT, byX, byY);
		screen.mouseReleased(fromX+byX, fromY+byY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}

	private static void hoverCardSlot(Minecraft mc)
	{
		if(mc.screen instanceof AbstractContainerScreen<?> screen)
		{
			Slot card = screen.getMenu().getSlot(TeletypeMenu.CARD_SLOT);
			hoverGui(mc, card.x+8, card.y+8);
		}
	}

	private static void queueDriveScreens(Minecraft mc)
	{
		onServer(mc, (level, player) -> use(level, player, driveOrigin(ComputerEra.DRUM)
				.offset(EraDrive.DRUM_COMPUTER.geometry().trigger())), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_drive_drum_computer", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, driveOrigin(ComputerEra.PUNCHCARD)
				.offset(EraDrive.PUNCHCARD_VAULT.geometry().trigger())), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_drive", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 44+2*18+8, 20+8)));
		screenshot(mc, "ec_drive_empty_slot_tooltip", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 44+8, 20+8)));
		screenshot(mc, "ec_drive_cell_tooltip", 2);
		STEPS.add(new Step(5, () -> {
			for(int i = 0; i<3; i++)
				clickGui(mc, 44+8, 20+19+5);
		}));
		STEPS.add(new Step(10, () -> hoverGui(mc, 44+8, 20+19+5)));
		screenshot(mc, "ec_drive_priority", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, driveOrigin(ComputerEra.DISK)
				.offset(EraDrive.DISK_MAINFRAME.geometry().trigger())), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_drive_disk", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueCables(Minecraft mc)
	{
		camera(mc, "ec_cables", ComputerEra.DRUM, 4.0, 1.0, -7.5, 31, 20);
		camera(mc, "ec_cables_cross_era", ComputerEra.TAPE, 2.6, 1.0, -5.2, 30, 25);
		camera(mc, "ec_floppy_port", ComputerEra.FLOPPY, 1.6, -0.6, -2.6, 23, 12);
		camera(mc, "ec_floppy_port_close", ComputerEra.FLOPPY, 1.3, -0.72, -1.6, 27, 13);
		camera(mc, "ec_floppy_port_refused", ComputerEra.FLOPPY, -2.2, -0.42, -1.2, -50, 16);
		camera(mc, "ec_disk_bank_port", EraDrive.DISK_BANK, 1.3, -0.72, -1.6, 27, 13);
		camera(mc, "ec_floppy_bank_port", EraDrive.FLOPPY_BANK, 1.3, -0.72, -1.6, 27, 13);
		camera(mc, "ec_tape_port", ComputerEra.TAPE, 1.6, -0.75, -2.2, 27, 9);
		onServer(mc, (level, player) -> {
			BlockPos port = rotatedFloppyOrigin();
			player.teleportTo(level, port.getX()+2.4, port.getY()-1.05, port.getZ()+0.5, 90, 2);
		}, 15);
		screenshot(mc, "ec_floppy_port_bare", 2);
		STEPS.add(new Step(5, () -> mc.options.hideGui = false));
		onServer(mc, (level, player) -> use(level, player, cableTerminalPos()), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_cable_terminal", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, crossEraTerminalPos()), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_cross_era_terminal", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueCoveredCables(Minecraft mc)
	{
		BlockState[] covers = {null, Blocks.STONE.defaultBlockState(), null, Blocks.GLASS.defaultBlockState(),
				Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), null,
				Blocks.OAK_LOG.defaultBlockState(), null};
		onServer(mc, (level, player) -> {
			BlockPos start = coveredCablesPos();
			for(int x = 0; x<covers.length; x++)
				placeCable(level, start.east(x));
			for(int x = 0; x<covers.length; x++)
				if(covers[x]!=null)
					CoveredCableBlock.cover(level, start.east(x), covers[x]);
			player.teleportTo(level, start.getX()+4.5, start.getY()-0.6, start.getZ()+3.2, 180, 12);
		}, 40);
		screenshot(mc, "ec_covered_cables", 2);
		onServer(mc, (level, player) -> {
			BlockPos start = coveredCablesPos();
			player.teleportTo(level, start.getX()+3.5, start.getY()+0.4, start.getZ()+1.6, 180, 35);
		}, 15);
		screenshot(mc, "ec_covered_cables_close", 2);
		onServer(mc, (level, player) -> CoveredCableBlock.uncover(level, coveredCablesPos().east(1)), 15);
		onServer(mc, (level, player) -> {
			BlockPos start = coveredCablesPos();
			player.teleportTo(level, start.getX()+4.5, start.getY()-0.6, start.getZ()+3.2, 180, 12);
		}, 15);
		screenshot(mc, "ec_covered_cables_uncovered", 2);
	}

	private static void queuePneumatics(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos();
			level.setBlockAndUpdate(start, Blocks.CHEST.defaultBlockState());
			for(BlockPos tube : List.of(start.east(2), start.east(3), start.east(4), start.east(4).south(),
					start.east(4).above(), start.east(5).above()))
				level.setBlockAndUpdate(tube, Block.updateFromNeighbourShapes(ECPneumatics.TUBE.get().defaultBlockState(),
						level, tube));
			level.setBlockAndUpdate(start.east(), ECPneumatics.TUBE_STATION.get().defaultBlockState()
					.setValue(TubeStationBlock.FACING, Direction.WEST));
			level.setBlockAndUpdate(start.east(6).above(), ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
			for(BlockPos windowed : List.of(start.east(3), start.east(5).above()))
				level.setBlockAndUpdate(windowed, level.getBlockState(windowed).setValue(PneumaticTubeBlock.WINDOWED, true));
			player.teleportTo(level, start.getX()+3.5, start.getY()+1.2, start.getZ()+4.5, 180, 20);
		}, 30);
		screenshot(mc, "ec_pneumatic_tubes", 2);
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos();
			player.teleportTo(level, start.getX()+4.5, start.getY()+0.6, start.getZ()+2.2, 180, 25);
		}, 15);
		screenshot(mc, "ec_pneumatic_tubes_close", 2);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			int[] tints = {0, 0, 0, PneumaticTubeBlock.tintOf(DyeColor.LIGHT_BLUE), PneumaticTubeBlock.tintOf(DyeColor.LIGHT_BLUE),
					-1, -1, PneumaticTubeBlock.tintOf(DyeColor.RED)};
			for(int x = 0; x<tints.length; x++)
				level.setBlockAndUpdate(run.east(x), Block.updateFromNeighbourShapes(ECPneumatics.TUBE.get()
						.defaultBlockState(), level, run.east(x)));
			for(int x = 0; x<tints.length; x++)
				if(tints[x]>=0)
					level.setBlockAndUpdate(run.east(x), level.getBlockState(run.east(x))
							.setValue(PneumaticTubeBlock.WINDOWED, true).setValue(PneumaticTubeBlock.TINT, tints[x]));
			player.teleportTo(level, run.getX()+3.5, run.getY()+1.4, run.getZ()+3.4, 160, 22);
			mc.options.hideGui = false;
			player.getInventory().setItem(0, new ItemStack(ECPneumatics.TUBE.get()));
			player.getInventory().setItem(1, new ItemStack(ECPneumatics.WINDOWED_TUBE_ITEM.get()));
			player.getInventory().setItem(2, new ItemStack(ECPneumatics.TUBE_STATION.get()));
		}, 20);
		screenshot(mc, "ec_pneumatic_windows", 2);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			CoveredTubeBlock.cover(level, run.east(1), Blocks.GLASS.defaultBlockState());
			CoveredTubeBlock.cover(level, run.east(4), Blocks.STONE_BRICKS.defaultBlockState());
			CoveredTubeBlock.cover(level, run.east(6), Blocks.OAK_LEAVES.defaultBlockState()
					.setValue(LeavesBlock.PERSISTENT, true));
			player.teleportTo(level, run.getX()+3.5, run.getY()+1.4, run.getZ()+3.4, 160, 22);
		}, 10);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			List<BlockPos> path = new ArrayList<>();
			for(int x = 0; x<8; x++)
				path.add(run.east(x));
			TubeCarriers.of(level).launch(level, new ItemStack(Items.COBBLESTONE), path);
		}, 2);
		screenshot(mc, "ec_pneumatic_covers", 1);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			player.teleportTo(level, run.getX()+2.9, run.getY()+1.1-player.getEyeHeight(), run.getZ()+1.1, 245, 30);
		}, 15);
		screenshot(mc, "ec_pneumatic_cover_cap", 2);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			player.teleportTo(level, run.getX()+2.5, run.getY()+0.9-player.getEyeHeight(), run.getZ()+1.3, 120, 25);
		}, 15);
		screenshot(mc, "ec_pneumatic_windows_close", 2);
		onServer(mc, (level, player) -> {
			BlockPos run = pneumaticsPos().south(4);
			player.teleportTo(level, run.getX()-1.5, run.getY()+0.5-player.getEyeHeight(), run.getZ()+0.5, 270, 0);
		}, 15);
		screenshot(mc, "ec_pneumatic_windows_along", 2);
		onServer(mc, (level, player) -> {
			mc.options.hideGui = true;
			for(int slot = 0; slot<3; slot++)
				player.getInventory().setItem(slot, ItemStack.EMPTY);
		}, 5);
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos();
			if(level.getBlockEntity(start) instanceof ChestBlockEntity chest)
				chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
			BlockPos station = start.east();
			List<BlockPos> route = TubeNetwork.of(level, station).route(station, start.east(6).above());
			TubeCarriers.of(level).launch(level, new ItemStack(Items.COBBLESTONE, 16), route);
		}, 5);
		screenshot(mc, "ec_pneumatic_carrier", 2);
		screenshot(mc, "ec_pneumatic_carrier_later", 2);
		onServer(mc, (level, player) -> use(level, player, pneumaticsPos().east()), 15);
		screenshot(mc, "ec_tube_station_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, pneumaticsPos().east(6).above()), 15);
		screenshot(mc, "ec_receiving_station_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			ItemStack deck = new ItemStack(ECCatalogue.decks().get(1).get());
			CatalogueCard card = new CatalogueCard(GlobalPos.of(level.dimension(), pneumaticsPos().east()),
					Component.literal("Quarry Chest"), CellContents.EMPTY, level.getGameTime());
			CardDeckItem.setDeck(deck, CardDeck.EMPTY.with(card).with(card));
			player.setItemInHand(InteractionHand.MAIN_HAND, deck);
			CardDeckMenu.open(player, InteractionHand.MAIN_HAND);
		}, 15);
		screenshot(mc, "ec_card_deck_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY), 5);
		queueAdvancedStation(mc);
		queuePressurizer(mc);
		queueAutotube(mc);
	}

	private static void queuePressurizer(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			BlockPos at = pneumaticsPos().east(3).south();
			level.setBlockAndUpdate(at, ECPneumatics.PRESSURIZER.get().defaultBlockState()
					.setValue(PressurizerBlock.FACING, Direction.SOUTH));
			BlockPos tube = at.north();
			level.setBlockAndUpdate(tube, Block.updateFromNeighbourShapes(level.getBlockState(tube), level, tube));
			player.teleportTo(level, at.getX()+1.8, at.getY()+0.6, at.getZ()+2.6, 145, 20);
		}, 20);
		screenshot(mc, "ec_pressurizer_cold", 2);
		onServer(mc, (level, player) -> {
			if(level.getBlockEntity(pneumaticsPos().east(3).south()) instanceof PressurizerBlockEntity pressurizer)
			{
				pressurizer.fuelContainer().setItem(0, new ItemStack(Items.COAL, 16));
				pressurizer.draw();
			}
		}, 15);
		screenshot(mc, "ec_pressurizer", 2);
		onServer(mc, (level, player) -> use(level, player, pneumaticsPos().east(3).south()), 15);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_pressurizer_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueAutotube(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos().south(8);
			level.setBlockAndUpdate(start, Blocks.CHEST.defaultBlockState());
			BlockPos junction = start.east(3);
			for(BlockPos tube : List.of(start.east(2), junction, junction.north(), junction.south()))
				level.setBlockAndUpdate(tube, ECPneumatics.TUBE.get().defaultBlockState());
			level.setBlockAndUpdate(junction.north(2), ECPneumatics.TUBE_STATION.get().defaultBlockState()
					.setValue(TubeStationBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(junction.north(3), Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(junction.south(2), ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
			level.setBlockAndUpdate(start.east(), ECPneumatics.AUTOTUBE.get().defaultBlockState()
					.setValue(AutotubeBlock.FACING, Direction.WEST));
			for(BlockPos pos : List.of(start.east(), start.east(2), junction, junction.north(), junction.south()))
				level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
			level.setBlockAndUpdate(start.east(2), level.getBlockState(start.east(2))
					.setValue(PneumaticTubeBlock.WINDOWED, true));
			if(level.getBlockEntity(junction.north(2)) instanceof TubeStationBlockEntity station)
				station.setTakesNewItems(true);
			level.setBlockAndUpdate(start.east().above(), Blocks.BARREL.defaultBlockState());
			if(level.getBlockEntity(start) instanceof ChestBlockEntity chest)
				chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
			player.teleportTo(level, start.getX()+2.0, start.getY()+2.4, start.getZ()+6.0, 180, 32);
		}, 30);
		screenshot(mc, "ec_autotube", 2);
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos().south(8);
			player.teleportTo(level, start.getX()+1.4, start.getY()+0.9-player.getEyeHeight(), start.getZ()+1.9, 175, 5);
		}, 15);
		screenshot(mc, "ec_autotube_close", 2);
		onServer(mc, (level, player) -> {
			BlockPos chest = pneumaticsPos().south(10);
			level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(chest.east(2), ECPneumatics.TUBE.get().defaultBlockState());
			level.setBlockAndUpdate(chest.east(), ECPneumatics.OBSERVING_AUTOTUBE.get().defaultBlockState()
					.setValue(AutotubeBlock.FACING, Direction.WEST));
			for(BlockPos pos : List.of(chest.east(), chest.east(2)))
				level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
			player.teleportTo(level, chest.getX()+1.5, chest.getY()+0.9-player.getEyeHeight(), chest.getZ()+2.0, 175, 5);
		}, 15);
		screenshot(mc, "ec_observing_autotube", 2);
		onServer(mc, (level, player) -> {
			BlockPos autotube = pneumaticsPos().south(8).east();
			mc.options.hideGui = false;
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECContent.ASSEMBLY_WRENCH.get()));
			player.teleportTo(level, autotube.getX()+14.5/16, autotube.getY()+0.85-player.getEyeHeight(),
					autotube.getZ()+1.8, 180, 15);
		}, 15);
		screenshot(mc, "ec_autotube_wrench_arm", 2);
		onServer(mc, (level, player) -> {
			BlockPos autotube = pneumaticsPos().south(8).east();
			AutotubeBlock.wrench(level, autotube, new Vec3(0.5, 15/16.0, 0.5));
			player.teleportTo(level, autotube.getX()+0.5, autotube.getY()+0.85-player.getEyeHeight(),
					autotube.getZ()+1.8, 180, 0);
		}, 15);
		screenshot(mc, "ec_autotube_wrench_cap", 2);
		onServer(mc, (level, player) -> {
			BlockPos tubes = pneumaticsPos().south(13);
			BlockPos cables = tubes.south(2);
			for(int x = 0; x<3; x++)
			{
				level.setBlockAndUpdate(tubes.east(x), ECPneumatics.TUBE.get().defaultBlockState());
				level.setBlockAndUpdate(cables.east(x), ECCables.block().get().defaultBlockState());
			}
			for(int x = 0; x<3; x++)
				for(BlockPos pos : List.of(tubes.east(x), cables.east(x)))
					level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos));
			ConnectorPorts.wrench(level, tubes.east(), new Vec3(14/16.0, 0.5, 0.5));
			ConnectorPorts.wrench(level, cables.east(), new Vec3(14/16.0, 0.5, 0.5));
			player.teleportTo(level, tubes.getX()+1.5, tubes.getY()+0.4, tubes.getZ()+4.6, 180, 32);
		}, 15);
		screenshot(mc, "ec_connector_caps", 2);
		onServer(mc, (level, player) -> {
			BlockPos tube = pneumaticsPos().south(13).east();
			player.teleportTo(level, tube.getX()+12.5/16, tube.getY()+1.8-player.getEyeHeight(),
					tube.getZ()+11.5/16+1.1, 180, 49.8F);
		}, 15);
		screenshot(mc, "ec_connector_wrench_cap", 2);
		onServer(mc, (level, player) -> {
			BlockPos tubes = pneumaticsPos().south(13);
			ConnectorPorts.wrench(level, tubes.east(), new Vec3(12.5/16, 0.5, 0.5));
			ConnectorPorts.wrench(level, tubes.south(2).east(), new Vec3(10.5/16, 0.5, 0.5));
			player.teleportTo(level, tubes.getX()+1.5, tubes.getY()+0.4, tubes.getZ()+4.6, 180, 32);
		}, 15);
		screenshot(mc, "ec_connector_reopened", 2);
		// Capping the branch makes the tube straight, but it should keep its junction box to hold the cap.
		onServer(mc, (level, player) -> {
			BlockPos middle = pneumaticsPos().south(13).east();
			level.setBlockAndUpdate(middle.south(), ECPneumatics.TUBE.get().defaultBlockState());
			level.setBlockAndUpdate(middle.south(), Block.updateFromNeighbourShapes(level.getBlockState(middle.south()),
					level, middle.south()));
			ConnectorPorts.wrench(level, middle, new Vec3(0.5, 0.5, 14/16.0));
			player.teleportTo(level, middle.getX()+0.5, middle.getY()+1.8-player.getEyeHeight(), middle.getZ()+2.2,
					180, 40);
		}, 15);
		screenshot(mc, "ec_connector_cap_on_run", 2);
		// Breaking the branch should remove the cap and leave a straight tube.
		onServer(mc, (level, player) -> level.setBlockAndUpdate(pneumaticsPos().south(13).east().south(),
				Blocks.AIR.defaultBlockState()), 15);
		screenshot(mc, "ec_connector_neighbour_gone", 2);
		onServer(mc, (level, player) -> {
			mc.options.hideGui = true;
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}, 5);
		onServer(mc, (level, player) -> {
			BlockPos start = pneumaticsPos().south(8);
			player.teleportTo(level, start.getX()+2.0, start.getY()+2.4, start.getZ()+6.0, 180, 32);
			if(level.getBlockEntity(start.east()) instanceof AutotubeBlockEntity autotube)
			{
				autotube.setAmount(8);
				autotube.fire();
			}
		}, 3);
		screenshot(mc, "ec_autotube_firing", 2);
		onServer(mc, (level, player) -> {
			player.setShiftKeyDown(true);
			use(level, player, pneumaticsPos().south(8).east());
			player.setShiftKeyDown(false);
		}, 15);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_autotube_screen", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, 8+80, 40)));
		STEPS.add(new Step(10, () -> hoverGui(mc, 8+80, 40)));
		screenshot(mc, "ec_autotube_screen_set", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, 176-8-12, 52+7)));
		STEPS.add(new Step(10, () -> hoverGui(mc, 176-8-12, 52+7)));
		screenshot(mc, "ec_autotube_screen_split", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueAdvancedStation(Minecraft mc)
	{
		ItemStack deck = new ItemStack(ECCatalogue.PROGRAM_DECK.get());
		((ProgramDeckItem)deck.getItem()).setCards(deck, List.of(
				FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.COBBLESTONE))),
				FilterCardItem.of(ProgramEntry.of(ItemTags.LOGS))));
		onServer(mc, (level, player) -> {
			BlockPos station = pneumaticsPos().north(3);
			level.setBlockAndUpdate(station, ECPneumatics.ADVANCED_TUBE_STATION.get().defaultBlockState()
					.setValue(TubeStationBlock.FACING, Direction.UP));
			level.setBlockAndUpdate(station.above(), Blocks.CHEST.defaultBlockState());
			if(level.getBlockEntity(station) instanceof TubeStationBlockEntity entity)
				entity.setFilterCard(deck.copy());
			player.teleportTo(level, station.getX()+2.3, station.getY()+1.0, station.getZ()+2.3, 135, 25);
		}, 20);
		screenshot(mc, "ec_advanced_tube_station", 2);
		onServer(mc, (level, player) -> use(level, player, pneumaticsPos().north(3)), 15);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_advanced_tube_station_screen", 2);
		int modeX = TubeStationMenu.FILTER_X-ECGui.FILTER_MODE_SIZE-3+5;
		int modeY = TubeStationMenu.FILTER_Y+(18-ECGui.FILTER_MODE_SIZE)/2+5;
		STEPS.add(new Step(5, () -> clickGui(mc, modeX, modeY)));
		STEPS.add(new Step(10, () -> hoverGui(mc, modeX, modeY)));
		screenshot(mc, "ec_advanced_tube_station_block_list", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, FilterCardItem.of(ProgramEntry.of(TagKey.create(
					Registries.ITEM, ResourceLocation.parse("c:ingots")))));
			ProgrammableItem.open(player, InteractionHand.MAIN_HAND);
		}, 15);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_filter_card_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			player.getInventory().setItem(0, new ItemStack(ECCatalogue.FILTER_CARD.get()));
			player.getInventory().setItem(1, FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.COBBLESTONE))));
		}, 5);
		// A creative player gets the creative inventory, whose hotbar is at y 112.
		STEPS.add(new Step(5, () -> mc.setScreen(new InventoryScreen(mc.player))));
		STEPS.add(new Step(5, () -> hoverGui(mc, 9+18+8, 112+8)));
		screenshot(mc, "ec_filter_card_tooltip", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			player.getInventory().setItem(0, ItemStack.EMPTY);
			player.getInventory().setItem(1, ItemStack.EMPTY);
		}, 5);
		onServer(mc, (level, player) -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, deck.copy());
			CardDeckMenu.open(player, InteractionHand.MAIN_HAND);
		}, 15);
		screenshot(mc, "ec_program_deck_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY), 5);
	}

	private static BlockPos pneumaticsPos()
	{
		return base.offset(-12, 1, 6);
	}

	private static void queueCrafting(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			BlockPos origin = craftingComputerOrigin();
			EraDriveGeometry geometry = EraMachine.CRAFTING_COMPUTER.geometry();
			for(BlockPos pos : geometry.shapes().keySet())
				level.setBlockAndUpdate(origin.offset(pos), ECContent.casing(ComputerEra.FLOPPY).get().defaultBlockState());
			if(!ECMultiblocks.CRAFTING_COMPUTER.tryForm(level, origin.offset(geometry.trigger()), Direction.SOUTH))
				EngineeredComputation.LOGGER.error("Visual check: the Crafting Computer didn't form");
			CraftingComputerState computer = (CraftingComputerState)((MultiblockPartBlockEntity)level.getBlockEntity(
					origin.offset(geometry.master()))).getState();
			computer.getPatternSlots().setStackInSlot(0, directFloppy(level, new ItemStack(Items.OAK_PLANKS),
					ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)));
			computer.getPatternSlots().setStackInSlot(1, directFloppy(level, new ItemStack(Items.COAL), ItemStack.EMPTY,
					ItemStack.EMPTY, new ItemStack(Items.STICK)));
			computer.getPatternSlots().setStackInSlot(5, directFloppy(level, new ItemStack(Items.STONE),
					new ItemStack(Items.STONE), ItemStack.EMPTY, new ItemStack(Items.STONE), new ItemStack(Items.STONE)));
			BlockPos terminal = craftingTerminalPos();
			level.setBlockAndUpdate(terminal, ECTerminals.block(ComputerEra.FLOPPY).get().defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
			ItemStack disk = new ItemStack(ECDataCells.terminalDisks().get(3).get());
			CellInventory cell = CellInventory.of(disk, () -> {
			});
			cell.insert(ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 64, false);
			cell.insert(ItemKey.of(new ItemStack(Items.COAL)), 16, false);
			cell.insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64, false);
			cell.insert(ItemKey.of(new ItemStack(Items.OAK_LOG)), 16, false);
			((TerminalDiskBlockEntity)level.getBlockEntity(terminal)).getDisks().insertItem(0, disk, false);
			placeCable(level, terminal.east());
			BlockPos bottom = dataProcessorPos();
			for(BlockPos pos : EraMachine.DATA_PROCESSOR.geometry().shapes().keySet())
				level.setBlockAndUpdate(bottom.offset(pos), ECContent.PROCESSOR_CASING.get().defaultBlockState());
			if(!ECMultiblocks.DATA_PROCESSOR.tryForm(level, bottom, Direction.SOUTH))
				EngineeredComputation.LOGGER.error("Visual check: the Advanced Data Processor didn't form");
			level.setBlockAndUpdate(bottom.south(), Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(bottom.above(2).south(), Blocks.CHEST.defaultBlockState());
			ItemStack smelting = new ItemStack(ECCrafting.PATTERN_FLOPPY.get());
			smelting.set(ECDataComponents.PATTERN.get(), new Pattern(Pattern.Mode.ABSTRACT,
					List.of(new ItemStack(Items.COBBLESTONE, 8)), List.of(new ItemStack(Items.STONE, 8))));
			Pattern logsToPlanks = new Pattern(Pattern.Mode.ABSTRACT, List.of(), List.of())
					.withTag(false, 0, ItemTags.LOGS).withTag(true, 0, ItemTags.PLANKS);
			ItemStack sawmill = new ItemStack(ECCrafting.PATTERN_FLOPPY.get());
			sawmill.set(ECDataComponents.PATTERN.get(), logsToPlanks.withOutput(0,
					logsToPlanks.output(0).copyWithCount(4)));
			DataProcessorState processor = DataProcessorState.at(level, bottom);
			processor.getPatternSlots().setStackInSlot(0, smelting);
			processor.getPatternSlots().setStackInSlot(1, sawmill);
			player.teleportTo(level, origin.getX()+4.0, origin.getY()+1.2, origin.getZ()+5.5, 150, 15);
		}, 40);
		screenshot(mc, "ec_crafting_computer", 2);
		onServer(mc, (level, player) -> {
			BlockPos origin = craftingComputerOrigin();
			player.teleportTo(level, origin.getX()+1.5, origin.getY()-0.1, origin.getZ()+4.2, 180, 8);
		}, 15);
		screenshot(mc, "ec_crafting_computer_bays", 2);
		onServer(mc, (level, player) -> {
			BlockPos origin = craftingComputerOrigin();
			player.teleportTo(level, origin.getX()-0.4, origin.getY()+0.6, origin.getZ()+2.6, 215, 20);
		}, 15);
		screenshot(mc, "ec_crafting_computer_bays_glancing", 2);
		onServer(mc, (level, player) -> {
			BlockPos origin = craftingComputerOrigin();
			player.teleportTo(level, origin.getX()+1.5, origin.getY()+1.6, origin.getZ()-3.0, 0, 10);
		}, 15);
		screenshot(mc, "ec_crafting_computer_back", 2);
		onServer(mc, (level, player) -> {
			BlockPos bottom = dataProcessorPos();
			player.teleportTo(level, bottom.getX()+2.2, bottom.getY()-0.3, bottom.getZ()+3.2, 145, 12);
		}, 15);
		screenshot(mc, "ec_data_processor", 2);
		onServer(mc, (level, player) -> {
			BlockPos bottom = dataProcessorPos();
			player.teleportTo(level, bottom.getX()+0.5, bottom.getY()-0.2, bottom.getZ()+3.6, 180, 0);
		}, 15);
		screenshot(mc, "ec_data_processor_front", 2);
		onServer(mc, (level, player) -> {
			BlockPos bottom = dataProcessorPos();
			player.teleportTo(level, bottom.getX()+1.8, bottom.getY()-2.6, bottom.getZ()+2.2, 140, -60);
		}, 15);
		screenshot(mc, "ec_data_processor_below", 2);
		onServer(mc, (level, player) -> use(level, player, dataProcessorPos()), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_data_processor_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player,
				craftingComputerOrigin().offset(EraMachine.CRAFTING_COMPUTER.geometry().trigger())), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_crafting_computer_idle", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		for(String mode : new String[]{"direct", "abstract", "tags"})
		{
			onServer(mc, (level, player) -> {
				CraftingComputerState computer = (CraftingComputerState)((MultiblockPartBlockEntity)level.getBlockEntity(
						craftingComputerOrigin().offset(EraMachine.CRAFTING_COMPUTER.geometry().master()))).getState();
				DataProcessorState processor = DataProcessorState.at(level, dataProcessorPos());
				ItemStack floppy = switch(mode)
				{
					case "direct" -> computer.getPatternSlots().getStackInSlot(1).copy();
					case "abstract" -> processor.getPatternSlots().getStackInSlot(0).copy();
					default -> processor.getPatternSlots().getStackInSlot(1).copy();
				};
				player.setItemInHand(InteractionHand.MAIN_HAND, floppy);
				floppy.use(level, player, InteractionHand.MAIN_HAND);
			}, 20);
			STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
			screenshot(mc, "ec_pattern_floppy_"+mode, 2);
			STEPS.add(new Step(5, () -> mc.setScreen(null)));
		}
		onServer(mc, (level, player) -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			use(level, player, craftingTerminalPos());
		}, 25);
		TerminalMenu.Layout ordering = TerminalMenu.Layout.of(TerminalFeatures.ORDERING);
		STEPS.add(new Step(20, () -> hoverGui(mc, TerminalMenu.GRID_X+4*18+9, ordering.gridY()+9)));
		screenshot(mc, "ec_terminal_offer", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, TerminalMenu.GRID_X+4*18+9, ordering.gridY()+9)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_order_popup_pick", 5);
		STEPS.add(new Step(5, () -> clickGui(mc, (195-168)/2+168-26+9, ordering.gridY()-8+44+9)));
		STEPS.add(new Step(5, () -> hoverGui(mc, (195-168)/2+6+30, ordering.gridY()-8+44+18+1+9)));
		screenshot(mc, "ec_order_popup_list", 5);
		STEPS.add(new Step(5, () -> clickGui(mc, (195-168)/2+6+30, ordering.gridY()-8+44+18+1+9)));
		STEPS.add(new Step(15, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_order_popup_picked", 5);
		STEPS.add(new Step(5, () -> clickGui(mc, (195-168)/2+168/2-50-3+25, ordering.gridY()-8+178-14-6+7)));
		STEPS.add(new Step(15, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, craftingTerminalPos()), 25);
		STEPS.add(new Step(20, () -> hoverGui(mc, TerminalMenu.GRID_X+6*18+9, ordering.gridY()+9)));
		screenshot(mc, "ec_terminal_ordering", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, TerminalMenu.GRID_X+6*18+9, ordering.gridY()+9)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_order_popup", 15);
		STEPS.add(new Step(5, () -> clickGui(mc, (195-168)/2+168/2-50-3+25, ordering.gridY()-8+178-14-6+7)));
		STEPS.add(new Step(25, () -> clickGui(mc, 195-7-11+5, 9)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_terminal_jobs", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			StorageNetwork network = StorageNetwork.of(level, craftingTerminalPos());
			CraftingNetwork.order(level, network, network.getStorage(), ItemKey.of(new ItemStack(Items.STONE_BRICKS)), 4);
			use(level, player, craftingComputerOrigin().offset(EraMachine.CRAFTING_COMPUTER.geometry().trigger()));
		}, 20);
		STEPS.add(new Step(15, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_crafting_computer_screen", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, CraftingComputerMenu.DISPLAY_X+CraftingComputerMenu.DISPLAY_WIDTH-4-4,
				CraftingComputerMenu.DISPLAY_Y+17)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_crafting_computer_tree", 2);
		int cancelX = CraftingComputerMenu.DISPLAY_X+CraftingComputerMenu.DISPLAY_WIDTH-4-9+4;
		int cancelY = CraftingComputerMenu.DISPLAY_Y+3+4;
		STEPS.add(new Step(5, () -> hoverGui(mc, cancelX-14, cancelY)));
		screenshot(mc, "ec_crafting_computer_elapsed", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, cancelX, cancelY)));
		screenshot(mc, "ec_crafting_computer_cancel", 2);
		STEPS.add(new Step(5, () -> clickGui(mc, cancelX, cancelY)));
		STEPS.add(new Step(1, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_crafting_computer_cancelling", 0);
		STEPS.add(new Step(20, () -> {
		}));
		screenshot(mc, "ec_crafting_computer_cancelled", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueStandaloneDrives(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			drive(level, EraDrive.DISK_PROCESSOR).getCellSlots().setStackInSlot(0,
					new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(1).get()));
			var processor = drive(level, EraDrive.FLOPPY_PROCESSOR).getCellSlots();
			processor.setStackInSlot(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(2).get()));
			processor.setStackInSlot(1, new ItemStack(ECDataCells.terminalDisks().get(0).get()));
			var bay = drive(level, EraDrive.COMPACT_FLOPPY_BAY).getCellSlots();
			for(int slot : new int[]{0, 1, 3})
				bay.setStackInSlot(slot, new ItemStack(ECDataCells.terminalDisks().get(slot).get()));
		}, 10);
		for(EraDrive drive : List.of(EraDrive.DISK_PROCESSOR, EraDrive.FLOPPY_PROCESSOR, EraDrive.COMPACT_FLOPPY_BAY))
		{
			BlockPos size = drive.geometry().size();
			double midX = size.getX()/2.0;
			String name = "ec_"+drive.blockName();
			camera(mc, name+"_front", drive, midX, size.getY()/2.0-0.6, size.getZ()+2.2+size.getY()/2.0, 180, 8);
			camera(mc, name+"_angle", drive, midX+2.4+size.getX()/2.0, size.getY()/2.0, size.getZ()+2.4, 135, 15);
			camera(mc, name+"_back", drive, midX, 0.4, -2.0-size.getY()/2.0, 0, 12);
		}
		camera(mc, "ec_era5_floppy_processor_bays", EraDrive.FLOPPY_PROCESSOR, 0.5, 0.9, 3.1, 180, 10);
		camera(mc, "ec_era5_compact_floppy_bay_bays", EraDrive.COMPACT_FLOPPY_BAY, 0.5, 0.8, 2.3, 180, 15);
		onServer(mc, (level, player) -> use(level, player, driveOrigin(EraDrive.FLOPPY_PROCESSOR)
				.offset(EraDrive.FLOPPY_PROCESSOR.geometry().trigger())), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_era5_floppy_processor_screen", 2);
		STEPS.add(new Step(5, () -> hoverGui(mc, 89-9*3+2*18+8, EraDriveMenu.CELLS_Y+8)));
		screenshot(mc, "ec_era5_floppy_processor_slot_tooltip", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static BlockPos craftingComputerOrigin()
	{
		return base.offset(EraDrive.values().length*7+2, 0, -8);
	}

	private static BlockPos dataProcessorPos()
	{
		return craftingTerminalPos().east().south();
	}

	private static BlockPos craftingTerminalPos()
	{
		NetworkPort port = EraMachine.CRAFTING_COMPUTER.geometry().ports().get(0);
		return craftingComputerOrigin().offset(port.pos()).relative(port.side());
	}

	private static ItemStack directFloppy(ServerLevel level, ItemStack... grid)
	{
		ItemStack floppy = new ItemStack(ECCrafting.PATTERN_FLOPPY.get());
		floppy.set(ECDataComponents.PATTERN.get(),
				new Pattern(Pattern.Mode.DIRECT, List.of(grid), List.of()).withCraftedOutput(level));
		return floppy;
	}

	private static BlockPos coveredCablesPos()
	{
		return base.offset(28, 1, 4);
	}

	private static void queueAdapters(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			BlockPos row = adapterRow();
			int x = 0;
			for(ComputerEra era : ComputerEra.values())
				for(AdapterKind kind : ECAdapters.of(era).keySet())
				{
					BlockPos at = row.east(x);
					x += 2;
					level.setBlockAndUpdate(at, ECAdapters.of(era).get(kind).get().defaultBlockState()
							.setValue(DataAdapterBlock.FACING, Direction.NORTH));
					placeCable(level, at.south());
				}
			BlockPos importer = workingImporter();
			level.setBlockAndUpdate(importer, ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.IMPORTER).get()
					.defaultBlockState().setValue(DataAdapterBlock.FACING, Direction.UP));
			level.setBlockAndUpdate(importer.above(), Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity)level.getBlockEntity(importer.above())).setItem(0, new ItemStack(Items.REDSTONE, 64));
			BlockPos exporter = workingImporter().north();
			level.setBlockAndUpdate(exporter, ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.EXPORTER).get()
					.defaultBlockState().setValue(DataAdapterBlock.FACING, Direction.UP));
			level.setBlockAndUpdate(exporter.above(), Blocks.CHEST.defaultBlockState());
			((DataAdapterBlockEntity)level.getBlockEntity(exporter)).setFilter(new ItemStack(Items.DIAMOND));
			for(int step = 0; step<4; step++)
				placeCable(level, outsidePort(EraDrive.DRUM_COMPUTER).north(step));
			player.teleportTo(level, row.getX()+7.5, row.getY()+1.2, row.getZ()-4.5, 0, 12);
		}, 40);
		screenshot(mc, "ec_adapters_row", 2);
		for(int pair = 0; pair<4; pair++)
		{
			int first = pair*4;
			onServer(mc, (level, player) -> {
				BlockPos row = adapterRow();
				player.teleportTo(level, row.getX()+first+3.4, row.getY()+1.6, row.getZ()-1.6, 35, 30);
			}, 15);
			screenshot(mc, "ec_adapters_pair_"+pair, 2);
		}
		onServer(mc, (level, player) -> {
			BlockPos row = adapterRow();
			player.teleportTo(level, row.getX()+3.5, row.getY()+1.9, row.getZ()+2.6, 160, 32);
		}, 15);
		screenshot(mc, "ec_adapters_back", 2);
		onServer(mc, (level, player) -> {
			int slot = 0;
			for(ComputerEra era : ComputerEra.values())
				for(AdapterKind kind : ECAdapters.of(era).keySet())
					player.getInventory().setItem(slot++, new ItemStack(ECAdapters.of(era).get(kind).get()));
		}, 5);
		screenshot(mc, "ec_adapters_items", 2);
		onServer(mc, (level, player) -> {
			for(int slot = 0; slot<9; slot++)
				player.getInventory().setItem(slot, ItemStack.EMPTY);
		}, 5);
		onServer(mc, (level, player) -> {
			BlockPos importer = workingImporter();
			player.teleportTo(level, importer.getX()-1.6, importer.getY()+0.8, importer.getZ()-1.6, -45, 18);
		}, 30);
		screenshot(mc, "ec_adapters_working", 2);
		STEPS.add(new Step(5, () -> mc.options.hideGui = false));
		onServer(mc, (level, player) -> use(level, player, workingImporter()), 20);
		screenshot(mc, "ec_adapter_importer_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, workingImporter().north()), 20);
		screenshot(mc, "ec_adapter_exporter_screen", 2);
		STEPS.add(new Step(25, () -> clickGui(mc, 176-8-12, 17+13)));
		screenshot(mc, "ec_adapter_exporter_off", 2);
		STEPS.add(new Step(25, () -> clickGui(mc, 4+5, 29+5)));
		STEPS.add(new Step(5, () -> hoverGui(mc, 4+5, 29+5)));
		screenshot(mc, "ec_adapter_exporter_block_list", 2);
		STEPS.add(new Step(25, () -> clickGui(mc, 4+5, 29+5)));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> level.setBlockAndUpdate(workingImporter().north(3),
				ECAdapters.of(ComputerEra.DRUM).get(AdapterKind.IMPORTER).get().defaultBlockState()), 30);
		onServer(mc, (level, player) -> use(level, player, workingImporter().north(3)), 20);
		screenshot(mc, "ec_adapter_503", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			ItemStack floppy = new ItemStack(ECAdapters.PROGRAM_FLOPPY.get());
			floppy.set(ECDataComponents.PROGRAM.get(), Program.EMPTY
					.with(0, ProgramEntry.of(new ItemStack(Items.REDSTONE)))
					.with(1, ProgramEntry.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:ingots"))))
					.with(2, ProgramEntry.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:logs")))));
			player.setItemInHand(InteractionHand.MAIN_HAND, floppy);
			floppy.use(level, player, InteractionHand.MAIN_HAND);
		}, 20);
		screenshot(mc, "ec_program_floppy_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY), 5);
	}

	private static BlockPos adapterRow()
	{
		return base.offset(0, 0, -15);
	}

	private static BlockPos workingImporter()
	{
		return outsidePort(EraDrive.DRUM_COMPUTER).north(1).above();
	}

	private static void placeCable(ServerLevel level, BlockPos pos)
	{
		level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(ECCables.block().get().defaultBlockState(), level, pos));
	}

	private static BlockPos crossEraTerminalPos()
	{
		return driveOrigin(ComputerEra.TAPE).north(4);
	}

	private static BlockPos terminalRowPos(ComputerEra era)
	{
		return base.offset(20+era.ordinal()*2, 0, 2);
	}

	private static BlockPos bridgePos()
	{
		return base.offset(20+ComputerEra.values().length*2+1, 0, 2);
	}

	private static BlockPos cableTerminalPos()
	{
		return outsidePort(EraDrive.DRUM_COMPUTER).north(4);
	}

	private static void queueConsoleClicks(Minecraft mc)
	{
		consoleClick(mc, ComputerEra.DISK, "screen", 2.19, 0.0, 4.2, 180, 0);
		consoleClick(mc, ComputerEra.DISK, "keyboard", 2.19, 0.38, 4.0, 180, 33.7F);
		consoleClick(mc, ComputerEra.DISK, "disk_unit", 0.69, -1.12, 4.3, 180, 0);
		consoleClick(mc, ComputerEra.FLOPPY, "floppy_screen", 2.0, 0.23, 3.5, 180, 0);
		consoleClick(mc, ComputerEra.FLOPPY, "floppy_keyboard", 2.0, 0.6, 3.2, 180, 40);
		consoleClick(mc, ComputerEra.FLOPPY, "floppy_drawers", 1.4, -0.72, 3.0, 180, 22.8F);
	}

	private static void consoleClick(Minecraft mc, ComputerEra era, String target, double x, double y, double z,
			float yaw, float pitch)
	{
		camera(mc, "ec_console_aim_"+target, era, x, y, z, yaw, pitch);
		STEPS.add(new Step(10, () -> KeyMapping.click(mc.options.keyUse.getKey())));
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_console_click_"+target, 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static void queueCardVault(Minecraft mc)
	{
		camera(mc, "ec_vault_front", ComputerEra.PUNCHCARD, 1.5, -0.4, 4.6, 180, 12);
		camera(mc, "ec_vault_right", ComputerEra.PUNCHCARD, 4.6, -0.2, 4.2, 135, 18);
		camera(mc, "ec_vault_left", ComputerEra.PUNCHCARD, -1.6, -0.2, 4.2, 225, 18);
		camera(mc, "ec_vault_sorter", ComputerEra.PUNCHCARD, 3.9, 0.4, 3.0, 125, 38);
		onServer(mc, (level, player) -> drive(level, ComputerEra.PUNCHCARD).getCellSlots().insertItem(2,
				new ItemStack(ECCatalogue.decks().get(1).get()), false), 0);
		for(int frame = 0; frame<4; frame++)
			screenshot(mc, "ec_vault_drop_"+frame, 2);
		onServer(mc, (level, player) -> drive(level, ComputerEra.PUNCHCARD).getCellSlots().extractItem(2, 1, false), 0);
		screenshot(mc, "ec_vault_lift", 2);
		STEPS.add(new Step(10, () -> {
		}));
		// Each deck should rest against the step behind it.
		onServer(mc, (level, player) -> {
			var slots = drive(level, ComputerEra.PUNCHCARD).getCellSlots();
			for(int slot = 0; slot<slots.getSlots(); slot++)
			{
				slots.extractItem(slot, 1, false);
				slots.insertItem(slot, new ItemStack(ECCatalogue.decks().get(slot).get()), false);
			}
		}, 40);
		camera(mc, "ec_vault_decks_above", ComputerEra.PUNCHCARD, 3.9, 1.2, 1.2, 100, 40);
		camera(mc, "ec_vault_decks_side", ComputerEra.PUNCHCARD, 4.0, 0.8, 2.2, 120, 30);
		camera(mc, "ec_vault_decks_angled", ComputerEra.PUNCHCARD, 2.6, 0.6, 3.0, 135, 25);
		camera(mc, "ec_vault_back", ComputerEra.PUNCHCARD, 1.5, 0.6, -2.4, 0, 25);
		camera(mc, "ec_vault_under", ComputerEra.PUNCHCARD, 1.5, -2.9, 4.0, 180, -35);
	}

	private static void queueDrumEra(Minecraft mc)
	{
		camera(mc, "ec_drum_computer_front", EraDrive.DRUM_COMPUTER, 1.0, -0.4, 4.4, 180, 10);
		camera(mc, "ec_drum_computer_left", EraDrive.DRUM_COMPUTER, -1.4, 0, 3.6, 225, 15);
		camera(mc, "ec_drum_computer_right", EraDrive.DRUM_COMPUTER, 3.4, 0, 3.6, 135, 15);
		camera(mc, "ec_drum_computer_back", EraDrive.DRUM_COMPUTER, 1.0, 0.8, -2.4, 0, 20);
		camera(mc, "ec_drum_bank_front", EraDrive.DRUM_BANK, 2.0, -0.2, 5.0, 180, 8);
		camera(mc, "ec_drum_bank_angle", EraDrive.DRUM_BANK, 5.2, 0.2, 4.2, 140, 15);
		camera(mc, "ec_drum_bank_turning_0", EraDrive.DRUM_BANK, 2.0, -0.1, 3.0, 180, 12);
		screenshot(mc, "ec_drum_bank_turning_1", 5);
		onServer(mc, (level, player) -> {
			BlockPos typewriter = terminalRowPos(ComputerEra.DRUM);
			player.teleportTo(level, typewriter.getX()+0.5, typewriter.getY()-0.8, typewriter.getZ()+1.9, 180, 25);
		}, 15);
		screenshot(mc, "ec_typewriter", 2);
		onServer(mc, (level, player) -> {
			BlockPos typewriter = terminalRowPos(ComputerEra.DRUM);
			player.teleportTo(level, typewriter.getX()+1.8, typewriter.getY()-0.6, typewriter.getZ()+1.6, 135, 25);
		}, 15);
		screenshot(mc, "ec_typewriter_side", 2);
	}

	private static void queueModelAngles(Minecraft mc)
	{
		for(EraDrive drive : EraDrive.values())
		{
			if(ANGLE_DRIVES.isEmpty() ? drive.era().ordinal()<ComputerEra.DISK.ordinal()
					: !ANGLE_DRIVES.contains(drive.blockName()))
				continue;
			BlockPos size = drive.geometry().size();
			double midX = size.getX()/2.0;
			String name = "ec_angle_"+drive.blockName();
			camera(mc, name+"_a_front_below", drive, midX, 0, size.getZ()+1.2, 180, -55);
			camera(mc, name+"_f_underneath", drive, midX, -FLOAT_HEIGHT+0.2, size.getZ()+2.5, 180, -40);
			camera(mc, name+"_b_above", drive, midX, size.getY()+1.5, size.getZ()+1.8, 180, 50);
			camera(mc, name+"_c_back", drive, midX, size.getY()-0.5, -2.2, 0, 30);
			camera(mc, name+"_d_side_below", drive, size.getX()+1.4, 0, size.getZ()/2.0, 90, -50);
			camera(mc, name+"_e_front", drive, midX, 0.5, size.getZ()+2.6, 180, 20);
		}
	}

	private static void queueCloseups(Minecraft mc)
	{
		camera(mc, "ec_close_era4_disks", ComputerEra.DISK, 0.7, -0.9, 4.0, 180, -25);
		camera(mc, "ec_close_era4_disks_side", ComputerEra.DISK, 2.4, -0.7, 3.8, 145, -25);
		camera(mc, "ec_close_era4_disks_left", ComputerEra.DISK, -1.2, -0.7, 3.6, 215, -25);
		camera(mc, "ec_close_era5_tower_below", ComputerEra.FLOPPY, 0.5, 0, 2.5, 180, -65);
		camera(mc, "ec_close_era5_tower_below_right", ComputerEra.FLOPPY, 2.5, 0, 2.5, 160, -60);
		camera(mc, "ec_close_era5_back", ComputerEra.FLOPPY, 1.5, 2.2, -1.6, 0, 15);
		camera(mc, "ec_close_era5_back_high", ComputerEra.FLOPPY, 1.5, 3.4, -1.4, 0, 35);
	}

	private static void queueFloppyBays(Minecraft mc)
	{
		ItemStack[] disks = {
				new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()),
				new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(1).get())};
		String[] states = {"empty", "lower", "both", "upper"};
		for(int step = 0; step<states.length; step++)
		{
			int state = step;
			onServer(mc, (level, player) -> {
				for(BlockPos master : List.of(floppyMaster(false), floppyMaster(true)))
				{
					var slots = ((EraDriveState)((MultiblockPartBlockEntity)level.getBlockEntity(master)).getState()).getCellSlots();
					slots.setStackInSlot(0, state==1||state==2? disks[0].copy(): ItemStack.EMPTY);
					slots.setStackInSlot(1, state==2||state==3? disks[1].copy(): ItemStack.EMPTY);
				}
			}, 5);
			camera(mc, "ec_floppy_"+state+"_front", ComputerEra.FLOPPY, 0.5, 1.3, 3.3, 180, 0);
			camera(mc, "ec_floppy_"+state+"_angle", ComputerEra.FLOPPY, 1.9, 1.7, 3.0, 145, 15);
			onServer(mc, (level, player) -> {
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				BlockPos origin = rotatedFloppyOrigin();
				player.teleportTo(level, origin.getX()-2.3, origin.getY()+1.3, origin.getZ()+0.5, -90, 0);
			}, 15);
			screenshot(mc, "ec_floppy_"+state+"_east_facing", 2);
		}
	}

	private static void queueTapeReels(Minecraft mc)
	{
		onServer(mc, (level, player) -> {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			BlockPos origin = driveOrigin(ComputerEra.TAPE);
			player.teleportTo(level, origin.getX()+1.0, origin.getY()+3.3, origin.getZ()+3.6, 180, 8);
		}, 20);
		for(int frame = 0; frame<6; frame++)
			screenshot(mc, String.format("ec_tape_wind_%02d", frame), 30);
		onServer(mc, (level, player) -> drive(level, ComputerEra.TAPE).getCellSlots().extractItem(0, 1, false), 0);
		for(int frame = 0; frame<4; frame++)
			screenshot(mc, String.format("ec_tape_unload_%02d", frame), 6);
		onServer(mc, (level, player) -> drive(level, ComputerEra.TAPE).getCellSlots().insertItem(0,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).get(2).get()), false), 0);
		for(int frame = 0; frame<4; frame++)
			screenshot(mc, String.format("ec_tape_load_%02d", frame), 6);
		onServer(mc, (level, player) -> {
			BlockPos origin = driveOrigin(ComputerEra.DISK);
			player.teleportTo(level, origin.getX()+1.7, origin.getY()+1.6, origin.getZ()+4.3, 155, 40);
		}, 20);
		for(int frame = 0; frame<3; frame++)
			screenshot(mc, String.format("ec_diskpack_spin_%02d", frame), 2);
		camera(mc, "ec_diskpack_lid_close", ComputerEra.DISK, 0.2, 0.9, 2.6, 180, 45);
		for(int frame = 0; frame<3; frame++)
			camera(mc, String.format("ec_tape_reels_close_%02d", frame), ComputerEra.TAPE, 1.0, 2.5, 2.9, 180, 5);
	}

	private static void queueDiskPack(Minecraft mc)
	{
		float[][] views = {{1.7F, 1.1F, 4.3F, 155, 35}, {-1.7F, 1.0F, 2.4F, -90, 25}};
		String[] names = {"front", "side"};
		for(int v = 0; v<views.length; v++)
		{
			float[] view = views[v];
			camera(mc, "ec_diskpack_"+names[v]+"_loaded", ComputerEra.DISK, view[0], view[1], view[2], view[3], view[4]);
			for(boolean load : new boolean[]{false, true})
			{
				onServer(mc, (level, player) -> {
					var slots = drive(level, ComputerEra.DISK).getCellSlots();
					if(load)
						slots.setStackInSlot(0, removedDiskPack);
					else
					{
						removedDiskPack = slots.getStackInSlot(0).copy();
						slots.setStackInSlot(0, ItemStack.EMPTY);
					}
				}, 1);
				for(int frame = 0; frame<9; frame++)
					screenshot(mc, String.format("ec_diskpack_%s_%s_%02d", names[v], load? "load": "unload", frame), 6);
			}
		}
	}

	private static void queueBanks(Minecraft mc)
	{
		camera(mc, "ec_floppy_bank_front", EraDrive.FLOPPY_BANK, 0.5, 1.0, 4.5, 180, 5);
		onServer(mc, (level, player) -> drive(level, EraDrive.FLOPPY_BANK).getCellSlots().insertItem(2,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(4).get()), false), 1);
		for(int frame = 0; frame<4; frame++)
			screenshot(mc, String.format("ec_floppy_bank_insert_%02d", frame), 1);
		camera(mc, "ec_floppy_bank_close", EraDrive.FLOPPY_BANK, 0.5, 0.6, 2.2, 180, 25);
		camera(mc, "ec_floppy_bank_angle", EraDrive.FLOPPY_BANK, 2.2, 1.4, 4.0, 150, 12);
		camera(mc, "ec_disk_bank_empty", EraDrive.DISK_BANK, 2.6, 1.2, 3.6, 145, 27);
		onServer(mc, (level, player) -> drive(level, EraDrive.DISK_BANK).getCellSlots().insertItem(0,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(0).get()), false), 1);
		for(int frame = 0; frame<5; frame++)
			screenshot(mc, String.format("ec_disk_bank_load_%02d", frame), 10);
		camera(mc, "ec_disk_bank_loaded", EraDrive.DISK_BANK, 2.6, 1.2, 3.6, 145, 27);
		camera(mc, "ec_disk_bank_back", EraDrive.DISK_BANK, 1.0, 1.4, -2.4, 15, 25);
		onServer(mc, (level, player) -> {
			BlockPos middle = terminalRowPos(ComputerEra.TAPE);
			player.teleportTo(level, middle.getX()+0.5, middle.getY()+0.4, middle.getZ()+7.5, 180, 16);
		}, 15);
		screenshot(mc, "ec_terminals", 2);
		onServer(mc, (level, player) -> {
			BlockPos floppy = terminalRowPos(ComputerEra.FLOPPY);
			player.teleportTo(level, floppy.getX()+0.5, floppy.getY()-1.0, floppy.getZ()+1.9, 180, 25);
		}, 15);
		screenshot(mc, "ec_floppy_terminal_bays", 2);
		onServer(mc, (level, player) -> {
			BlockPos bridge = bridgePos();
			player.teleportTo(level, bridge.getX()+1.8, bridge.getY()-0.6, bridge.getZ()+2.4, 145, 20);
		}, 15);
		screenshot(mc, "ec_storage_bridge", 2);
		// The back should be vents, not a cable port, since cables only join the sides.
		onServer(mc, (level, player) -> {
			BlockPos bridge = bridgePos();
			player.teleportTo(level, bridge.getX()-0.8, bridge.getY()-0.6, bridge.getZ()-1.4, -34, 20);
		}, 15);
		screenshot(mc, "ec_storage_bridge_back", 2);
	}

	private static ItemStack filled(ItemStack cell, boolean full)
	{
		CellInventory inventory = CellInventory.of(cell, () -> {
		});
		inventory.insert(ItemKey.of(new ItemStack(Items.STONE)), 1, false);
		for(var item : BuiltInRegistries.ITEM)
			if(inventory.isTypesFull())
				break;
			else if(item!=Items.AIR&&!(item instanceof EraDataCell))
				inventory.insert(ItemKey.of(new ItemStack(item)), 1, false);
		if(full)
			inventory.insert(ItemKey.of(new ItemStack(Items.STONE)), Integer.MAX_VALUE, false);
		return cell;
	}

	private static BlockPos rotatedFloppyOrigin()
	{
		return base.offset(54, 0, -8);
	}

	private static BlockPos floppyMaster(boolean rotated)
	{
		BlockPos master = EraDrive.FLOPPY_WORKSTATION.geometry().master();
		return rotated
				? rotatedFloppyOrigin().offset(MultiblockOrientation.toWorldOffset(master, Direction.EAST))
				: driveOrigin(ComputerEra.FLOPPY).offset(master);
	}

	private static void camera(Minecraft mc, String shot, ComputerEra era, double x, double y, double z, float yaw,
			float pitch)
	{
		camera(mc, shot, era.drives().get(0), x, y, z, yaw, pitch);
	}

	private static void camera(Minecraft mc, String shot, EraDrive drive, double x, double y, double z, float yaw,
			float pitch)
	{
		onServer(mc, (level, player) -> {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			BlockPos origin = driveOrigin(drive);
			player.teleportTo(level, origin.getX()+x, origin.getY()+y, origin.getZ()+z, yaw, pitch);
		}, 15);
		screenshot(mc, shot, 2);
	}

	private static void buildScene(ServerLevel level, ServerPlayer player)
	{
		base = player.blockPosition().above(FLOAT_HEIGHT);
		level.setDayTime(6000);
		level.setWeatherParameters(100000, 0, false, false);
		for(BlockPos pos : BlockPos.betweenClosed(base.offset(-4, 0, -16), base.offset(90, 8, 4)))
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

		for(EraDrive drive : EraDrive.values())
		{
			EraDriveGeometry geometry = drive.geometry();
			BlockPos origin = driveOrigin(drive);
			for(BlockPos pos : geometry.shapes().keySet())
				level.setBlockAndUpdate(origin.offset(pos), ECContent.casing(drive.era()).get().defaultBlockState());
			if(!ECMultiblocks.ERA_DRIVES.get(drive).tryForm(level, origin.offset(geometry.trigger()), Direction.SOUTH))
				EngineeredComputation.LOGGER.error("Visual check: {} didn't form", drive);
		}
		for(ComputerEra era : ComputerEra.values())
			if(era!=ComputerEra.TAPE)
				level.setBlockAndUpdate(terminalPos(era), ECTerminals.block(era).get().defaultBlockState()
						.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

		EraDriveGeometry floppy = EraDrive.FLOPPY_WORKSTATION.geometry();
		for(BlockPos pos : floppy.shapes().keySet())
			level.setBlockAndUpdate(rotatedFloppyOrigin().offset(MultiblockOrientation.toWorldOffset(pos, Direction.EAST)),
					ECContent.casing(ComputerEra.FLOPPY).get().defaultBlockState());
		if(!ECMultiblocks.ERA_DRIVES.get(EraDrive.FLOPPY_WORKSTATION).tryForm(level,
				rotatedFloppyOrigin().offset(MultiblockOrientation.toWorldOffset(floppy.trigger(), Direction.EAST)),
				Direction.WEST))
			EngineeredComputation.LOGGER.error("Visual check: east-facing floppy drive didn't form");

		EraDriveState disk = drive(level, ComputerEra.DISK);
		disk.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DISK).get(3).get()), false);
		List<CellInventory> cells = disk.getCells();
		int[] amounts = {1, 7, 64, 250, 999, 1000, 4321, 12345, 64000};
		int i = 0;
		for(var item : BuiltInRegistries.ITEM)
		{
			if(i>=80||item==Items.AIR||item instanceof EraDataCell)
				continue;
			long amount = amounts[i%amounts.length];
			ItemKey key = ItemKey.of(new ItemStack(item));
			for(CellInventory cell : cells)
				amount -= cell.insert(key, amount, false);
			i++;
		}
		for(CellInventory cell : cells)
			cell.insert(ItemKey.of(new ItemStack(Items.DIAMOND_SWORD)), 500, false);

		for(int step = 0; step<4; step++)
			placeCable(level, outsidePort(EraDrive.DRUM_COMPUTER).north(step));
		for(int step = 1; step<=3; step++)
			placeCable(level, driveOrigin(ComputerEra.TAPE).north(step));
		level.setBlockAndUpdate(crossEraTerminalPos(), ECTerminals.block(ComputerEra.FLOPPY).get().defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		((TerminalDiskBlockEntity)level.getBlockEntity(crossEraTerminalPos())).getDisks().insertItem(0,
				new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);
		drive(level, ComputerEra.TAPE).getCellSlots().insertItem(0,
				new ItemStack(ECDataCells.cellsOf(ComputerEra.TAPE).get(0).get()), false);

		BlockPos floppyPort = driveOrigin(EraDrive.FLOPPY_WORKSTATION);
		placeCable(level, floppyPort.north());
		placeCable(level, floppyPort.north(2));
		placeCable(level, floppyPort.west());

		EraDriveState floppyBank = drive(level, EraDrive.FLOPPY_BANK);
		floppyBank.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()),
				false);
		floppyBank.getCellSlots().insertItem(1,
				filled(new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(1).get()), false), false);
		floppyBank.getCellSlots().insertItem(3,
				filled(new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(2).get()), true), false);

		for(ComputerEra era : ComputerEra.values())
			if(era.hasTerminal())
				level.setBlockAndUpdate(terminalRowPos(era), ECTerminals.block(era).get().defaultBlockState()
						.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		TerminalDiskBlockEntity bays = (TerminalDiskBlockEntity)level.getBlockEntity(terminalRowPos(ComputerEra.FLOPPY));
		bays.getDisks().insertItem(0, new ItemStack(ECDataCells.terminalDisks().get(0).get()), false);
		bays.getDisks().insertItem(1, filled(new ItemStack(ECDataCells.terminalDisks().get(3).get()), true), false);
		BlockPos bridge = bridgePos();
		level.setBlockAndUpdate(bridge, ECBridges.block().get().defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		placeCable(level, bridge.east());
		placeCable(level, bridge.west());

		BlockPos receiver = outsidePort(EraDrive.PUNCHCARD_VAULT);
		level.setBlockAndUpdate(receiver, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		level.setBlockAndUpdate(receiver.east(), Block.updateFromNeighbourShapes(
				ECPneumatics.TUBE.get().defaultBlockState(), level, receiver.east()));
		level.setBlockAndUpdate(receiver.east(3), Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(receiver.east(2), ECPneumatics.TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.EAST));
		if(level.getBlockEntity(receiver.east(3)) instanceof ChestBlockEntity chest)
		{
			chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
			chest.setItem(1, new ItemStack(Items.OAK_LOG, 23));
			chest.setItem(2, new ItemStack(Items.IRON_INGOT, 9));
			chest.setItem(3, new ItemStack(Items.REDSTONE, 40));
		}
		TubeStationBlockEntity station = (TubeStationBlockEntity)level.getBlockEntity(receiver.east(2));
		BlockPos lockedStation = receiver.east().north();
		level.setBlockAndUpdate(lockedStation.north(), Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(lockedStation, ECPneumatics.ADVANCED_TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.NORTH));
		if(level.getBlockEntity(lockedStation.north()) instanceof ChestBlockEntity chest)
		{
			chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
			chest.setItem(1, new ItemStack(Items.REDSTONE, 16));
		}
		TubeStationBlockEntity locked = (TubeStationBlockEntity)level.getBlockEntity(lockedStation);
		locked.setInsertOnly(true);
		locked.setName("Overflow");
		ItemStack deck = new ItemStack(ECCatalogue.decks().get(0).get());
		CardDeckItem.setDeck(deck, CardDeck.EMPTY.with(new CatalogueCard(GlobalPos.of(level.dimension(),
				receiver.east(2)), station.label(), station.tally(), level.getGameTime())).with(new CatalogueCard(
				GlobalPos.of(level.dimension(), lockedStation), locked.label(), locked.tally(), level.getGameTime())));
		level.setBlockAndUpdate(receiver.east().above(), ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		((ReceivingStationBlockEntity)level.getBlockEntity(receiver.east().above())).setName("Packing Room");
		level.setBlockAndUpdate(receiver.east().below(), ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		((ReceivingStationBlockEntity)level.getBlockEntity(receiver.east().below())).setName("Smithy");
		var vaultSlots = drive(level, ComputerEra.PUNCHCARD).getCellSlots();
		vaultSlots.insertItem(0, deck, false);
		vaultSlots.insertItem(1, new ItemStack(ECCatalogue.decks().get(3).get()), false);
		vaultSlots.insertItem(3, new ItemStack(ECCatalogue.decks().get(2).get()), false);

		EraDriveState drumBank = drive(level, EraDrive.DRUM_BANK);
		drumBank.getCellSlots().insertItem(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(0).get()), false);
		drumBank.getCellSlots().insertItem(1, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(3).get()), false);
		BlockPos bankPort = outsidePort(EraDrive.DRUM_BANK);
		for(int step = 0; step<=2; step++)
			placeCable(level, bankPort.north(step));
		for(int x = bankPort.getX()-1; x>outsidePort(EraDrive.DRUM_COMPUTER).getX(); x--)
			placeCable(level, new BlockPos(x, bankPort.getY(), bankPort.getZ()-2));

		EraDriveState drum = drive(level, ComputerEra.DRUM);
		ItemStack drumCell = new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).get(2).get());
		CellInventory.of(drumCell, () -> {
		}).insert(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 4000, false);
		drum.getCellSlots().insertItem(0, drumCell, false);

		overview(player);
	}

	private static void overview(ServerPlayer player)
	{
		player.teleportTo((ServerLevel)player.level(), base.getX()+24.5, base.getY()+3, base.getZ()+22, 180, 14);
	}

	private static void placeLights(ServerLevel level)
	{
		BlockPos first = driveOrigin(ComputerEra.PUNCHCARD);
		level.setBlockAndUpdate(first.offset(-1, 0, 2), Blocks.SEA_LANTERN.defaultBlockState());
		level.setBlockAndUpdate(driveOrigin(ComputerEra.TAPE).offset(1, 0, 3), Blocks.SEA_LANTERN.defaultBlockState());
	}

	private static BlockPos driveOrigin(ComputerEra era)
	{
		return driveOrigin(era.drives().get(0));
	}

	private static BlockPos driveOrigin(EraDrive drive)
	{
		return base.offset(drive.ordinal()*7, 0, -8);
	}

	private static BlockPos outsidePort(EraDrive drive)
	{
		NetworkPort port = drive.geometry().ports().get(0);
		return driveOrigin(drive).offset(port.pos()).relative(port.side());
	}

	private static BlockPos terminalPos(ComputerEra era)
	{
		return switch(era)
		{
			case PUNCHCARD -> outsidePort(EraDrive.PUNCHCARD_VAULT).north();
			case DRUM -> cableTerminalPos();
			case TAPE -> crossEraTerminalPos();
			case DISK -> outsidePort(EraDrive.DISK_MAINFRAME);
			case FLOPPY -> outsidePort(EraDrive.FLOPPY_WORKSTATION).north(2);
		};
	}

	private static EraDriveState drive(ServerLevel level, ComputerEra era)
	{
		return drive(level, era.drives().get(0));
	}

	private static EraDriveState drive(ServerLevel level, EraDrive drive)
	{
		BlockPos master = driveOrigin(drive).offset(drive.geometry().master());
		return (EraDriveState)((MultiblockPartBlockEntity)level.getBlockEntity(master)).getState();
	}

	private static void use(ServerLevel level, ServerPlayer player, BlockPos pos)
	{
		player.teleportTo(level, pos.getX()+0.5, base.getY(), pos.getZ()+2.5, 180, 30);
		level.getBlockState(pos).useWithoutItem(level, player,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
	}

	private static void onServer(Minecraft mc, SceneAction action, int waitAfter)
	{
		STEPS.add(new Step(waitAfter, () -> {
			ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
			ServerLevel level = (ServerLevel)player.level();
			mc.getSingleplayerServer().execute(() -> action.run(level, player));
		}));
	}

	@FunctionalInterface
	private interface SceneAction
	{
		void run(ServerLevel level, ServerPlayer player);
	}

	private static void screenshot(Minecraft mc, String name, int waitAfter)
	{
		STEPS.add(new Step(waitAfter, () -> {
			// Logged relative to the scene, so a moved camera can be told apart from a changed scene.
			if(mc.player!=null&&base!=null)
				EngineeredComputation.LOGGER.debug("Visual check: {} from ({}, {}, {}) facing {}/{}{}", name,
						mc.player.getX()-base.getX(), mc.player.getEyeY()-base.getY(), mc.player.getZ()-base.getZ(),
						mc.player.getYRot(), mc.player.getXRot(), mc.player.getAbilities().flying? " flying": "");
			Screenshot.grab(mc.gameDirectory, name+".png", mc.getMainRenderTarget(),
					message -> EngineeredComputation.LOGGER.info("Visual check: {}", message.getString()));
		}));
	}

	private static void queuePower(Minecraft mc)
	{
		TerminalMenu.Layout crafting = TerminalMenu.Layout.of(TerminalFeatures.CRAFTING);
		onServer(mc, (level, player) -> {
			ECConfig.POWER.set(true);
			BlockPos accepter = terminalPos(ComputerEra.DISK).above();
			if(!level.getBlockState(accepter).isAir())
				EngineeredComputation.LOGGER.error("Visual check: no room for the Energy Accepter at {}", accepter);
			level.setBlockAndUpdate(accepter, ECPower.BLOCK.get().defaultBlockState());
			player.teleportTo(level, accepter.getX()+0.5, accepter.getY()+0.4, accepter.getZ()-2.6, 0, 15);
		}, 20);
		screenshot(mc, "ec_power_accepter", 2);
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DISK)), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, -40, -40)));
		screenshot(mc, "ec_power_terminal_unpaid", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DISK).above()), 20);
		screenshot(mc, "ec_power_accepter_unpaid", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			BlockPos pos = terminalPos(ComputerEra.DISK).above();
			if(level.getBlockEntity(pos) instanceof EnergyAccepterBlockEntity accepter)
				accepter.setEnergy(80_000);
		}, 10);
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DISK).above()), 20);
		screenshot(mc, "ec_power_accepter_screen", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> use(level, player, terminalPos(ComputerEra.DISK)), 20);
		STEPS.add(new Step(5, () -> hoverGui(mc, 90, crafting.gridY()+crafting.rows()*18+5)));
		screenshot(mc, "ec_power_terminal_tooltip", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
		onServer(mc, (level, player) -> {
			ECConfig.POWER.set(false);
			level.setBlockAndUpdate(terminalPos(ComputerEra.DISK).above(), Blocks.AIR.defaultBlockState());
		}, 5);
	}

	private static void queueConfig(Minecraft mc)
	{
		ModContainer mod = ModList.get().getModContainerById(EngineeredComputation.MODID).orElseThrow();
		STEPS.add(new Step(10, () -> mc.setScreen(new ConfigurationScreen(mod, null))));
		screenshot(mc, "ec_config", 2);
		STEPS.add(new Step(5, () -> mc.setScreen(null)));
	}

	private static int guideLeft(Minecraft mc)
	{
		return (mc.getWindow().getGuiScaledWidth()-292)/2;
	}

	private static void hoverGui(Minecraft mc, int x, int y)
	{
		if(!(mc.screen instanceof AbstractContainerScreen<?> screen))
			return;
		hoverAbsolute(mc, screen.getGuiLeft()+x, screen.getGuiTop()+y);
	}

	private static void hoverAbsolute(Minecraft mc, int x, int y)
	{
		double toWindow = (double)mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth();
		setMouse(mc.mouseHandler, x*toWindow, y*toWindow);
	}

	private static void clickGui(Minecraft mc, int x, int y)
	{
		if(mc.screen instanceof AbstractContainerScreen<?> screen)
			screen.mouseClicked(screen.getGuiLeft()+x, screen.getGuiTop()+y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}

	private static void setMouse(MouseHandler mouse, double x, double y)
	{
		try
		{
			Field xpos = MouseHandler.class.getDeclaredField("xpos");
			Field ypos = MouseHandler.class.getDeclaredField("ypos");
			xpos.setAccessible(true);
			ypos.setAccessible(true);
			xpos.setDouble(mouse, x);
			ypos.setDouble(mouse, y);
		}
		catch(ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
