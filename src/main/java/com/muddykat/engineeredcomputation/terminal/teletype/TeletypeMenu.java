package com.muddykat.engineeredcomputation.terminal.teletype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.muddykat.engineeredcomputation.catalogue.CatalogueDesk;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.program.ProgramEditPayload;
import com.muddykat.engineeredcomputation.program.ProgramEditor;
import com.muddykat.engineeredcomputation.program.ProgramEntry;
import com.muddykat.engineeredcomputation.registration.ECMenuTypes;
import com.muddykat.engineeredcomputation.storage.ItemKey;
import com.muddykat.engineeredcomputation.terminal.EraTerminalBlock;
import com.muddykat.engineeredcomputation.terminal.TerminalAction;
import com.muddykat.engineeredcomputation.terminal.TerminalFeatures;
import com.muddykat.engineeredcomputation.terminal.TerminalMenu;

public class TeletypeMenu extends TerminalMenu implements ProgramEditor
{
	public static final int CARD_SLOT = INVENTORY_SLOTS;
	public static final int CARD_X = GRID_X+COLUMNS*18+2;
	public static final int SEND_SLIP = 2;
	public static final int NEXT_OUTLET = 3;
	public static final int PREVIOUS_OUTLET = 4;
	public static final int HIDE_LOCKED = 5;
	public static final int SLIP_REMOVE = 16;
	public static final int SLIP_LOWER = SLIP_REMOVE+RequestSlip.SIZE;

	@Nullable
	private final CatalogueDesk desk;
	@Nullable
	private final TeletypeBlockEntity teletype;
	private Set<ItemKey> sentUnreachable = Set.of();
	private Map<ItemKey, Long> sentLocked = Map.of();
	@Nullable
	private TeletypeSlipPayload sentSlip;

	private Set<ItemKey> clientUnreachable = Set.of();
	private Map<ItemKey, Long> clientLocked = Map.of();
	private boolean clientHidesLocked;
	private List<RequestSlip.Line> clientSlip = List.of();
	private Component clientOutlet = Component.empty();
	private boolean clientOutletHome = true;
	private List<TeletypeSlipPayload.Outlet> clientOutlets = List.of();

	public TeletypeMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf)
	{
		this(id, playerInventory, buf.readBlockPos());
	}

	public TeletypeMenu(int id, Inventory playerInventory, BlockPos terminalPos)
	{
		super(ECMenuTypes.TELETYPE.get(), id, playerInventory, terminalPos);
		boolean server = !player.level().isClientSide;
		this.teletype = server&&player.level().getBlockEntity(terminalPos) instanceof TeletypeBlockEntity entity
				? entity: null;
		this.desk = server? new CatalogueDesk(player.level(), terminalPos,
				() -> teletype==null? null: teletype.getOutlet()): null;
		addDataSlot(new DataSlot()
		{
			@Override
			public int get()
			{
				return hidesLocked()? 1: 0;
			}

			@Override
			public void set(int value)
			{
				clientHidesLocked = value!=0;
			}
		});
		addSlot(new Slot(teletype!=null? teletype.cardContainer(): new SimpleContainer(1), 0, CARD_X+1,
				getLayout().inventoryY()+1)
		{
			@Override
			public boolean mayPlace(ItemStack stack)
			{
				return stack.getItem() instanceof FilterCardItem;
			}

			@Override
			public int getMaxStackSize()
			{
				return 1;
			}
		});
	}

	@Override
	public TerminalFeatures getFeatures()
	{
		return TerminalFeatures.REQUESTS;
	}

	@Override
	public void edit(int index, ProgramEditPayload.Action action, String text, ItemStack item)
	{
		if(teletype==null||index!=0||!(teletype.getCard().getItem() instanceof FilterCardItem))
			return;
		ProgramEntry entry = ProgramEditor.entry(action, text, item.isEmpty()? getCarried(): item);
		if(entry==null)
			return;
		FilterCardItem.setFilter(teletype.getCard(), entry);
		teletype.cardContainer().setChanged();
		broadcastChanges();
	}

	public ProgramEntry cardFilter()
	{
		return FilterCardItem.filter(getSlot(CARD_SLOT).getItem());
	}

	@Override
	protected Snapshot snapshot(ServerPlayer serverPlayer)
	{
		CatalogueDesk.View view = desk.view();
		if(!view.unreachable().equals(sentUnreachable))
		{
			sentUnreachable = view.unreachable();
			PacketDistributor.sendToPlayer(serverPlayer,
					new TeletypeUnreachablePayload(containerId, List.copyOf(sentUnreachable)));
		}
		if(!view.locked().equals(sentLocked))
		{
			sentLocked = Map.copyOf(view.locked());
			PacketDistributor.sendToPlayer(serverPlayer, new TeletypeLockedPayload(containerId, sentLocked.entrySet()
					.stream().map(entry -> new TeletypeLockedPayload.Entry(entry.getKey(), entry.getValue())).toList()));
		}
		List<TeletypeSlipPayload.Outlet> outlets = view.outlets().stream().map(pos -> new TeletypeSlipPayload.Outlet(
				pos, desk.label(pos), pos.equals(view.outlet()))).toList();
		TeletypeSlipPayload slip = new TeletypeSlipPayload(containerId,
				teletype==null? List.of(): List.copyOf(teletype.getSlip().lines()),
				view.outlet()==null? Component.empty(): desk.label(view.outlet()),
				view.outlet()==null||view.outlet().equals(view.home()), outlets);
		if(!slip.equals(sentSlip))
		{
			sentSlip = slip;
			PacketDistributor.sendToPlayer(serverPlayer, slip);
		}
		return new Snapshot(view.contents(), view.stats());
	}

	@Override
	protected void click(@Nullable ItemKey key, TerminalAction action)
	{
		if(action==TerminalAction.INSERT_ALL||action==TerminalAction.INSERT_ONE)
		{
			ItemStack carried = getCarried();
			if(carried.isEmpty())
				return;
			int amount = action==TerminalAction.INSERT_ALL? carried.getCount(): 1;
			ItemStack left = desk.deposit(carried.split(amount));
			carried.grow(left.getCount());
			setCarried(carried.isEmpty()&&!left.isEmpty()? left: carried);
		}
		else if(key!=null&&teletype!=null)
		{
			int tallied = (int)Math.min(Integer.MAX_VALUE, desk.view().contents().getOrDefault(key, 0L));
			teletype.getSlip().add(key, action==TerminalAction.TAKE_HALF? 1: key.getMaxStackSize(), tallied);
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int id)
	{
		if(id<SEND_SLIP||id>HIDE_LOCKED&&(id<SLIP_REMOVE||id>=SLIP_LOWER+RequestSlip.SIZE))
			return false;
		if(player.level().isClientSide||teletype==null)
			return true;
		if(id==SEND_SLIP)
			sendSlip();
		else if(id==NEXT_OUTLET||id==PREVIOUS_OUTLET)
			cycleOutlet(id==NEXT_OUTLET? 1: -1);
		else if(id==HIDE_LOCKED)
			teletype.setHidesLocked(!teletype.hidesLocked());
		else if(id<SLIP_LOWER)
			teletype.getSlip().remove(id-SLIP_REMOVE);
		else
			teletype.getSlip().lower(id-SLIP_LOWER, 1);
		return true;
	}

	private void sendSlip()
	{
		if(teletype.getSlip().isEmpty())
			return;
		List<RequestSlip.Line> left = new ArrayList<>();
		for(RequestSlip.Line line : teletype.getSlip().lines())
		{
			int asked = desk.request(line.key(), line.amount());
			if(asked<line.amount())
				left.add(new RequestSlip.Line(line.key(), line.amount()-asked));
		}
		teletype.getSlip().set(left);
		player.level().playSound(null, terminalPos, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.BLOCKS, 0.7F, 1.3F);
	}

	public void chooseOutlet(BlockPos pos)
	{
		if(teletype==null)
			return;
		List<BlockPos> outlets = desk.outlets();
		if(!outlets.contains(pos))
			return;
		teletype.setOutlet(pos.equals(outlets.get(0))? null: pos);
		desk.view();
	}

	private void cycleOutlet(int step)
	{
		List<BlockPos> outlets = desk.outlets();
		if(outlets.isEmpty())
			return;
		BlockPos current = desk.view().outlet();
		int index = Math.max(0, outlets.indexOf(current));
		BlockPos next = outlets.get(Math.floorMod(index+step, outlets.size()));
		teletype.setOutlet(next.equals(outlets.get(0))? null: next);
		desk.view();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		Slot slot = slots.get(index);
		if(player.level().isClientSide||!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		if(index==CARD_SLOT)
			moveItemStackTo(stack, 0, INVENTORY_SLOTS, true);
		else if(stack.getItem() instanceof FilterCardItem&&!getSlot(CARD_SLOT).hasItem())
			moveItemStackTo(stack, CARD_SLOT, CARD_SLOT+1, false);
		else
			slot.set(desk.deposit(stack));
		slot.setChanged();
		broadcastChanges();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player)
	{
		return player.level().getBlockState(terminalPos).getBlock() instanceof EraTerminalBlock
				&&player.canInteractWithBlock(terminalPos, 4.0);
	}

	void applyLocked(TeletypeLockedPayload payload)
	{
		Map<ItemKey, Long> locked = new HashMap<>();
		payload.locked().forEach(entry -> locked.put(entry.key(), entry.count()));
		clientLocked = locked;
		clientChanged();
	}

	void applyUnreachable(TeletypeUnreachablePayload payload)
	{
		clientUnreachable = Set.copyOf(payload.unreachable());
		clientChanged();
	}

	void applySlip(TeletypeSlipPayload payload)
	{
		clientSlip = payload.lines();
		clientOutlet = payload.outlet();
		clientOutletHome = payload.home();
		clientOutlets = payload.outlets();
	}

	public boolean hidesLocked()
	{
		return teletype!=null? teletype.hidesLocked(): clientHidesLocked;
	}

	public Map<ItemKey, Long> getClientLocked()
	{
		return clientLocked;
	}

	public boolean isUnreachableOnClient(ItemKey key)
	{
		return clientUnreachable.contains(key);
	}

	public List<TeletypeSlipPayload.Outlet> getClientOutlets()
	{
		return clientOutlets;
	}

	public List<RequestSlip.Line> getClientSlip()
	{
		return clientSlip;
	}

	public Component getClientOutlet()
	{
		return clientOutlet;
	}

	public boolean isClientOutletHome()
	{
		return clientOutletHome;
	}
}
