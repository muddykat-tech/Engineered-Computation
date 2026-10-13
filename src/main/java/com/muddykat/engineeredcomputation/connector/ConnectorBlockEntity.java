package com.muddykat.engineeredcomputation.connector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Holds wrench-disabled sides in data so you can still keep them when covering them with a block. */
public class ConnectorBlockEntity extends BlockEntity
{
	public static final ModelProperty<Integer> CUT = new ModelProperty<>();

	private int cut;

	public ConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public static boolean isCut(BlockGetter level, BlockPos pos, Direction side)
	{
		return level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector&&connector.isCut(side);
	}

	public boolean isCut(Direction side)
	{
		return (cut&1<<side.ordinal())!=0;
	}

	public int getCut()
	{
		return cut;
	}

	public void setCut(int cut)
	{
		this.cut = cut;
		setChanged();
		if(level!=null&&!level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	public void setCut(Direction side, boolean cut)
	{
		setCut(cut? this.cut|1<<side.ordinal(): this.cut&~(1<<side.ordinal()));
	}

	@Override
	public ModelData getModelData()
	{
		return ModelData.builder().with(CUT, cut).build();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		if(cut!=0)
			tag.putInt("cut", cut);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		cut = tag.getInt("cut");
	}

	/**
	 * Always sends the cut sides, even when there are none, because if the client ignores an empty update -> stale data
	 */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.putInt("cut", cut);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
			HolderLookup.Provider registries)
	{
		super.onDataPacket(connection, packet, registries);
		redraw();
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.handleUpdateTag(tag, registries);
		redraw();
	}

	protected void redraw()
	{
		requestModelDataUpdate();
		if(level!=null&&level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
	}
}
