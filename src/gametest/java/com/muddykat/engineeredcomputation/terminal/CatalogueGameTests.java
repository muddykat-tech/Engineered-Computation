package com.muddykat.engineeredcomputation.terminal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.catalogue.CardDeck;
import com.muddykat.engineeredcomputation.catalogue.CardDeckItem;
import com.muddykat.engineeredcomputation.catalogue.CardDeckMenu;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCard;
import com.muddykat.engineeredcomputation.catalogue.CatalogueCardItem;
import com.muddykat.engineeredcomputation.catalogue.CatalogueDesk;
import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.gametest.TestCategory;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.NetworkCableBlock;
import com.muddykat.engineeredcomputation.network.NetworkMemberBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.ReceivingStationBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.TubeMemberBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlock;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECCables;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;
import com.muddykat.engineeredcomputation.storage.CellContents;
import com.muddykat.engineeredcomputation.storage.ItemKey;

@TestCategory("catalogue")
@GameTestHolder(EngineeredComputation.MODID)
@PrefixGameTestTemplate(false)
public final class CatalogueGameTests
{
	private static final ItemKey DIAMOND = ItemKey.of(new ItemStack(Items.DIAMOND));

	private CatalogueGameTests()
	{
	}

	private record Setup(EraDriveState catalogue, BlockPos receiver, BlockPos tube, BlockPos station, BlockPos chest,
			BlockPos teletype)
	{
		ChestBlockEntity chestEntity(ServerLevel level)
		{
			return (ChestBlockEntity)level.getBlockEntity(chest);
		}

		ReceivingStationBlockEntity receiverEntity(ServerLevel level)
		{
			return (ReceivingStationBlockEntity)level.getBlockEntity(receiver);
		}

		TubeStationBlockEntity stationEntity(ServerLevel level)
		{
			return (TubeStationBlockEntity)level.getBlockEntity(station);
		}

		CatalogueCard card(ServerLevel level)
		{
			return new CatalogueCard(GlobalPos.of(level.dimension(), station), Component.literal("Chest"),
					CellContents.EMPTY, CatalogueCard.NEVER);
		}

		void file(CatalogueCard... cards)
		{
			ItemStack deck = new ItemStack(ECCatalogue.decks().get(0).get());
			CardDeck filed = CardDeck.EMPTY;
			for(CatalogueCard card : cards)
				filed = filed.with(card);
			CardDeckItem.setDeck(deck, filed);
			catalogue.getCellSlots().setStackInSlot(0, deck);
		}

		CatalogueCard filedCard()
		{
			return CardDeckItem.deck(catalogue.getCellSlots().getStackInSlot(0)).cards().get(0);
		}
	}

	private static Setup setup(GameTestHelper helper, int diamonds)
	{
		ServerLevel level = helper.getLevel();
		EraDriveState catalogue = StorageGameTests.formDrive(helper, ComputerEra.PUNCHCARD, Direction.NORTH);
		BlockPos receiver = StorageGameTests.atPort(helper, ComputerEra.PUNCHCARD, Direction.NORTH);
		Direction out = Arrays.stream(Direction.values()).filter(d -> d.getAxis().isHorizontal()
				&&level.getBlockState(receiver.relative(d.getOpposite())).getBlock()!=Blocks.AIR).findFirst().orElseThrow();
		level.setBlockAndUpdate(receiver, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		BlockPos tube = receiver.above();
		level.setBlockAndUpdate(tube, ECPneumatics.TUBE.get().defaultBlockState());
		BlockPos station = tube.above();
		BlockPos chest = station.above();
		level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(station, ECPneumatics.TUBE_STATION.get().defaultBlockState()
				.setValue(TubeStationBlock.FACING, Direction.UP));
		BlockPos teletype = receiver.relative(out);
		level.setBlockAndUpdate(teletype, StorageGameTests.terminal(ComputerEra.PUNCHCARD));
		if(diamonds>0)
			((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, diamonds));
		return new Setup(catalogue, receiver, tube, station, chest, teletype);
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aBlankCardUsedOnAStationIsPunchedWithIt(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECCatalogue.CARD.get(), 3));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(setup.station()), Direction.NORTH, setup.station(), false)));

		StorageGameTests.check(helper, player.getMainHandItem().getCount()==2, "one blank card should be used");
		ItemStack punched = ItemStack.EMPTY;
		for(ItemStack stack : player.getInventory().items)
			if(CatalogueCardItem.card(stack)!=null)
				punched = stack;
		CatalogueCard card = CatalogueCardItem.card(punched);
		StorageGameTests.check(helper, card!=null, "no punched card came back");
		StorageGameTests.check(helper, card.station().pos().equals(setup.station())
				&&card.station().dimension().equals(level.dimension()), "card punched with the wrong station");
		StorageGameTests.check(helper, card.label().getString().equals(Blocks.CHEST.getName().getString()),
				"card should be labelled with the chest's name, got "+card.label().getString());
		StorageGameTests.check(helper, card.tally().totalCount()==5, "card should have tallied the chest's 5 diamonds");
		StorageGameTests.check(helper, punched.getMaxStackSize()==1, "punched cards shouldn't stack");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void decksFileCardsUpToTheirSize(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		ItemStack deck = new ItemStack(ECCatalogue.decks().get(0).get());
		CardDeckItem item = (CardDeckItem)deck.getItem();
		Slot slot = new Slot(new SimpleContainer(1), 0, 0, 0);
		slot.set(deck);
		CatalogueCard card = new CatalogueCard(GlobalPos.of(level.dimension(), BlockPos.ZERO), Component.literal("Chest"),
				CellContents.EMPTY, CatalogueCard.NEVER);
		ItemStack[] cursor = {ItemStack.EMPTY};
		SlotAccess carried = new SlotAccess()
		{
			@Override
			public ItemStack get()
			{
				return cursor[0];
			}

			@Override
			public boolean set(ItemStack stack)
			{
				cursor[0] = stack;
				return true;
			}
		};

		for(int i = 0; i<item.getCapacity(); i++)
		{
			cursor[0] = CatalogueCardItem.punched(card);
			item.overrideOtherStackedOnMe(deck, cursor[0], slot, ClickAction.SECONDARY, player, carried);
			StorageGameTests.check(helper, cursor[0].isEmpty(), "card "+(i+1)+" wasn't filed");
		}
		cursor[0] = CatalogueCardItem.punched(card);
		item.overrideOtherStackedOnMe(deck, cursor[0], slot, ClickAction.SECONDARY, player, carried);
		StorageGameTests.check(helper, !cursor[0].isEmpty()&&CardDeckItem.deck(deck).size()==item.getCapacity(),
				"a full deck took another card");

		cursor[0] = ItemStack.EMPTY;
		item.overrideOtherStackedOnMe(deck, ItemStack.EMPTY, slot, ClickAction.SECONDARY, player, carried);
		StorageGameTests.check(helper, CatalogueCardItem.card(cursor[0])!=null
				&&CardDeckItem.deck(deck).size()==item.getCapacity()-1, "an empty hand should take the last card out");
		StorageGameTests.check(helper, !ItemStack.isSameItemSameTags(new ItemStack(ECCatalogue.CARD.get()), cursor[0]),
				"a card taken out should still be punched");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void theCatalogueRetalliesCardsItCanReach(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		setup.file(setup.card(level));
		helper.runAfterDelay(25, () -> {
			CatalogueCard card = setup.filedCard();
			StorageGameTests.check(helper, card.talliedAt()!=CatalogueCard.NEVER&&card.tally().totalCount()==5,
					"the catalogue should have tallied the chest's 5 diamonds, got "+card.tally());
			level.removeBlock(setup.tube(), false);
			setup.chestEntity(level).setItem(1, new ItemStack(Items.DIAMOND, 7));
			helper.runAfterDelay(25, () -> {
				StorageGameTests.check(helper, setup.filedCard().tally().totalCount()==5,
						"a card the catalogue can't reach should keep its last tally, got "+setup.filedCard().tally());
				helper.succeed();
			});
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void theTeletypeSendsItsSlip(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 40);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), setup.teletype(), TerminalFeatures.REQUESTS);
		TeletypeBlockEntity teletype = (TeletypeBlockEntity)level.getBlockEntity(setup.teletype());

		CatalogueDesk.View view = new CatalogueDesk(level, setup.teletype()).view();
		StorageGameTests.check(helper, view.contents().getOrDefault(DIAMOND, 0L)==40,
				"the teletype should list the 40 diamonds tallied, got "+view.contents());
		StorageGameTests.check(helper, view.unreachable().isEmpty(), "the diamonds should be in reach");

		menu.handleClick(DIAMOND, TerminalAction.TAKE_STACK);
		menu.handleClick(DIAMOND, TerminalAction.TAKE_HALF);
		StorageGameTests.check(helper, teletype.getSlip().lines().equals(List.of(new RequestSlip.Line(DIAMOND, 40))),
				"the slip should ask for the 40 diamonds tallied, got "+teletype.getSlip().lines());
		menu.clickMenuButton(player, TerminalMenu.SLIP_LOWER);
		StorageGameTests.check(helper, teletype.getSlip().lines().get(0).amount()==39, "right-clicking a line lowers it");
		StorageGameTests.check(helper, setup.receiverEntity(level).getTray().isEmpty()
				&&setup.chestEntity(level).countItem(Items.DIAMOND)==40, "nothing should be sent before the slip is");

		menu.clickMenuButton(player, TerminalMenu.SEND_SLIP);
		StorageGameTests.check(helper, teletype.getSlip().isEmpty(), "a sent slip should be empty");
		helper.succeedWhen(() -> {
			helper.assertTrue(setup.receiverEntity(level).getTray().countItem(Items.DIAMOND)==39,
					"39 diamonds should land in the Receiving Station");
			helper.assertTrue(setup.chestEntity(level).countItem(Items.DIAMOND)==1, "1 should be left in the chest");
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void whatCantBeSentForStaysOnTheSlip(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 10);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), setup.teletype(), TerminalFeatures.REQUESTS);
		TeletypeBlockEntity teletype = (TeletypeBlockEntity)level.getBlockEntity(setup.teletype());
		menu.handleClick(DIAMOND, TerminalAction.TAKE_STACK);
		level.removeBlock(setup.tube(), false);
		menu.clickMenuButton(player, TerminalMenu.SEND_SLIP);
		StorageGameTests.check(helper, teletype.getSlip().lines().equals(List.of(new RequestSlip.Line(DIAMOND, 10))),
				"diamonds out of reach should stay on the slip, got "+teletype.getSlip().lines());
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void theTeletypePutsThingsAwayWhereTheyAreKept(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), setup.teletype(), TerminalFeatures.REQUESTS);

		menu.setCarried(new ItemStack(Items.COBBLESTONE, 16));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().getCount()==16, "cobblestone went somewhere that doesn't keep it");
		menu.setCarried(new ItemStack(Items.DIAMOND, 10));
		menu.handleClick(null, TerminalAction.INSERT_ALL);
		StorageGameTests.check(helper, menu.getCarried().isEmpty(), "the diamonds should have been sent off");
		setup.stationEntity(level).setTakesNewItems(true);
		player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 16));
		menu.quickMoveStack(player, 27);
		StorageGameTests.check(helper, player.getInventory().getItem(0).isEmpty(), "shift-clicked cobblestone wasn't sent");
		helper.succeedWhen(() -> {
			helper.assertTrue(setup.chestEntity(level).countItem(Items.DIAMOND)==15, "the diamonds should reach the chest");
			helper.assertTrue(setup.chestEntity(level).countItem(Items.COBBLESTONE)==16,
					"the cobblestone should reach the chest");
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void whatNoLongerFitsComesBack(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 5);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		CatalogueDesk desk = new CatalogueDesk(level, setup.teletype());
		StorageGameTests.check(helper, desk.deposit(new ItemStack(Items.DIAMOND, 10)).isEmpty(), "diamonds weren't sent");
		ChestBlockEntity chest = setup.chestEntity(level);
		for(int slot = 1; slot<chest.getContainerSize(); slot++)
			chest.setItem(slot, new ItemStack(Items.STONE, 64));
		chest.setItem(0, new ItemStack(Items.DIAMOND, 64));
		helper.succeedWhen(() -> helper.assertTrue(setup.receiverEntity(level).getTray().countItem(Items.DIAMOND)==10,
				"diamonds with no room left should come back to the Receiving Station"));
	}

	@GameTest(template = StorageGameTests.TEMPLATE, timeoutTicks = 200)
	public static void theTeletypeCanSendToAnotherReceivingStation(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 20);
		setup.file(setup.card(level).withTally(setup.stationEntity(level).tally(), level.getGameTime()));
		Direction side = Arrays.stream(Direction.values()).filter(d -> d.getAxis().isHorizontal()
				&&level.getBlockState(setup.tube().relative(d)).isAir()
				&&setup.tube().relative(d).distManhattan(setup.teletype())>1).findFirst().orElseThrow();
		BlockPos away = setup.tube().relative(side);
		level.setBlockAndUpdate(away, ECPneumatics.RECEIVING_STATION.get().defaultBlockState());
		((ReceivingStationBlockEntity)level.getBlockEntity(away)).setName("Packing Room");

		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		TerminalMenu menu = new TerminalMenu(1, player.getInventory(), setup.teletype(), TerminalFeatures.REQUESTS);
		TeletypeBlockEntity teletype = (TeletypeBlockEntity)level.getBlockEntity(setup.teletype());
		menu.clickMenuButton(player, TerminalMenu.NEXT_OUTLET);
		StorageGameTests.check(helper, away.equals(teletype.getOutlet()), "the teletype should point at the other station");
		menu.chooseOutlet(setup.receiver());
		StorageGameTests.check(helper, teletype.getOutlet()==null, "picking the home station should clear the outlet");
		menu.chooseOutlet(setup.chest());
		StorageGameTests.check(helper, teletype.getOutlet()==null, "a block that isn't an outlet shouldn't be picked");
		menu.chooseOutlet(away);
		StorageGameTests.check(helper, away.equals(teletype.getOutlet()), "the Packing Room should be picked");
		menu.handleClick(DIAMOND, TerminalAction.TAKE_STACK);
		menu.clickMenuButton(player, TerminalMenu.SEND_SLIP);
		helper.succeedWhen(() -> {
			helper.assertTrue(((ReceivingStationBlockEntity)level.getBlockEntity(away)).getTray()
					.countItem(Items.DIAMOND)==20, "the diamonds should land at the Packing Room");
			helper.assertTrue(setup.receiverEntity(level).getTray().isEmpty(), "nothing should land at home");
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aRenamedStationRelabelsItsCards(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 3);
		setup.file(setup.card(level));
		setup.stationEntity(level).setName("  Gem Store  ");
		StorageGameTests.check(helper, setup.stationEntity(level).label().getString().equals("Gem Store"),
				"a named station should be labelled with its name, trimmed");
		helper.runAfterDelay(25, () -> {
			StorageGameTests.check(helper, setup.filedCard().label().getString().equals("Gem Store"),
					"the catalogue should relabel the card, got "+setup.filedCard().label().getString());
			setup.stationEntity(level).setName("");
			StorageGameTests.check(helper, setup.stationEntity(level).label().getString()
					.equals(Blocks.CHEST.getName().getString()), "an unnamed station goes by its inventory's name");
			helper.succeed();
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void anEjectingReceivingStationSpitsOutWhatLands(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
		level.setBlockAndUpdate(pos, ECPneumatics.RECEIVING_STATION.get().defaultBlockState()
				.setValue(ReceivingStationBlock.FACING, Direction.EAST));
		ReceivingStationBlockEntity receiver = (ReceivingStationBlockEntity)level.getBlockEntity(pos);
		receiver.accept(new ItemStack(Items.DIAMOND, 7));
		receiver.setEjecting(true);
		helper.succeedWhen(() -> {
			helper.assertTrue(receiver.getTray().isEmpty(), "the tray should have been emptied");
			helper.assertItemEntityPresent(Items.DIAMOND, new BlockPos(4, 1, 3), 1.5);
		});
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aDecksScreenWritesItsCardsBack(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		StorageGameTests.MenuRecordingPlayer player = new StorageGameTests.MenuRecordingPlayer(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ECCatalogue.decks().get(0).get()));
		CardDeckMenu.open(player, InteractionHand.MAIN_HAND);
		CardDeckMenu menu = (CardDeckMenu)player.openedMenu();
		CatalogueCard card = new CatalogueCard(GlobalPos.of(level.dimension(), BlockPos.ZERO), Component.literal("Chest"),
				CellContents.EMPTY, CatalogueCard.NEVER);
		StorageGameTests.check(helper, !menu.getSlot(0).mayPlace(new ItemStack(ECCatalogue.CARD.get())),
				"a blank card shouldn't go in a deck");
		menu.getSlot(3).set(CatalogueCardItem.punched(card));
		StorageGameTests.check(helper, CardDeckItem.deck(player.getMainHandItem()).cards().equals(List.of(card)),
				"a card put in the deck's screen should be written to the deck");
		int deckSlot = menu.getCapacity()+27+player.getInventory().selected;
		StorageGameTests.check(helper, !menu.getSlot(deckSlot).mayPickup(player), "the open deck shouldn't be movable");
		menu.getSlot(3).set(ItemStack.EMPTY);
		StorageGameTests.check(helper, CardDeckItem.deck(player.getMainHandItem()).isEmpty(),
				"taking the card out should empty the deck");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void itemsOutOfReachAreListedButNotSent(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 10);
		level.removeBlock(setup.tube(), false);
		setup.file(setup.card(level).withTally(CellContents.of(Map.of(DIAMOND, 10L)), level.getGameTime()));
		CatalogueDesk desk = new CatalogueDesk(level, setup.teletype());
		CatalogueDesk.View view = desk.view();
		StorageGameTests.check(helper, view.contents().getOrDefault(DIAMOND, 0L)==10, "the last tally should still show");
		StorageGameTests.check(helper, view.unreachable().contains(DIAMOND), "the diamonds should be out of reach");
		StorageGameTests.check(helper, desk.request(DIAMOND, 10)==0, "nothing out of reach should be sent for");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void aTeletypeWithoutAReceivingStationShows424(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos teletype = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlockAndUpdate(teletype, StorageGameTests.terminal(ComputerEra.PUNCHCARD));
		StorageGameTests.check(helper, new CatalogueDesk(level, teletype).view().stats().noOutlet(),
				"a teletype against no Receiving Station should say so");
		helper.succeed();
	}

	@GameTest(template = StorageGameTests.TEMPLATE)
	public static void theCatalogueAndTeletypeStayOffTheDataNetwork(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		Setup setup = setup(helper, 0);
		BlockPos part = helper.absolutePos(StorageGameTests.ORIGIN);
		StorageGameTests.check(helper, NetworkMemberBlock.at(level, setup.teletype(), null)==null,
				"the teletype must never be a storage network member");
		StorageGameTests.check(helper, TubeMemberBlock.at(level, setup.teletype(), null)==null,
				"the teletype isn't on the tubes: it stands against a Receiving Station");
		StorageGameTests.check(helper, NetworkMemberBlock.at(level, part, null)==null,
				"the Tabulator Machine must never be a storage network member");
		StorageGameTests.check(helper, TubeMemberBlock.at(level, part, null)!=null,
				"the Tabulator Machine should be on the tubes");
		BlockPos cable = setup.teletype().above();
		level.setBlockAndUpdate(cable, ECCables.block().get().defaultBlockState());
		StorageGameTests.check(helper, !level.getBlockState(cable).getValue(NetworkCableBlock.CONNECTIONS.get(Direction.DOWN)),
				"a cable joined the teletype");
		helper.succeed();
	}
}
