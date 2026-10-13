package com.muddykat.engineeredcomputation.pneumatic.pressurizer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

/** Burns fuel only when it runs out of charge, so nothing is wasted while the network is idle. */
public class PressurizerBlockEntity extends BlockEntity
{
	private final SimpleContainer fuel = new SimpleContainer(1)
	{
		@Override
		public boolean canPlaceItem(int slot, ItemStack stack)
		{
			return isFuel(stack);
		}

		@Override
		public void setChanged()
		{
			super.setChanged();
			PressurizerBlockEntity.this.setChanged();
			updateLit();
		}
	};
	private int charge;

	public PressurizerBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECPneumatics.PRESSURIZER_ENTITY.get(), pos, state);
	}

	public static TagKey<Item> fuelTag()
	{
		ResourceLocation id = ResourceLocation.tryParse(ECConfig.pressurizerFuel());
		return id==null? ItemTags.COALS: TagKey.create(Registries.ITEM, id);
	}

	public static boolean isFuel(ItemStack stack)
	{
		return stack.is(fuelTag());
	}

	public SimpleContainer fuelContainer()
	{
		return fuel;
	}

	public int getCharge()
	{
		return charge;
	}

	public boolean hasPressure()
	{
		return charge>0||isFuel(fuel.getItem(0));
	}

	public boolean draw()
	{
		if(charge<=0)
		{
			ItemStack held = fuel.getItem(0);
			if(!isFuel(held))
				return false;
			held.shrink(1);
			charge = ECConfig.carriersPerFuel();
			fuel.setChanged();
		}
		charge--;
		setChanged();
		updateLit();
		return true;
	}

	private void updateLit()
	{
		if(level==null||level.isClientSide)
			return;
		BlockState state = getBlockState();
		boolean lit = hasPressure();
		if(state.hasProperty(PressurizerBlock.LIT)&&state.getValue(PressurizerBlock.LIT)!=lit)
			level.setBlock(worldPosition, state.setValue(PressurizerBlock.LIT, lit), Block.UPDATE_ALL);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		if(!fuel.getItem(0).isEmpty())
			tag.put("fuel", fuel.getItem(0).save(registries));
		tag.putInt("charge", charge);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		fuel.setItem(0, tag.contains("fuel")? ItemStack.parseOptional(registries, tag.getCompound("fuel")): ItemStack.EMPTY);
		charge = tag.getInt("charge");
	}
}
