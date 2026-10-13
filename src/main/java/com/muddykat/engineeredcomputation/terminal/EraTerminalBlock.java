package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

import com.muddykat.engineeredcomputation.ECConfig;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.network.NetworkMember;
import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.terminal.crafting.CraftingGrid;

public class EraTerminalBlock extends HorizontalDirectionalBlock
{
	public static final MapCodec<EraTerminalBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ComputerEra.CODEC.fieldOf("era").forGetter(EraTerminalBlock::getEra),
			propertiesCodec()
	).apply(instance, EraTerminalBlock::new));

	public static final NetworkMember NETWORK_MEMBER = new NetworkMember()
	{
		@Override
		public int powerCost(Level level, BlockPos pos)
		{
			return level.getBlockState(pos).getBlock() instanceof EraTerminalBlock terminal
					? ECConfig.cost(ECConfig.terminalKey(terminal.getEra())): 0;
		}
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
	protected MapCodec<? extends HorizontalDirectionalBlock> codec()
	{
		return CODEC;
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return era.terminalShape(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{
		if(player instanceof ServerPlayer serverPlayer)
			openTerminal(serverPlayer, level, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	public void openTerminal(ServerPlayer player, Level level, BlockPos pos)
	{
		TerminalMenu.open(player, pos, getName(), era, craftingGrid(level, pos));
	}

	@Nullable
	protected CraftingGrid craftingGrid(Level level, BlockPos pos)
	{
		return null;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			StorageNetwork.changed(level);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			StorageNetwork.changed(level);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
