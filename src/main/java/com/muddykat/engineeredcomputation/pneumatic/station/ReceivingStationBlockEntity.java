package com.muddykat.engineeredcomputation.pneumatic.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class ReceivingStationBlockEntity extends BlockEntity
{
	public static final int SLOTS = 9;
	public static final int EJECT_TICKS = 4;
	public static final Component TITLE = Component.translatable("container.engineered_computation.receiving_station");

	private final SimpleContainer tray = new SimpleContainer(SLOTS)
	{
		@Override
		public void setChanged()
		{
			super.setChanged();
			ReceivingStationBlockEntity.this.setChanged();
		}

		@Override
		public boolean stillValid(Player player)
		{
			return Container.stillValidBlockEntity(ReceivingStationBlockEntity.this, player);
		}
	};

	private final IItemHandler output = new InvWrapper(tray)
	{
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
		{
			return stack;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack)
		{
			return false;
		}
	};

	private String name = "";
	private boolean ejecting;
	private int ejectCooldown;

	public ReceivingStationBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.RECEIVING_STATION_ENTITY.get(), pos, state);
	}

	public SimpleContainer getTray()
	{
		return tray;
	}

	public IItemHandler getOutput()
	{
		return output;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		String stripped = name.strip();
		this.name = stripped.substring(0, Math.min(TubeStationBlockEntity.MAX_NAME, stripped.length()));
		setChanged();
	}

	public Component label()
	{
		return name.isEmpty()? getBlockState().getBlock().getName(): Component.literal(name);
	}

	public boolean isEjecting()
	{
		return ejecting;
	}

	public void setEjecting(boolean ejecting)
	{
		this.ejecting = ejecting;
		setChanged();
	}

	void tickServer()
	{
		if(!ejecting||tray.isEmpty()||--ejectCooldown>0||level==null)
			return;
		ejectCooldown = EJECT_TICKS;
		Direction front = getBlockState().getValue(ReceivingStationBlock.FACING);
		IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(front),
				front.getOpposite());
		for(int slot = 0; slot<tray.getContainerSize(); slot++)
		{
			ItemStack stack = tray.getItem(slot);
			if(stack.isEmpty())
				continue;
			if(target!=null)
			{
				ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
				if(left.getCount()==stack.getCount())
					continue;
				tray.setItem(slot, left);
			}
			else
			{
				Vec3 mouth = Vec3.atCenterOf(worldPosition).add(front.getStepX()*0.7, 0, front.getStepZ()*0.7);
				DefaultDispenseItemBehavior.spawnItem(level, tray.removeItemNoUpdate(slot), 6, front, mouth);
				tray.setChanged();
				level.levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, worldPosition, front.get3DDataValue());
			}
			level.levelEvent(LevelEvent.SOUND_DISPENSER_DISPENSE, worldPosition, 0);
			return;
		}
	}

	public ItemStack accept(ItemStack items)
	{
		return fits(items)? tray.addItem(items.copy()): items;
	}

	private boolean fits(ItemStack items)
	{
		return room(items)>=items.getCount();
	}

	public int room(ItemStack items)
	{
		int room = 0;
		for(int slot = 0; slot<tray.getContainerSize()&&room<items.getCount(); slot++)
		{
			ItemStack held = tray.getItem(slot);
			if(held.isEmpty())
				room += items.getMaxStackSize();
			else if(ItemStack.isSameItemSameComponents(held, items))
				room += Math.max(0, held.getMaxStackSize()-held.getCount());
		}
		return Math.min(room, items.getCount());
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.put("tray", tray.createTag(registries));
		if(!name.isEmpty())
			tag.putString("name", name);
		tag.putBoolean("ejecting", ejecting);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		tray.fromTag(tag.getList("tray", Tag.TAG_COMPOUND), registries);
		name = tag.getString("name");
		ejecting = tag.getBoolean("ejecting");
	}
}
