package com.muddykat.engineeredcomputation.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveMenu;
import com.muddykat.engineeredcomputation.storage.EraDataCell;

public class FloppyTerminalBlock extends CraftingTerminalBlock
{
	private static final double BAYS_TOP = 5/16.0;
	private static final double BAYS_FRONT = 1.5/16.0;
	private static final double BAYS_SPLIT = 8/16.0;

	public FloppyTerminalBlock(Properties properties)
	{
		super(ComputerEra.FLOPPY, properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TerminalDiskBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult)
	{
		if(hand!=InteractionHand.MAIN_HAND)
			return InteractionResult.PASS;
		int bay = bayAt(state, pos, hitResult.getLocation());
		if(bay>=0&&player.isSecondaryUseActive()&&level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays
				&&!bays.getDisks().getStackInSlot(bay).isEmpty())
		{
			if(!level.isClientSide)
				player.setItemInHand(InteractionHand.MAIN_HAND, bays.getDisks().extractItem(bay, 1, false));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if(bay>=0)
		{
			if(player instanceof ServerPlayer serverPlayer&&level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays)
				EraDriveMenu.openTerminalDisks(serverPlayer, pos, getName(), bays.getDisks());
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.use(state, level, pos, player, hand, hitResult);
	}

	public static InteractionResult insertIntoBay(Level level, BlockPos pos, Player player, InteractionHand hand,
			Vec3 click)
	{
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof FloppyTerminalBlock)
				||!(level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays))
			return InteractionResult.PASS;
		int slot = bayAt(state, pos, click);
		if(slot<0||!bays.getDisks().getStackInSlot(slot).isEmpty()
				||!bays.getDisks().isItemValid(slot, player.getItemInHand(hand)))
			return InteractionResult.PASS;
		if(!level.isClientSide)
			bays.getDisks().setStackInSlot(slot, EraDataCell.takeFromHand(player, hand));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected TerminalFeatures features()
	{
		return TerminalFeatures.ORDERING;
	}

	private static int bayAt(BlockState state, BlockPos pos, Vec3 click)
	{
		Vec3 local = MultiblockOrientation.toTemplatePoint(click.subtract(Vec3.atLowerCornerOf(pos)),
				state.getValue(FACING));
		if(local.y>=BAYS_TOP||local.z>=BAYS_FRONT)
			return -1;
		return local.x>=BAYS_SPLIT? 0: 1;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
	{
		if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof TerminalDiskBlockEntity bays)
			bays.dropDisks(level, pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
