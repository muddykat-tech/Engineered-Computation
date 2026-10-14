package com.muddykat.engineeredcomputation.storage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.drive.EraDriveState;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.registration.ECSounds;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;

@TestCategory("storage")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = EngineeredComputation.MODID)
public final class CellBaySoundGameTests
{
	/** Every sound the server plays; each test only looks at the ones inside its own area. */
	private static final List<Heard> HEARD = new ArrayList<>();

	private CellBaySoundGameTests()
	{
	}

	@SubscribeEvent
	public static void onSound(PlayLevelSoundEvent.AtPosition event)
	{
		if(!event.getLevel().isClientSide&&event.getSound()!=null)
			synchronized(HEARD)
			{
				HEARD.add(new Heard(event.getSound().value(), event.getPosition()));
			}
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void bigFloppyDrivesSoundTheirDisks(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDriveAt(helper, EraDrive.FLOPPY_WORKSTATION, Direction.NORTH,
				StorageGameTests.ORIGIN);
		ItemStackHandler slots = drive.getCellSlots();
		slots.setStackInSlot(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).getFirst().get()));
		expect(helper, ECSounds.FLOPPY_525_INSERT.get(), "putting a 5.25-inch disk in a drive");
		slots.setStackInSlot(0, ItemStack.EMPTY);
		expect(helper, ECSounds.FLOPPY_525_EJECT.get(), "taking a 5.25-inch disk out of a drive");

		slots.setStackInSlot(1, new ItemStack(ECDataCells.cellsOf(ComputerEra.FLOPPY).getFirst().get()));
		heard(helper);
		helper.getLevel().destroyBlock(helper.absolutePos(StorageGameTests.ORIGIN), false);
		check(helper, heard(helper).isEmpty(), "breaking a drive played its disks' eject sounds");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void smallFloppyBaysSoundTheirDisks(GameTestHelper helper)
	{
		EraDriveState bay = StorageGameTests.formDriveAt(helper, EraDrive.COMPACT_FLOPPY_BAY, Direction.NORTH,
				StorageGameTests.ORIGIN);
		bay.getCellSlots().setStackInSlot(2, new ItemStack(ECDataCells.terminalDisks().getFirst().get()));
		expect(helper, ECSounds.FLOPPY_35_INSERT.get(), "putting a 3.5-inch disk in a compact bay");
		bay.getCellSlots().setStackInSlot(2, ItemStack.EMPTY);
		expect(helper, ECSounds.FLOPPY_35_EJECT.get(), "taking a 3.5-inch disk out of a compact bay");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void floppyTerminalSoundsItsDisks(GameTestHelper helper)
	{
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, StorageGameTests.terminal(ComputerEra.FLOPPY)
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		TerminalDiskBlockEntity bays = (TerminalDiskBlockEntity)helper.getBlockEntity(pos);
		bays.getDisks().setStackInSlot(0, new ItemStack(ECDataCells.terminalDisks().getFirst().get()));
		expect(helper, ECSounds.FLOPPY_35_INSERT.get(), "putting a disk in the floppy terminal");
		bays.getDisks().setStackInSlot(0, ItemStack.EMPTY);
		expect(helper, ECSounds.FLOPPY_35_EJECT.get(), "taking a disk out of the floppy terminal");

		bays.getDisks().setStackInSlot(1, new ItemStack(ECDataCells.terminalDisks().getFirst().get()));
		heard(helper);
		helper.destroyBlock(pos);
		check(helper, heard(helper).isEmpty(), "breaking the floppy terminal played its disks' eject sounds");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void otherCellsAreSilent(GameTestHelper helper)
	{
		EraDriveState drive = StorageGameTests.formDrive(helper, ComputerEra.DRUM, Direction.NORTH);
		drive.getCellSlots().setStackInSlot(0, new ItemStack(ECDataCells.cellsOf(ComputerEra.DRUM).getFirst().get()));
		drive.getCellSlots().setStackInSlot(0, ItemStack.EMPTY);
		check(helper, heard(helper).isEmpty(), "a drum cell played a sound");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void everySoundFileExists(GameTestHelper helper) throws Exception
	{
		String root = "/assets/"+EngineeredComputation.MODID+"/";
		JsonObject sounds;
		try(InputStream in = CellBaySoundGameTests.class.getResourceAsStream(root+"sounds.json"))
		{
			check(helper, in!=null, "sounds.json is missing");
			sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		for(Map.Entry<String, JsonElement> event : sounds.entrySet())
			for(JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds"))
			{
				String name = sound.getAsJsonObject().get("name").getAsString();
				String file = root+"sounds/"+name.substring(name.indexOf(':')+1)+".ogg";
				check(helper, CellBaySoundGameTests.class.getResource(file)!=null, event.getKey()+" needs "+file);
			}
		check(helper, sounds.has("block.floppy_35.insert")&&sounds.has("block.floppy_525.eject"),
				"sounds.json is missing the floppy sounds");
		helper.succeed();
	}

	private static void expect(GameTestHelper helper, SoundEvent sound, String action)
	{
		List<SoundEvent> heard = heard(helper);
		check(helper, heard.equals(List.of(sound)), action+" should play "+sound.getLocation()+", played "+heard);
	}

	/** The sounds played inside this test's area since the last call. */
	private static List<SoundEvent> heard(GameTestHelper helper)
	{
		AABB area = helper.getBounds();
		List<SoundEvent> heard = new ArrayList<>();
		synchronized(HEARD)
		{
			HEARD.removeIf(entry -> {
				if(!area.contains(entry.at))
					return false;
				heard.add(entry.sound);
				return true;
			});
		}
		return heard;
	}

	private static void check(GameTestHelper helper, boolean condition, String failure)
	{
		StorageGameTests.check(helper, condition, failure);
	}

	private record Heard(SoundEvent sound, Vec3 at)
	{
	}
}
