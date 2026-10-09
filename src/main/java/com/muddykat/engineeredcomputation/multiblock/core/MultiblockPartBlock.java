package com.muddykat.engineeredcomputation.multiblock.core;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.muddykat.engineeredcomputation.network.StorageNetwork;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

public class MultiblockPartBlock extends Block implements EntityBlock
{
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private final Supplier<? extends MultiblockDefinition<?>> definition;

	public MultiblockPartBlock(Properties properties, Supplier<? extends MultiblockDefinition<?>> definition)
	{
		super(properties.dynamicShape());
		this.definition = definition;
	}

	public MultiblockDefinition<?> getDefinition()
	{
		return definition.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new MultiblockPartBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level, BlockState state, BlockEntityType<T> type)
	{
		if(level.isClientSide||!getDefinition().isTicking()||type!=ECMultiblocks.PART_BLOCK_ENTITY.get())
			return null;
		return (tickLevel, pos, tickState, be) -> ((MultiblockPartBlockEntity)be).tickServer();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		if(level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part)
			return getDefinition().shapeAt(part.getPosInMultiblock(), state.getValue(FACING));
		return Shapes.block();
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
	{
		if(level.isClientSide)
			return InteractionResult.SUCCESS;
		if(level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part)
		{
			MultiblockPartBlockEntity master = part.getMaster();
			if(master!=null&&master.getState()!=null)
			{
				MultiblockContext ctx = master.getContext();
				return master.getState().use(
						ctx, player, part.getPosInMultiblock(), ctx.toTemplatePoint(hitResult.getLocation()));
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock()))
			StorageNetwork.changed(level);
		BlockEntity be = level.getBlockEntity(pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
		if(!level.isClientSide&&!state.is(newState.getBlock())
				&&be instanceof MultiblockPartBlockEntity part&&!part.isDisassembling())
			getDefinition().disassemble(level, part);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
	{
		if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MultiblockPartBlockEntity part)
		{
			BlockState original = getDefinition().getOriginalBlock(
					params.getLevel(), part.getPosInMultiblock(), state.getValue(FACING));
			if(original!=null)
				return original.getDrops(params);
		}
		return List.of();
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
	{
		return new ItemStack(getDefinition().templateBlock());
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType)
	{
		return false;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
	{
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(!oldState.is(state.getBlock()))
			StorageNetwork.changed(level);
	}
}
