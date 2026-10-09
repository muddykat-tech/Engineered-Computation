package com.muddykat.engineeredcomputation.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;
import com.muddykat.engineeredcomputation.registration.ECCables;

public class CoveredCableBlock extends NetworkCableBlock implements EntityBlock
{
	public static final MapCodec<CoveredCableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.BOOL.fieldOf("clear").forGetter(CoveredCableBlock::isClear),
			propertiesCodec()
	).apply(instance, CoveredCableBlock::new));

	public static final TagKey<Block> CANNOT_COVER = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "cannot_cover"));

	private final boolean clear;

	public CoveredCableBlock(boolean clear, Properties properties)
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

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new CoveredCableBlockEntity(pos, state);
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
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
	{
		BlockState cover = coverAt(level, pos);
		return cover.isAir()? ECCables.item().toStack(): new ItemStack(cover.getBlock());
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

	public static boolean isValidCover(BlockState cover, BlockGetter level, BlockPos pos)
	{
		return !cover.isAir()&&cover.getRenderShape()==RenderShape.MODEL&&!cover.hasBlockEntity()
				&&cover.isCollisionShapeFullBlock(level, pos)&&!(cover.getBlock() instanceof FallingBlock)
				&&!(cover.getBlock() instanceof NetworkCableBlock)&&!(cover.getBlock() instanceof PneumaticTubeBlock)
				&&!cover.isSignalSource()&&!cover.is(CANNOT_COVER);
	}

	public static boolean cover(Level level, BlockPos pos, BlockState cover)
	{
		BlockState cable = level.getBlockState(pos);
		if(!cable.is(ECCables.block().get())||!isValidCover(cover, level, pos))
			return false;
		Block covered = (cover.canOcclude()? ECCables.covered(): ECCables.clearCovered()).get();
		level.setBlock(pos, withConnections(covered.defaultBlockState(), cable), Block.UPDATE_ALL);
		if(level.getBlockEntity(pos) instanceof CoveredCableBlockEntity entity)
			entity.setCover(cover);
		return true;
	}

	public static ItemStack uncover(Level level, BlockPos pos)
	{
		BlockState state = level.getBlockState(pos);
		if(!(state.getBlock() instanceof CoveredCableBlock))
			return ItemStack.EMPTY;
		BlockState cover = coverAt(level, pos);
		level.setBlock(pos, withConnections(ECCables.block().get().defaultBlockState(), state), Block.UPDATE_ALL);
		return cover.isAir()? ItemStack.EMPTY: new ItemStack(cover.getBlock());
	}

	private static BlockState withConnections(BlockState state, BlockState from)
	{
		for(BooleanProperty connection : CONNECTIONS.values())
			state = state.setValue(connection, from.getValue(connection));
		return state;
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		if(!player.isSecondaryUseActive()||!(stack.getItem() instanceof BlockItem item)
				||!level.getBlockState(pos).is(ECCables.block().get()))
			return;
		BlockState cover = item.getBlock().getStateForPlacement(new BlockPlaceContext(player, event.getHand(), stack,
				event.getHitVec()));
		if(cover==null)
			cover = item.getBlock().defaultBlockState();
		if(!isValidCover(cover, level, pos))
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
