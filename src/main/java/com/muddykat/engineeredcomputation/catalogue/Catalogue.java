package com.muddykat.engineeredcomputation.catalogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.pneumatic.TubeNetwork;
import com.muddykat.engineeredcomputation.pneumatic.TubeStationBlockEntity;

public final class Catalogue
{
	public static final int RETALLY_TICKS = 20;

	private Catalogue()
	{
	}

	public static boolean retally(Level level, BlockPos pos, IItemHandler decks)
	{
		TubeNetwork network = TubeNetwork.of(level, pos);
		long now = level.getGameTime();
		boolean changed = false;
		for(int slot = 0; slot<decks.getSlots(); slot++)
		{
			ItemStack stack = decks.getStackInSlot(slot);
			if(!(stack.getItem() instanceof CardDeckItem))
				continue;
			CardDeck deck = CardDeckItem.deck(stack);
			CardDeck retallied = deck;
			for(int i = 0; i<deck.size(); i++)
			{
				TubeStationBlockEntity station = station(level, network, deck.cards().get(i));
				if(station!=null)
					retallied = retallied.with(i, deck.cards().get(i).withReport(station.label(), station.tally(), now));
			}
			if(retallied!=deck)
			{
				CardDeckItem.setDeck(stack, retallied);
				changed = true;
			}
		}
		return changed;
	}

	@Nullable
	public static TubeStationBlockEntity station(Level level, TubeNetwork network, CatalogueCard card)
	{
		BlockPos pos = card.station().pos();
		if(!card.station().dimension().equals(level.dimension())||!network.getEnds().contains(pos)
				||!level.isLoaded(pos))
			return null;
		return level.getBlockEntity(pos) instanceof TubeStationBlockEntity station? station: null;
	}

	public static List<EraDriveState> on(Level level, TubeNetwork network)
	{
		Set<EraDriveState> found = Collections.newSetFromMap(new IdentityHashMap<>());
		List<EraDriveState> catalogues = new ArrayList<>();
		for(BlockPos end : network.getEnds())
			if(level.getBlockEntity(end) instanceof MultiblockPartBlockEntity part&&part.getMaster()!=null
					&&part.getMaster().getState() instanceof EraDriveState state&&state.getEra()==ComputerEra.PUNCHCARD
					&&found.add(state))
				catalogues.add(state);
		return catalogues;
	}

	public static List<CatalogueCard> cards(EraDriveState catalogue)
	{
		List<CatalogueCard> cards = new ArrayList<>();
		IItemHandler decks = catalogue.getCellSlots();
		for(int slot = 0; slot<decks.getSlots(); slot++)
			cards.addAll(CardDeckItem.deck(decks.getStackInSlot(slot)).cards());
		return cards;
	}

	public static int capacity(EraDriveState catalogue)
	{
		int capacity = 0;
		IItemHandler decks = catalogue.getCellSlots();
		for(int slot = 0; slot<decks.getSlots(); slot++)
			if(decks.getStackInSlot(slot).getItem() instanceof CardDeckItem deck)
				capacity += deck.getCapacity();
		return capacity;
	}
}
