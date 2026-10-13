package com.muddykat.engineeredcomputation.terminal;

import java.util.Optional;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.storage.StorageGameTests;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeBlockEntity;
import com.muddykat.engineeredcomputation.terminal.teletype.TeletypeMenu;

@TestCategory({"catalogue", "terminal"})
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class TeletypeCardGameTests
{
	private TeletypeCardGameTests()
	{
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aNameMatchesLikeTheTerminalSearch(GameTestHelper helper)
	{
		ResourceLocation iron = BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT);
		StorageGameTests.check(helper, ItemSearch.matches("Iron Ingot", iron, " iron IN "),
				"a name should match part of it, in any case");
		StorageGameTests.check(helper, ItemSearch.matches("Iron Ingot", iron, "@mine"),
				"@ should match the start of the mod id");
		StorageGameTests.check(helper, !ItemSearch.matches("Iron Ingot", iron, "gold"), "gold matched an iron ingot");
		StorageGameTests.check(helper, ItemSearch.matches("Iron Ingot", iron, ""), "an empty search should match all");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aNameFilterMatchesItemIdsWhereNamesArentKnown(GameTestHelper helper)
	{
		ProgramEntry iron = ProgramEntry.ofName("Iron");
		StorageGameTests.check(helper, iron!=null&&iron.matches(new ItemStack(Items.IRON_INGOT))
				&&iron.matches(new ItemStack(Items.IRON_ORE)), "\"Iron\" should match iron_ingot and iron_ore");
		StorageGameTests.check(helper, !iron.matches(new ItemStack(Items.GOLD_INGOT)), "\"Iron\" matched a gold ingot");
		ProgramEntry spaced = ProgramEntry.ofName("iron ingot");
		StorageGameTests.check(helper, spaced.matches(new ItemStack(Items.IRON_INGOT))
				&&!spaced.matches(new ItemStack(Items.IRON_ORE)), "\"iron ingot\" should match iron_ingot only");
		ProgramEntry mod = ProgramEntry.ofName("@engineered");
		StorageGameTests.check(helper, mod.matches(new ItemStack(ECCatalogue.FILTER_CARD.get()))
				&&!mod.matches(new ItemStack(Items.STONE)), "@engineered should match this mod's items only");
		StorageGameTests.check(helper, ProgramEntry.ofName("   ")==null, "a blank name made a filter");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aNameFilterIsSavedAndSent(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		ProgramEntry entry = ProgramEntry.ofName("cobble");
		var saved = ProgramEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry).getOrThrow();
		StorageGameTests.check(helper, ProgramEntry.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().equals(entry),
				"the name wasn't saved");
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
		ProgramEntry.STREAM_CODEC.encode(buf, entry);
		StorageGameTests.check(helper, ProgramEntry.STREAM_CODEC.decode(buf).equals(entry), "the name wasn't sent");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theCardSlotTakesOneFilterCard(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		var slot = setup.menu().getSlot(TeletypeMenu.CARD_SLOT);
		StorageGameTests.check(helper, slot.mayPlace(new ItemStack(ECCatalogue.FILTER_CARD.get()))
				&&!slot.mayPlace(new ItemStack(ECCatalogue.CARD.get()))&&!slot.mayPlace(new ItemStack(Items.PAPER)),
				"the card slot should take Filter Cards only");
		StorageGameTests.check(helper, slot.getMaxStackSize()==1, "the card slot should hold one card");
		StorageGameTests.check(helper, setup.menu().cardFilter().isEmpty(), "an empty slot filtered the view");
		setup.teletype(helper).cardContainer().setItem(0, FilterCardItem.of(ProgramEntry.ofName("iron")));
		StorageGameTests.check(helper, setup.menu().cardFilter().name().equals(Optional.of("iron")),
				"the menu doesn't see the teletype's card");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void shiftClickingACardFilesIt(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		ItemStack card = FilterCardItem.of(ProgramEntry.of(new ItemStack(Items.COBBLESTONE)));
		setup.player().getInventory().setItem(9, card);
		int inventoryIndex = 0;
		setup.menu().quickMoveStack(setup.player(), inventoryIndex);
		StorageGameTests.check(helper, setup.teletype(helper).getCard().getItem() instanceof FilterCardItem
				&&setup.player().getInventory().getItem(9).isEmpty(), "shift-clicking the card didn't put it in the slot");
		setup.menu().quickMoveStack(setup.player(), TeletypeMenu.CARD_SLOT);
		StorageGameTests.check(helper, setup.teletype(helper).getCard().isEmpty()
						&&setup.player().getInventory().countItem(ECCatalogue.FILTER_CARD.get())==1,
				"shift-clicking the slot didn't take the card back");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theCardIsEditedThroughTheTeletype(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		TeletypeMenu menu = setup.menu();
		menu.edit(0, ProgramEditPayload.Action.SET_NAME, "iron", ItemStack.EMPTY);
		StorageGameTests.check(helper, setup.teletype(helper).getCard().isEmpty(), "an edit without a card made one");
		setup.teletype(helper).cardContainer().setItem(0, new ItemStack(ECCatalogue.FILTER_CARD.get()));
		menu.edit(0, ProgramEditPayload.Action.SET_NAME, "  log ", ItemStack.EMPTY);
		StorageGameTests.check(helper, menu.cardFilter().name().equals(Optional.of("log")),
				"the teletype's card should filter by the name \"log\", not "+menu.cardFilter());
		menu.edit(0, ProgramEditPayload.Action.SET_NAME, "   ", ItemStack.EMPTY);
		StorageGameTests.check(helper, menu.cardFilter().name().equals(Optional.of("log")), "a blank name replaced it");
		menu.setCarried(new ItemStack(Items.COBBLESTONE, 5));
		menu.edit(0, ProgramEditPayload.Action.SET_ITEM, "", ItemStack.EMPTY);
		StorageGameTests.check(helper, menu.cardFilter().matches(new ItemStack(Items.COBBLESTONE))
				&&menu.getCarried().getCount()==5, "clicking the filter with cobblestone should set it, keeping it");
		menu.edit(0, ProgramEditPayload.Action.CLEAR, "", ItemStack.EMPTY);
		StorageGameTests.check(helper, menu.cardFilter().isEmpty()
				&&setup.teletype(helper).getCard().getItem() instanceof FilterCardItem, "clearing lost the card");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void rightClickingTheCardTakesItOut(GameTestHelper helper)
	{
		Setup setup = setup(helper);
		setup.teletype(helper).cardContainer().setItem(0, new ItemStack(ECCatalogue.FILTER_CARD.get()));
		setup.menu().clicked(TeletypeMenu.CARD_SLOT, 1, ClickType.PICKUP, setup.player());
		StorageGameTests.check(helper, setup.menu().getCarried().getItem() instanceof FilterCardItem
				&&setup.teletype(helper).getCard().isEmpty(),
				"right-clicking the card should take it out");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theCardIsSavedAndDropped(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper);
		TeletypeBlockEntity teletype = setup.teletype(helper);
		teletype.cardContainer().setItem(0, FilterCardItem.of(ProgramEntry.ofName("iron")));
		var registries = level.registryAccess();
		TeletypeBlockEntity loaded = new TeletypeBlockEntity(teletype.getBlockPos(), teletype.getBlockState());
		loaded.loadWithComponents(teletype.saveWithoutMetadata(registries), registries);
		StorageGameTests.check(helper, FilterCardItem.filter(loaded.getCard()).name().equals(Optional.of("iron")),
				"the teletype's card wasn't saved");
		level.removeBlock(setup.pos(), false);
		boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(setup.pos()).inflate(2)).stream()
				.anyMatch(drop -> drop.getItem().getItem() instanceof FilterCardItem);
		StorageGameTests.check(helper, dropped, "breaking the teletype didn't drop its card");
		helper.succeed();
	}

	private record Setup(BlockPos pos, TeletypeMenu menu, StorageGameTests.MenuRecordingPlayer player)
	{
		TeletypeBlockEntity teletype(GameTestHelper helper)
		{
			return (TeletypeBlockEntity)helper.getLevel().getBlockEntity(pos);
		}
	}

	private static Setup setup(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(StorageGameTests.ORIGIN);
		level.setBlockAndUpdate(pos, StorageGameTests.terminal(ComputerEra.PUNCHCARD));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
		TeletypeMenu menu = new TeletypeMenu(1, player.getInventory(), pos);
		player.containerMenu = menu;
		return new Setup(pos, menu, player);
	}
}
