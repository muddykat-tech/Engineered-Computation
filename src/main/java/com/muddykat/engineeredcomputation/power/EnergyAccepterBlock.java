package com.muddykat.engineeredcomputation.power;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.NetworkMemberBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECPower;

/** Takes RF on any side and joins network cables on any side, paying its network's machines while power is enabled. */
public class EnergyAccepterBlock extends BaseEntityBlock implements NetworkMemberBlock
{
	private static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
		@Override
		public boolean isEnergyAccepter()
		{
			return true;
		}
	};

	public EnergyAccepterBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	@Nullable
	public NetworkMember getNetworkMember(Level level, BlockPos pos, BlockState state, @Nullable Direction side)
	{
		return NETWORK_MEMBER;
	}

	@Override
	public RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new EnergyAccepterBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type)
	{
		return level.isClientSide? null: createTickerHelper(type, ECPower.BLOCK_ENTITY.get(),
				(tickLevel, pos, tickState, accepter) -> accepter.tickServer());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult)
	{
		if(!level.isClientSide&&level.getBlockEntity(pos) instanceof EnergyAccepterBlockEntity accepter)
			NetworkHooks.openScreen((ServerPlayer)player, accepter, buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			StorageNetwork.changed(level);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			StorageNetwork.changed(level);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
