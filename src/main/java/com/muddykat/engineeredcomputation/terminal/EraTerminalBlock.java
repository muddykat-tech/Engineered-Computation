package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.NetworkMemberBlock;
import com.muddykat.engineeredcomputation.network.StorageNetwork;

public class EraTerminalBlock extends HorizontalDirectionalBlock implements NetworkMemberBlock
{
	public static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
	};

	private final ComputerEra era;

	public EraTerminalBlock(ComputerEra era, Properties properties)
	{
		super(properties);
		this.era = era;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public ComputerEra getEra()
	{
		return era;
	}

	@Override
	@Nullable
	public NetworkMember getNetworkMember(Level level, BlockPos pos, BlockState state, @Nullable Direction side)
	{
		if(TerminalFeatures.of(era).requests())
			return null;
		return level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays? bays: NETWORK_MEMBER;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return era.terminalShape(state.getValue(FACING));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult)
	{
		if(hand!=InteractionHand.MAIN_HAND)
			return InteractionResult.PASS;
		if(player instanceof ServerPlayer serverPlayer)
			TerminalMenu.open(serverPlayer, pos, getName(), features(), craftingGrid(level, pos));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	protected TerminalFeatures features()
	{
		return TerminalFeatures.of(era);
	}

	@Nullable
	protected CraftingGrid craftingGrid(Level level, BlockPos pos)
	{
		return null;
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
