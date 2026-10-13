package com.muddykat.engineeredcomputation.pneumatic.tube;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlock;
import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.registration.ECPneumatics;

public class CoveredTubeBlock extends PneumaticTubeBlock implements EntityBlock
{
	public static final MapCodec<CoveredTubeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.BOOL.fieldOf("clear").forGetter(CoveredTubeBlock::isClear),
			propertiesCodec()
	).apply(instance, CoveredTubeBlock::new));

	private final boolean clear;

	public CoveredTubeBlock(boolean clear, Properties properties)
	{
		super(properties);
		this.clear = clear;
	}

	public boolean isClear()
	{
		return clear;
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	public boolean cuttable(BlockState state)
	{
		return false;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new CoveredTubeBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.block();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
	{
		return clear;
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
	{
		return clear? 1: super.getShadeBrightness(state, level, pos);
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return clear? Shapes.empty(): super.getVisualShape(state, level, pos, context);
	}

	@Override
	public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity)
	{
		BlockState cover = coverAt(level, pos);
		return cover.isAir()? super.getSoundType(state, level, pos, entity): cover.getSoundType(level, pos, entity);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos)
	{
		BlockState cover = coverAt(level, pos);
		return cover.isAir()? super.getDestroyProgress(state, player, level, pos)
				: cover.getDestroyProgress(player, level, pos);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
	{
		BlockState cover = coverAt(level, pos);
		return cover.isAir()? super.getCloneItemStack(level, pos, state): new ItemStack(cover.getBlock());
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
			FluidState fluid)
	{
		if(!level.isClientSide)
		{
			ItemStack cover = uncover(level, pos);
			if(!player.getAbilities().instabuild&&!cover.isEmpty())
				popResource(level, pos, cover);
		}
		return false;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
	{
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
		if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CoveredCableBlockEntity covered
				&&!covered.getCover().isAir())
			drops.add(new ItemStack(covered.getCover().getBlock()));
		return drops;
	}

	private static BlockState coverAt(BlockGetter level, BlockPos pos)
	{
		return level.getBlockEntity(pos) instanceof CoveredCableBlockEntity covered? covered.getCover()
				: Blocks.AIR.defaultBlockState();
	}

	public static boolean cover(Level level, BlockPos pos, BlockState cover)
	{
		BlockState tube = level.getBlockState(pos);
		if(!tube.is(ECPneumatics.TUBE.get())||!CoveredCableBlock.isValidCover(cover, level, pos))
			return false;
		int cut = level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector? connector.getCut(): 0;
		Block covered = (cover.canOcclude()? ECPneumatics.COVERED_TUBE: ECPneumatics.CLEAR_COVERED_TUBE).get();
		level.setBlock(pos, sameTube(covered.defaultBlockState(), tube), Block.UPDATE_ALL);
		if(level.getBlockEntity(pos) instanceof CoveredCableBlockEntity entity)
		{
			entity.setCut(cut);
			entity.setCover(cover);
		}
		return true;
	}

	public static ItemStack uncover(Level level, BlockPos pos)
	{
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof CoveredTubeBlock))
			return ItemStack.EMPTY;
		BlockState cover = coverAt(level, pos);
		int cut = level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector? connector.getCut(): 0;
		level.setBlock(pos, sameTube(ECPneumatics.TUBE.get().defaultBlockState(), state), Block.UPDATE_ALL);
		if(cut!=0&&level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector)
			connector.setCut(cut);
		return cover.isAir()? ItemStack.EMPTY: new ItemStack(cover.getBlock());
	}

	private static BlockState sameTube(BlockState state, BlockState from)
	{
		for(Property<?> property : from.getProperties())
			state = copy(state, from, property);
		return state;
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState state, BlockState from, Property<T> property)
	{
		return state.hasProperty(property)? state.setValue(property, from.getValue(property)): state;
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		if(!player.isSecondaryUseActive()||!(stack.getItem() instanceof BlockItem item)
				||!level.getBlockState(pos).is(ECPneumatics.TUBE.get()))
			return;
		BlockState cover = item.getBlock().getStateForPlacement(new BlockPlaceContext(player, event.getHand(), stack,
				event.getHitVec()));
		if(cover==null)
			cover = item.getBlock().defaultBlockState();
		if(!CoveredCableBlock.isValidCover(cover, level, pos))
			return;
		if(!level.isClientSide&&cover(level, pos, cover))
		{
			SoundType sound = cover.getSoundType(level, pos, player);
			level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume()+1)/2,
					sound.getPitch()*0.8F);
			stack.consume(1, player);
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
	}
}
