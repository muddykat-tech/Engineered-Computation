package com.muddykat.engineeredcomputation.multiblock.core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

public class MultiblockPartBlockEntity extends BlockEntity implements MultiblockStateHost
{
	private static final int RENDER_REACH = 5;
	public static final ModelProperty<BlockPos> MODEL_OFFSET = new ModelProperty<>();

	private static final String POS_TAG = "pos_in_multiblock";
	private static final String STATE_TAG = "state";
	private static final String SYNC_TAG = "sync";

	private final MultiblockDefinition<?> definition;
	private BlockPos posInMultiblock = BlockPos.ZERO;
	@Nullable
	private MultiblockState state;
	private boolean disassembling;
	private CompoundTag clientSyncData = new CompoundTag();

	public MultiblockPartBlockEntity(BlockPos pos, BlockState blockState)
	{
		super(ECMultiblocks.PART_BLOCK_ENTITY.get(), pos, blockState);
		this.definition = ((MultiblockPartBlock)blockState.getBlock()).getDefinition();
	}

	void initialize(BlockPos posInMultiblock)
	{
		this.posInMultiblock = posInMultiblock.immutable();
		this.state = isMaster()? definition.createState(this): null;
		setChanged();
	}

	public MultiblockDefinition<?> getDefinition()
	{
		return definition;
	}

	public BlockPos getPosInMultiblock()
	{
		return posInMultiblock;
	}

	public Direction getFacing()
	{
		return getBlockState().getValue(MultiblockPartBlock.FACING);
	}

	public boolean isMaster()
	{
		return posInMultiblock.equals(definition.masterPos());
	}

	public BlockPos getMasterPos()
	{
		Direction facing = getFacing();
		return worldPosition
				.subtract(MultiblockOrientation.toWorldOffset(posInMultiblock, facing))
				.offset(MultiblockOrientation.toWorldOffset(definition.masterPos(), facing));
	}

	@Nullable
	public MultiblockPartBlockEntity getMaster()
	{
		if(isMaster())
			return this;
		if(level!=null&&level.getBlockEntity(getMasterPos()) instanceof MultiblockPartBlockEntity master
				&&master.definition==definition&&master.isMaster())
			return master;
		return null;
	}

	@Nullable
	public MultiblockState getState()
	{
		return state;
	}

	public CompoundTag getClientSyncData()
	{
		return clientSyncData;
	}

	@Override
	public void syncToClient()
	{
		setChanged();
		if(level!=null&&!level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Nullable
	@Override
	public MultiblockContext context()
	{
		return level==null||definition==null? null: getContext();
	}

	public MultiblockContext getContext()
	{
		return new MultiblockContext(level, worldPosition, getFacing(), definition, this::setChanged);
	}

	void markDisassembling()
	{
		disassembling = true;
	}

	boolean isDisassembling()
	{
		return disassembling;
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side)
	{
		MultiblockPartBlockEntity master = getMaster();
		if(master==null||master.state==null)
			return null;
		return master.state.getItemHandler(posInMultiblock, side);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side)
	{
		if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			IItemHandler handler = getItemHandler(side);
			if(handler!=null)
				return LazyOptional.of(() -> handler).cast();
		}
		return super.getCapability(capability, side);
	}

	@Override
	public AABB getRenderBoundingBox()
	{
		return isMaster()? new AABB(worldPosition).inflate(RENDER_REACH): new AABB(worldPosition);
	}

	void tickServer()
	{
		if(state!=null)
			state.tick(getContext());
	}

	@Override
	protected void saveAdditional(CompoundTag tag)
	{
		super.saveAdditional(tag);
		tag.putLong(POS_TAG, posInMultiblock.asLong());
		if(state!=null)
		{
			CompoundTag stateTag = new CompoundTag();
			state.save(stateTag);
			tag.put(STATE_TAG, stateTag);
		}
	}

	@Override
	public void load(CompoundTag tag)
	{
		super.load(tag);
		posInMultiblock = BlockPos.of(tag.getLong(POS_TAG));
		if(tag.contains(SYNC_TAG))
			clientSyncData = tag.getCompound(SYNC_TAG);
		if(isMaster()&&tag.contains(STATE_TAG))
		{
			if(state==null)
				state = definition.createState(this);
			state.load(tag.getCompound(STATE_TAG));
		}
	}

	@Override
	public CompoundTag getUpdateTag()
	{
		CompoundTag tag = new CompoundTag();
		tag.putLong(POS_TAG, posInMultiblock.asLong());
		if(state!=null)
		{
			CompoundTag sync = new CompoundTag();
			state.writeSyncData(sync);
			tag.put(SYNC_TAG, sync);
		}
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
	{
		BlockPos before = posInMultiblock;
		super.onDataPacket(net, pkt);
		onClientPositionKnown(before);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag)
	{
		BlockPos before = posInMultiblock;
		super.handleUpdateTag(tag);
		onClientPositionKnown(before);
	}

	/**
	 * Redraws the block when its piece of the model changes. Only then: a redraw is a chunk rebuild the client waits
	 * on, and drive contents sync through here every time a cell goes in or out.
	 */
	private void onClientPositionKnown(BlockPos before)
	{
		if(posInMultiblock.equals(before))
			return;
		requestModelDataUpdate();
		if(level!=null&&level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
	}

	@Override
	public IModelData getModelData()
	{
		return new ModelDataMap.Builder().withInitial(MODEL_OFFSET,
				posInMultiblock.subtract(definition.masterPos())).build();
	}
}
