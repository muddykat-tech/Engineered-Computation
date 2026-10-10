package com.muddykat.engineeredcomputation.pneumatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class ReceivingStationBlockEntity extends BlockEntity
{
	public static final int SLOTS = 9;
	public static final int EJECT_TICKS = 4;
	public static final Component TITLE =
			new TranslatableComponent("container.engineered_computation.receiving_station");

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
			return level!=null&&level.getBlockEntity(worldPosition)==ReceivingStationBlockEntity.this
					&&player.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64;
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

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side)
	{
		if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			IItemHandler handler = getOutput();
			if(handler!=null)
				return LazyOptional.of(() -> handler).cast();
		}
		return super.getCapability(capability, side);
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
		return name.isEmpty()? getBlockState().getBlock().getName(): new TextComponent(name);
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
		BlockEntity inFront = level.getBlockEntity(worldPosition.relative(front));
		IItemHandler target = inFront==null? null
				: inFront.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, front.getOpposite())
						.orElse(null);
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
				level.levelEvent(LevelEvent.PARTICLES_SHOOT, worldPosition, front.get3DDataValue());
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
		int room = 0;
		for(int slot = 0; slot<tray.getContainerSize()&&room<items.getCount(); slot++)
		{
			ItemStack held = tray.getItem(slot);
			if(held.isEmpty())
				room += items.getMaxStackSize();
			else if(ItemStack.isSameItemSameTags(held, items))
				room += Math.max(0, held.getMaxStackSize()-held.getCount());
		}
		return room>=items.getCount();
	}

	@Override
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		tag.put("tray", tray.createTag());
		if(!name.isEmpty())
			tag.putString("name", name);
		tag.putBoolean("ejecting", ejecting);
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		tray.fromTag(tag.getList("tray", Tag.TAG_COMPOUND));
		name = tag.getString("name");
		ejecting = tag.getBoolean("ejecting");
	}
}
