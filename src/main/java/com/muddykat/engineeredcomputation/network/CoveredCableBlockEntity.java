package com.muddykat.engineeredcomputation.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

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
	public ModelData getModelData()
	{
		return ModelData.builder().with(COVER, cover).build();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.put("cover", NbtUtils.writeBlockState(cover));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		cover = tag.contains("cover")? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),
				tag.getCompound("cover")): Blocks.AIR.defaultBlockState();
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		return saveWithoutMetadata(registries);
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

	private void redraw()
	{
		requestModelDataUpdate();
		if(level!=null&&level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
	}
}
