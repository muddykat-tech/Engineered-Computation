package com.muddykat.engineeredcomputation.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECDataCells;

@TestCategory({"multiblock", "storage"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CardVaultGameTests
{
	private CardVaultGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everyDeckSizeHasAThickerModel(GameTestHelper helper)
	{
		int tiers = ECCatalogue.DECK_TIERS;
		double previous = 0;
		for(int tier = 1; tier<=tiers; tier++)
		{
			Path json = asset("models/block/era1_card_deck_"+tier+".json");
			StorageGameTests.check(helper, Files.isRegularFile(json), "no deck model for deck tier "+tier);
			String model = JsonParser.parseString(read(json)).getAsJsonObject().get("model").getAsString();
			Path obj = asset(model.substring(model.indexOf(':')+1));
			StorageGameTests.check(helper, Files.isRegularFile(obj), "tier "+tier+"'s deck model has no OBJ: "+model);
			double thickness = thickness(read(obj));
			StorageGameTests.check(helper, thickness>previous,
					"tier "+tier+"'s deck should be thicker than tier "+(tier-1)+"'s ("+thickness+" <= "+previous+")");
			previous = thickness;
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void sneakUsingDecksFillsAndEmptiesEachCompartment(GameTestHelper helper)
	{
		Direction facing = Direction.EAST;
		EraDriveState vault = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		for(int slot = 0; slot<EraDrive.PUNCHCARD_VAULT.slots(); slot++)
		{
			AABB compartment = compartment(slot);
			Vec3 floor = new Vec3(compartment.getCenter().x, compartment.minY, compartment.getCenter().z);
			Vec3 above = floor.add(0, 2, 0);
			ItemStack deck = new ItemStack(ECCatalogue.decks().get(slot).get());
			player.setItemInHand(InteractionHand.MAIN_HAND, deck.copy());
			sneakUse(helper, player, above, floor, facing);
			StorageGameTests.check(helper, ItemStack.isSameItemSameComponents(vault.getCellSlots().getStackInSlot(slot),
					deck)&&player.getMainHandItem().isEmpty(), "a deck sneak-used into compartment "+slot+" didn't go in");
			sneakUse(helper, player, above, floor, facing);
			StorageGameTests.check(helper, vault.getCellSlots().getStackInSlot(slot).isEmpty()
							&&ItemStack.isSameItemSameComponents(player.getMainHandItem(), deck),
					"sneak-clicking compartment "+slot+" with an empty hand didn't take its deck");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theDeckInTheWayIsTheOneTaken(GameTestHelper helper)
	{
		Direction facing = Direction.NORTH;
		EraDriveState vault = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		ItemStack front = new ItemStack(ECCatalogue.decks().get(0).get());
		ItemStack behind = new ItemStack(ECCatalogue.decks().get(3).get());
		vault.getCellSlots().setStackInSlot(0, front.copy());
		vault.getCellSlots().setStackInSlot(1, behind.copy());

		Vec3[] sight = pastTheFrontDeck(helper);
		sneakUse(helper, player, sight[0], sight[1], facing);
		StorageGameTests.check(helper, ItemStack.isSameItemSameComponents(player.getMainHandItem(), front)
						&&vault.getCellSlots().getStackInSlot(0).isEmpty()
						&&ItemStack.isSameItemSameComponents(vault.getCellSlots().getStackInSlot(1), behind),
				"a click through the front deck took "+player.getMainHandItem()+" rather than the front deck");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aDeckInTheWayBlocksPuttingOneBehindIt(GameTestHelper helper)
	{
		Direction facing = Direction.NORTH;
		EraDriveState vault = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		vault.getCellSlots().setStackInSlot(0, new ItemStack(ECCatalogue.decks().get(0).get()));
		ItemStack deck = new ItemStack(ECCatalogue.decks().get(1).get());
		player.setItemInHand(InteractionHand.MAIN_HAND, deck.copy());

		Vec3[] sight = pastTheFrontDeck(helper);
		sneakUse(helper, player, sight[0], sight[1], facing);
		StorageGameTests.check(helper, vault.getCellSlots().getStackInSlot(1).isEmpty()
						&&ItemStack.isSameItemSameComponents(player.getMainHandItem(), deck),
				"a deck went into compartment 1 through the deck in front of it");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void onlyPunchCardsGoInTheSortingBox(GameTestHelper helper)
	{
		Direction facing = Direction.SOUTH;
		EraDriveState vault = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, facing);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(helper.getLevel());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).get(0).get()));
		AABB compartment = compartment(0);
		Vec3 floor = new Vec3(compartment.getCenter().x, compartment.minY, compartment.getCenter().z);
		sneakUse(helper, player, floor.add(0, 2, 0), floor, facing);
		StorageGameTests.check(helper, vault.getCellSlots().getStackInSlot(0).isEmpty()
				&&!player.getMainHandItem().isEmpty(), "a floppy went into the card vault's sorting box");
		helper.succeed();
	}

	private static AABB compartment(int slot)
	{
		return EraDrive.PUNCHCARD_VAULT.geometry().bayZones().get(slot);
	}

	private static Vec3[] pastTheFrontDeck(GameTestHelper helper)
	{
		AABB front = compartment(0);
		AABB behind = compartment(1);
		Vec3 eye = new Vec3(front.getCenter().x, front.maxY+0.03, EraDrive.PUNCHCARD_VAULT.geometry().size().getZ()+2);
		Vec3 hit = new Vec3(behind.getCenter().x, behind.minY, behind.getCenter().z);
		StorageGameTests.check(helper, front.clip(eye, hit).isPresent()&&!front.inflate(1/64.0).contains(hit),
				"test set-up: the line of sight should pass through compartment 0 and land beyond it");
		return new Vec3[]{eye, hit};
	}

	private static void sneakUse(GameTestHelper helper, StorageGameTests.MenuRecordingPlayer player, Vec3 eye,
			Vec3 target, Direction facing)
	{
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(StorageGameTests.ORIGIN);
		Vec3 worldEye = Vec3.atLowerCornerOf(origin).add(StorageGameTests.worldOffset(eye, facing));
		player.setPos(worldEye.subtract(0, player.getEyeHeight(), 0));
		player.setShiftKeyDown(true);
		BlockPos block = StorageGameTests.world(origin, BlockPos.containing(target), facing);
		BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(origin).add(
				StorageGameTests.worldOffset(target, facing)), Direction.UP, block, false);
		if(player.getMainHandItem().isEmpty())
			level.getBlockState(block).useWithoutItem(level, player, hit);
		else
			player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private static double thickness(String obj)
	{
		List<Double> zs = obj.lines().filter(line -> line.startsWith("v "))
				.map(line -> Double.parseDouble(line.trim().split("\\s+")[3])).toList();
		return zs.stream().mapToDouble(z -> z).max().orElse(0)-zs.stream().mapToDouble(z -> z).min().orElse(0);
	}

	private static Path asset(String path)
	{
		return ModList.get().getModFileById(EngineeredComputation.MODID).getFile()
				.findResource(("assets/"+EngineeredComputation.MODID+"/"+path).split("/"));
	}

	private static String read(Path path)
	{
		try
		{
			return Files.readString(path);
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
