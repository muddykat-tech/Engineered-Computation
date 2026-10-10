package com.muddykat.engineeredcomputation.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockDefinition;
import com.muddykat.engineeredcomputation.network.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.CoveredTubeBlock;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;

public class AssemblyWrenchItem extends Item
{
	public AssemblyWrenchItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		BlockState clicked = level.getBlockState(context.getClickedPos());
		if(clicked.getBlock() instanceof CoveredCableBlock||clicked.getBlock() instanceof CoveredTubeBlock)
		{
			if(!level.isClientSide)
				takeCover(level, context.getClickedPos(), context.getPlayer());
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		boolean isTemplateBlock = ECMultiblocks.formable().stream()
				.anyMatch(definition -> clicked.is(definition.templateBlock()));
		if(!isTemplateBlock)
			return InteractionResult.PASS;
		if(level.isClientSide)
			return InteractionResult.SUCCESS;

		for(MultiblockDefinition<?> definition : ECMultiblocks.formable())
			if(definition.tryForm(level, context.getClickedPos(), context.getClickedFace()))
			{
				level.playSound(null, context.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
				return InteractionResult.CONSUME;
			}

		if(context.getPlayer()!=null)
			context.getPlayer().displayClientMessage(
					new TranslatableComponent("message.engineered_computation.structure_incomplete"), true);
		return InteractionResult.CONSUME;
	}

	private static void takeCover(Level level, BlockPos pos, @Nullable Player player)
	{
		BlockState cover = level.getBlockEntity(pos) instanceof CoveredCableBlockEntity covered? covered.getCover(): null;
		ItemStack taken = level.getBlockState(pos).getBlock() instanceof CoveredTubeBlock? CoveredTubeBlock.uncover(level, pos)
				: CoveredCableBlock.uncover(level, pos);
		if(cover!=null&&!cover.isAir())
		{
			SoundType sound = cover.getSoundType(level, pos, player);
			level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume()+1)/2,
					sound.getPitch()*0.8F);
		}
		if(taken.isEmpty()||player!=null&&player.getAbilities().instabuild)
			return;
		if(player!=null)
			player.getInventory().placeItemBackInInventory(taken);
		else
			Block.popResource(level, pos, taken);
	}
}
