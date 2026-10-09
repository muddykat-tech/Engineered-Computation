package com.muddykat.engineeredcomputation.terminal;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.catalogue.RequestSlip;
import com.muddykat.engineeredcomputation.registration.ECTerminals;

public class TeletypeBlockEntity extends BlockEntity
{
	private final RequestSlip slip = new RequestSlip(this::setChanged);
	@Nullable
	private BlockPos outlet;

	public TeletypeBlockEntity(BlockPos pos, BlockState state)
	{
		super(ECTerminals.TELETYPE.get(), pos, state);
	}

	public RequestSlip getSlip()
	{
		return slip;
	}

	@Nullable
	public BlockPos getOutlet()
	{
		return outlet;
	}

	public void setOutlet(@Nullable BlockPos outlet)
	{
		this.outlet = outlet==null? null: outlet.immutable();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		RequestSlip.CODEC.encodeStart(NbtOps.INSTANCE, slip.lines()).result()
				.ifPresent(lines -> tag.put("slip", lines));
		if(outlet!=null)
			tag.putLong("outlet", outlet.asLong());
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		Optional.ofNullable(tag.get("slip")).flatMap(lines -> RequestSlip.CODEC.parse(
				NbtOps.INSTANCE, lines).result())
				.ifPresentOrElse(slip::set, () -> slip.set(List.of()));
		outlet = tag.contains("outlet")? BlockPos.of(tag.getLong("outlet")): null;
	}
}
