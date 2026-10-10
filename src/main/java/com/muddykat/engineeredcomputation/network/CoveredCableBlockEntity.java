package com.muddykat.engineeredcomputation.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import com.muddykat.engineeredcomputation.registration.ECCables;

public class CoveredCableBlockEntity extends BlockEntity
{
	public static final ModelProperty<BlockState> COVER = new ModelProperty<>();

	private BlockState cover = Blocks.AIR.defaultBlockState();

	public CoveredCableBlockEntity(BlockPos pos, BlockState state)
	{
		this(ECCables.COVERED_ENTITY.get(), pos, state);
	}

	protected CoveredCableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public BlockState getCover()
	{
		return cover;
	}

	public void setCover(BlockState cover)
	{
		this.cover = cover;
		setChanged();
		if(level!=null&&!level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	@Override
	public IModelData getModelData()
	{
		return new ModelDataMap.Builder().withInitial(COVER, cover).build();
	}

	@Override
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		tag.put("cover", NbtUtils.writeBlockState(cover));
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		cover = tag.contains("cover")?
				NbtUtils.readBlockState(tag.getCompound("cover")): Blocks.AIR.defaultBlockState();
	}

	@Override
	public CompoundTag getUpdateTag()
	{
		return saveWithoutMetadata();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet)
	{
		super.onDataPacket(connection, packet);
		redraw();
	}

	@Override
	public void handleUpdateTag(CompoundTag tag)
	{
		super.handleUpdateTag(tag);
		redraw();
	}

	private void redraw()
	{
		requestModelDataUpdate();
		if(level!=null&&level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
	}
}
