package com.muddykat.engineeredcomputation.compat.ponder.scenes;

import java.util.Collection;
import java.util.List;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.compat.ponder.CarrierElement;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.pneumatic.pressurizer.PressurizerBlock;
import com.muddykat.engineeredcomputation.pneumatic.tube.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.registration.ECContent;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

/** Era 1: the Tabulator Machine, its Teletype and the Tube Stations it catalogues. */
public final class TabulatorScenes
{
	private TabulatorScenes()
	{
	}

	/**
	 * Starts from ponder/era1/tabulator_setup.nbt, the finished setup with a punched card in the Tabulator's last bay;
	 * the scene takes it back to bare casings and builds it up again.
	 */
	public static void setup(SceneBuilder scene, SceneBuildingUtil util)
	{
		scene.title("tabulator_setup", "Setting up a Tabulator Machine");
		scene.showBasePlate();
		scene.idle(10);

		Selection tabulator = util.select().fromTo(2, 1, 2, 4, 2, 3)
				.add(util.select().position(3, 3, 3))
				.add(util.select().position(4, 3, 3));
		BlockPos front = util.grid().at(3, 1, 2);
		BlockPos master = util.grid().at(3, 1, 3);
		BlockPos stationChest = util.grid().at(1, 1, 2);
		BlockPos station = util.grid().at(1, 1, 3);
		BlockPos outletChest = util.grid().at(5, 1, 2);
		BlockPos outlet = util.grid().at(5, 1, 3);
		BlockPos teletype = util.grid().at(5, 2, 3);
		Selection tubes = util.select().fromTo(1, 1, 4, 5, 1, 4);

		scene.world().setBlocks(tabulator, ECContent.casing(ComputerEra.PUNCHCARD).get().defaultBlockState(), false);
		scene.world().showSection(tabulator, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("Build the Tabulator Machine from Analogue Casings in this shape")
				.pointAt(util.vector().topOf(3, 3, 3))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showText(60)
				.text("Right-click the front with the Assembly Wrench to form it")
				.pointAt(util.vector().blockSurface(front, Direction.NORTH))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().blockSurface(front, Direction.NORTH), Pointing.DOWN, 30)
				.rightClick()
				.withItem(new ItemStack(ECContent.ASSEMBLY_WRENCH.get()));
		scene.idle(10);
		restoreDrawn(scene, tabulator);
		Tag[] deck = new Tag[1];
		scene.world().modifyBlockEntityNBT(util.select().position(master), MultiblockPartBlockEntity.class, tag -> {
			ListTag bays = bays(tag);
			deck[0] = bays.copy();
			bays.clear();
		});
		scene.effects().indicateSuccess(front);
		scene.idle(50);

		scene.world().showSection(util.select().position(stationChest), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(station), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70)
				.text("Place a Tube Station against an inventory, with its hatch facing it")
				.pointAt(util.vector().topOf(station))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(tubes, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("Connect the stations to the Tabulator Machine with Pneumatic Tubes")
				.pointAt(util.vector().topOf(3, 1, 4))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(outletChest), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(outlet), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(60)
				.text("Requested items are sent to a Receiving Station")
				.pointAt(util.vector().topOf(outlet))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(70);

		scene.world().showSection(util.select().position(teletype), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("Place a Teletype Terminal against a Receiving Station to request items from the network")
				.pointAt(util.vector().topOf(teletype))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 40)
				.rightClick()
				.withItem(new ItemStack(ECCatalogue.CARD.get()));
		scene.overlay().showText(70)
				.text("To punch a Catalogue Card with a station's location, right-click the Tube Station with it")
				.pointAt(util.vector().topOf(station))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showText(70)
				.text("File punched cards in a Card Deck, then place the deck in the Tabulator Machine")
				.pointAt(util.vector().topOf(2, 2, 3))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(2, 2, 3), Pointing.DOWN, 30)
				.rightClick()
				.withItem(new ItemStack(ECCatalogue.decks().getFirst().get()));
		scene.idle(10);
		scene.world().modifyBlockEntityNBT(util.select().position(master), MultiblockPartBlockEntity.class,
				tag -> bays(tag).addAll(((ListTag)deck[0]).copy()));
		scene.idle(60);

		scene.overlay().showText(80)
				.text("The station's items can now be requested from the Teletype Terminal")
				.pointAt(util.vector().topOf(teletype))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(90);
	}

	/**
	 * Starts from ponder/era1/tabulator_usage.nbt, the finished network after its second, pressurised request; the
	 * scene takes it back to plain tubes, an empty bay and no Pressurizer, and sends the barrel's coal twice.
	 */
	public static void usage(SceneBuilder scene, SceneBuildingUtil util)
	{
		scene.title("tabulator_usage", "Sending items between stations");
		showNetworkPlate(scene);

		Selection windows = util.select().fromTo(2, 2, 4, 2, 4, 4).add(util.select().fromTo(3, 4, 4, 6, 4, 4));
		Selection wall = util.select().fromTo(4, 1, 2, 4, 3, 4);
		Selection wallTop = util.select().fromTo(4, 4, 2, 4, 4, 3);
		ItemStack coal = new ItemStack(Items.COAL, 64);

		scene.world().modifyBlock(COVERED, state -> sameProperties(state, ECPneumatics.TUBE.get()), false);
		scene.world().modifyBlocks(windows, state -> state.setValue(PneumaticTubeBlock.WINDOWED, false), false);
		scene.world().modifyBlock(HOME_TUBE, state -> joined(state, Direction.WEST, false), false);
		scene.world().modifyBlock(PRESSURIZER, state -> state.setValue(PressurizerBlock.LIT, false), false);
		Tag[] deck = emptyBay(scene, util);

		scene.world().showSection(tabulator(util).add(tubes(util)), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(STATION), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(HOME), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(OUTPOST), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("A tube network can have several Receiving Stations")
				.pointAt(util.vector().topOf(OUTPOST))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(STATION_BARREL), Direction.DOWN);
		scene.idle(10);
		refillBay(scene, util, deck);
		scene.idle(30);

		scene.overlay().showText(70)
				.text("To add a window to a tube, sneak while right-clicking it with an empty hand")
				.pointAt(util.vector().blockSurface(util.grid().at(2, 3, 4), Direction.NORTH))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		for(BlockPos tube : List.of(util.grid().at(2, 2, 4), util.grid().at(2, 3, 4), util.grid().at(2, 4, 4),
				util.grid().at(3, 4, 4), util.grid().at(4, 4, 4), util.grid().at(5, 4, 4), util.grid().at(6, 4, 4)))
		{
			scene.overlay().showControls(util.vector().blockSurface(tube, Direction.NORTH), Pointing.DOWN, 8)
					.rightClick()
					.whileSneaking();
			scene.idle(4);
			scene.world().modifyBlock(tube, state -> state.setValue(PneumaticTubeBlock.WINDOWED, true), false);
			scene.idle(6);
		}
		scene.idle(30);

		scene.world().showSection(wall, Direction.DOWN);
		scene.idle(20);
		scene.overlay().showText(70)
				.text("To hide a tube, sneak while placing a block on it to cover it")
				.pointAt(util.vector().blockSurface(COVERED, Direction.NORTH))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().blockSurface(COVERED, Direction.NORTH), Pointing.DOWN, 30)
				.rightClick()
				.whileSneaking()
				.withItem(new ItemStack(Items.OAK_PLANKS));
		scene.idle(10);
		scene.world().restoreBlocks(util.select().position(COVERED));
		scene.idle(20);
		scene.world().showSection(wallTop, Direction.DOWN);
		scene.idle(30);

		scene.overlay().showText(60)
				.text("Right-click a Receiving Station to name it")
				.pointAt(util.vector().topOf(OUTPOST))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(OUTPOST), Pointing.DOWN, 30).rightClick();
		scene.idle(55);

		scene.world().showSection(util.select().position(HOME_CHEST).add(util.select().position(OUTPOST_CHEST)),
				Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(HOME_TELETYPE).add(util.select().position(OUTPOST_TELETYPE)),
				Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("Each Receiving Station can have its own Teletype Terminal")
				.pointAt(util.vector().topOf(OUTPOST_TELETYPE))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(STATION_BARREL), Pointing.DOWN, 50).withItem(coal);
		scene.overlay().showText(80)
				.text("To send items to another Receiving Station, choose it under To: on the Teletype Terminal's request slip")
				.pointAt(util.vector().topOf(HOME_TELETYPE))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(HOME_TELETYPE), Pointing.DOWN, 30).rightClick();
		scene.idle(40);
		int lowItems = ECConfig.carrierItems(false);
		scene.overlay().showText(80)
				.text("Without a Pressurizer, each carrier holds %1$s items", lowItems)
				.pointAt(util.vector().topOf(5, 4, 4))
				.placeNearTarget();
		CarrierElement.send(scene, carriersFor(coal, lowItems), ECConfig.carrierTicks(false), STATION,
				util.grid().at(11, 1, 4), util.grid().at(11, 4, 4), util.grid().at(2, 4, 4), util.grid().at(2, 1, 4),
				OUTPOST);
		scene.overlay().showControls(util.vector().topOf(OUTPOST_CHEST), Pointing.DOWN, 50).withItem(coal);
		scene.idle(60);

		scene.world().restoreBlocks(util.select().position(HOME_TUBE));
		scene.world().showSection(util.select().position(PRESSURIZER_TUBE).add(util.select().position(PRESSURIZER)),
				Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
				.text("A Pressurizer anywhere on the network raises it to high pressure while it has fuel")
				.pointAt(util.vector().topOf(PRESSURIZER))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(30);
		scene.overlay().showControls(util.vector().topOf(PRESSURIZER), Pointing.DOWN, 30)
				.rightClick()
				.withItem(new ItemStack(Items.COAL));
		scene.idle(10);
		scene.world().modifyBlock(PRESSURIZER, state -> state.setValue(PressurizerBlock.LIT, true), false);
		scene.idle(50);

		scene.overlay().showControls(util.vector().topOf(STATION_BARREL), Pointing.DOWN, 40).withItem(coal);
		scene.overlay().showControls(util.vector().topOf(HOME_TELETYPE), Pointing.DOWN, 30).rightClick();
		scene.idle(30);
		int highItems = ECConfig.carrierItems(true);
		scene.overlay().showText(70)
				.text("At high pressure, each carrier holds %1$s items", highItems)
				.pointAt(util.vector().topOf(5, 4, 4))
				.placeNearTarget()
				.attachKeyFrame();
		CarrierElement.send(scene, carriersFor(coal, highItems), ECConfig.carrierTicks(true), STATION,
				util.grid().at(11, 1, 4), util.grid().at(11, 4, 4), util.grid().at(2, 4, 4), util.grid().at(2, 1, 4),
				OUTPOST);
		scene.overlay().showControls(util.vector().topOf(OUTPOST_CHEST), Pointing.DOWN, 50).withItem(coal);
		scene.idle(60);
	}

	/**
	 * Starts from ponder/era1/tabulator_autosmelting.nbt, the usage scene's network with the Outpost swapped for an
	 * Observing Autotube and a blast furnace fed from a "Melter" Receiving Station, after its iron has come back.
	 */
	public static void autosmelting(SceneBuilder scene, SceneBuildingUtil util)
	{
		scene.title("tabulator_autosmelting", "Smelting through the tube network");
		showNetworkPlate(scene);

		BlockPos results = util.grid().at(11, 2, 3);
		BlockPos resultsChest = util.grid().at(11, 2, 2);
		BlockPos resultsTube = util.grid().at(11, 2, 4);
		BlockPos melter = util.grid().at(3, 4, 3);
		BlockPos melterChest = util.grid().at(3, 4, 2);
		BlockPos melterTube = util.grid().at(3, 4, 4);
		BlockPos feeder = util.grid().at(3, 3, 2);
		BlockPos furnace = util.grid().at(3, 2, 2);
		BlockPos hopper = util.grid().at(3, 1, 2);
		BlockPos returner = OUTPOST;
		BlockPos returnerTube = util.grid().at(2, 1, 4);
		ItemStack rawIron = new ItemStack(Items.RAW_IRON, 64);

		scene.world().modifyBlock(resultsTube, state -> joined(state, Direction.NORTH, false)
				.setValue(PneumaticTubeBlock.STRAIGHT, PneumaticTubeBlock.Straight.Y), false);
		scene.world().modifyBlock(melterTube, state -> joined(state, Direction.NORTH, false)
				.setValue(PneumaticTubeBlock.STRAIGHT, PneumaticTubeBlock.Straight.X), false);
		scene.world().modifyBlock(returnerTube, state -> joined(state, Direction.NORTH, false), false);

		scene.world().showSection(tabulator(util).add(tubes(util))
				.add(util.select().fromTo(4, 1, 2, 4, 4, 4))
				.add(util.select().position(STATION)).add(util.select().position(STATION_BARREL))
				.add(util.select().position(HOME)).add(util.select().position(HOME_CHEST))
				.add(util.select().position(HOME_TELETYPE))
				.add(util.select().position(PRESSURIZER)).add(util.select().position(PRESSURIZER_TUBE))
				.add(util.select().position(OUTPOST_CHEST)), Direction.DOWN);
		scene.idle(20);
		scene.overlay().showText(70)
				.text("Observing Autotubes can run a furnace from the tube network")
				.pointAt(util.vector().topOf(OUTPOST_CHEST))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.world().restoreBlocks(util.select().position(resultsTube));
		scene.world().showSection(util.select().position(resultsChest).add(util.select().position(results)),
				Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(resultsChest), Pointing.DOWN, 60).withItem(rawIron);
		scene.overlay().showText(70)
				.text("Right-click a Tube Station to set it to take new items")
				.pointAt(util.vector().topOf(results))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(results), Pointing.DOWN, 30).rightClick();
		scene.idle(65);

		scene.world().restoreBlocks(util.select().position(melterTube));
		scene.world().showSection(util.select().position(melter), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(melterChest), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("Set to eject, a Receiving Station puts what it receives into the inventory in front of it")
				.pointAt(util.vector().topOf(melterChest))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(furnace), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(feeder), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
				.text("An Observing Autotube pulls items whenever the inventory at its intake changes")
				.pointAt(util.vector().blockSurface(feeder, Direction.NORTH))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(80);
		scene.overlay().showText(80)
				.text("Before items are sent to it, sneak while right-clicking it to set how many it pulls at once")
				.pointAt(util.vector().blockSurface(feeder, Direction.NORTH))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().blockSurface(feeder, Direction.NORTH), Pointing.DOWN, 30)
				.rightClick()
				.whileSneaking();
		scene.idle(70);

		scene.world().showSection(util.select().position(hopper), Direction.DOWN);
		scene.idle(10);
		scene.world().restoreBlocks(util.select().position(returnerTube));
		scene.world().showSection(util.select().position(returner), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
				.text("A second Observing Autotube sends each smelted item to a Tube Station holding it, or one that takes new items")
				.pointAt(util.vector().topOf(returner))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showText(70)
				.text("Request the raw iron to the Melter from the Teletype Terminal")
				.pointAt(util.vector().topOf(HOME_TELETYPE))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(HOME_TELETYPE), Pointing.DOWN, 30).rightClick();
		scene.idle(40);
		CarrierElement.send(scene, carriersFor(rawIron, ECConfig.carrierItems(true)), ECConfig.carrierTicks(true),
				results, resultsTube, util.grid().at(11, 4, 4), melterTube, melter);
		scene.overlay().showControls(util.vector().topOf(melterChest), Pointing.DOWN, 40).withItem(rawIron);
		scene.idle(10);
		scene.world().modifyBlock(furnace, state -> state.setValue(AbstractFurnaceBlock.LIT, true), false);
		scene.idle(40);

		scene.overlay().showText(90)
				.text("Over time, each iron ingot is sent back to the results station")
				.pointAt(util.vector().topOf(5, 4, 4))
				.placeNearTarget()
				.attachKeyFrame();
		CarrierElement.send(scene, 6, 15, returner, returnerTube, util.grid().at(2, 4, 4), util.grid().at(11, 4, 4),
				resultsTube, results);
		scene.world().modifyBlock(furnace, state -> state.setValue(AbstractFurnaceBlock.LIT, false), false);
		scene.overlay().showControls(util.vector().topOf(resultsChest), Pointing.DOWN, 60)
				.withItem(new ItemStack(Items.IRON_INGOT, 64));
		scene.idle(70);
	}

	// The usage and autosmelting structures share one layout: the Tabulator, a coal station, Homebase and the Outpost
	// on a lower run, the stations' risers and a windowed overhead run through a plank wall.
	private static final BlockPos STATION = new BlockPos(11, 1, 3);
	private static final BlockPos STATION_BARREL = new BlockPos(11, 1, 2);
	private static final BlockPos HOME = new BlockPos(7, 1, 3);
	private static final BlockPos HOME_CHEST = new BlockPos(7, 1, 2);
	private static final BlockPos HOME_TELETYPE = new BlockPos(7, 2, 3);
	private static final BlockPos HOME_TUBE = new BlockPos(7, 1, 4);
	private static final BlockPos OUTPOST = new BlockPos(2, 1, 3);
	private static final BlockPos OUTPOST_CHEST = new BlockPos(2, 1, 2);
	private static final BlockPos OUTPOST_TELETYPE = new BlockPos(2, 2, 3);
	private static final BlockPos PRESSURIZER = new BlockPos(6, 1, 3);
	private static final BlockPos PRESSURIZER_TUBE = new BlockPos(6, 1, 4);
	private static final BlockPos COVERED = new BlockPos(4, 4, 4);
	private static final BlockPos TABULATOR_MASTER = new BlockPos(9, 1, 3);

	/** The plate is 14 by 8, so the view is centred on a 14 wide square around it. */
	private static void showNetworkPlate(SceneBuilder scene)
	{
		scene.configureBasePlate(0, -3, 14);
		scene.scaleSceneView(0.65f);
		scene.showBasePlate();
		scene.idle(10);
	}

	private static Selection tabulator(SceneBuildingUtil util)
	{
		return util.select().fromTo(8, 1, 2, 10, 2, 3)
				.add(util.select().position(9, 3, 3))
				.add(util.select().position(10, 3, 3));
	}

	/** Every tube but the Pressurizer's. */
	private static Selection tubes(SceneBuildingUtil util)
	{
		return util.select().fromTo(7, 1, 4, 11, 1, 4)
				.add(util.select().fromTo(11, 2, 4, 11, 4, 4))
				.add(util.select().fromTo(2, 4, 4, 10, 4, 4))
				.add(util.select().fromTo(2, 1, 4, 2, 3, 4));
	}

	private static Tag[] emptyBay(SceneBuilder scene, SceneBuildingUtil util)
	{
		Tag[] deck = new Tag[1];
		scene.world().modifyBlockEntityNBT(util.select().position(TABULATOR_MASTER), MultiblockPartBlockEntity.class,
				tag -> {
					ListTag bays = bays(tag);
					deck[0] = bays.copy();
					bays.clear();
				});
		return deck;
	}

	private static void refillBay(SceneBuilder scene, SceneBuildingUtil util, Tag[] deck)
	{
		scene.overlay().showControls(util.vector().topOf(8, 2, 3), Pointing.DOWN, 30)
				.rightClick()
				.withItem(new ItemStack(ECCatalogue.decks().getFirst().get()));
		scene.idle(10);
		scene.world().modifyBlockEntityNBT(util.select().position(TABULATOR_MASTER), MultiblockPartBlockEntity.class,
				tag -> bays(tag).addAll(((ListTag)deck[0]).copy()));
	}

	private static BlockState joined(BlockState tube, Direction side, boolean joined)
	{
		return tube.setValue(PneumaticTubeBlock.CONNECTIONS.get(side), joined);
	}

	private static int carriersFor(ItemStack stack, int perCarrier)
	{
		return Math.ceilDiv(stack.getCount(), perCarrier);
	}

	/** The same block with every property the two share copied across, as a covered tube keeps its tube's. */
	private static BlockState sameProperties(BlockState from, Block to)
	{
		BlockState state = to.defaultBlockState();
		for(Property<?> property : from.getProperties())
			if(state.hasProperty(property))
				state = copy(from, state, property);
		return state;
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property)
	{
		return to.setValue(property, from.getValue(property));
	}

	private static ListTag bays(CompoundTag partTag)
	{
		return partTag.getCompound("state").getCompound("cells").getList("Items", Tag.TAG_COMPOUND);
	}

	/** Ponder's restoreBlocks brings block entities back without drawing them, which would leave the bays empty. */
	private static void restoreDrawn(SceneBuilder scene, Selection selection)
	{
		scene.world().restoreBlocks(selection);
		scene.addInstruction(ponder -> {
			PonderLevel level = ponder.getWorld();
			if(!(level.getRenderedBlockEntities() instanceof Collection<BlockEntity> rendered))
				return;
			for(BlockPos pos : selection)
			{
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if(blockEntity!=null&&!rendered.contains(blockEntity))
					rendered.add(blockEntity);
			}
		});
	}
}
