package com.muddykat.engineeredcomputation.terminal.teletype;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.registration.ECTerminals;

public class TeletypeBlockEntity extends BlockEntity
{
	private final RequestSlip slip = new RequestSlip(this::setChanged);
	private final SimpleContainer card = new SimpleContainer(1)
	{
		@Override
		public void setChanged()
		{
			super.setChanged();
			TeletypeBlockEntity.this.setChanged();
		}
	};
	@Nullable
	private BlockPos outlet;
	private boolean hidesLocked;

	public TeletypeBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECTerminals.TELETYPE.get(), pos, state);
	}

	public RequestSlip getSlip()
	{
		return slip;
	}

	public SimpleContainer cardContainer()
	{
		return card;
	}

	public ItemStack getCard()
	{
		return card.getItem(0);
	}

	@Nullable
	public BlockPos getOutlet()
	{
		return outlet;
	}

	public boolean hidesLocked()
	{
		return hidesLocked;
	}

	public void setHidesLocked(boolean hidesLocked)
	{
		this.hidesLocked = hidesLocked;
		setChanged();
	}

	public void setOutlet(@Nullable BlockPos outlet)
	{
		this.outlet = outlet==null? null: outlet.immutable();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		RequestSlip.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), slip.lines()).result()
				.ifPresent(lines -> tag.put("slip", lines));
		if(outlet!=null)
			tag.putLong("outlet", outlet.asLong());
		if(hidesLocked)
			tag.putBoolean("hides_locked", true);
		if(!getCard().isEmpty())
			tag.put("card", getCard().save(registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		Optional.ofNullable(tag.get("slip")).flatMap(lines -> RequestSlip.CODEC.parse(
				registries.createSerializationContext(NbtOps.INSTANCE), lines).result())
				.ifPresentOrElse(slip::set, () -> slip.set(List.of()));
		outlet = tag.contains("outlet")? BlockPos.of(tag.getLong("outlet")): null;
		hidesLocked = tag.getBoolean("hides_locked");
		card.setItem(0, tag.contains("card")? ItemStack.parseOptional(registries, tag.getCompound("card")): ItemStack.EMPTY);
	}
}
