package com.muddykat.engineeredcomputation.client.model;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;

public final class BakedSplitModel implements BakedModel
{
	private final BakedModel whole;
	private final Map<Vec3i, List<BakedQuad>> pieces;
	private final ItemTransforms transforms;

	BakedSplitModel(BakedModel whole, Set<Vec3i> parts, Transformation rotation, ItemTransforms transforms)
	{
		this.whole = whole;
		this.transforms = transforms;
		this.pieces = Map.copyOf(QuadSplitter.split(
				whole.getQuads(null, null, new Random(42), EmptyModelData.INSTANCE), parts, rotation));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData data)
	{
		BlockPos offset = data.getData(MultiblockPartBlockEntity.MODEL_OFFSET);
		if(side!=null||offset==null)
			return List.of();
		return pieces.getOrDefault(offset, List.of());
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
	{
		return getQuads(state, side, rand, EmptyModelData.INSTANCE);
	}

	@Override
	public boolean useAmbientOcclusion()
	{
		return whole.useAmbientOcclusion();
	}

	@Override
	public boolean isGui3d()
	{
		return whole.isGui3d();
	}

	@Override
	public boolean usesBlockLight()
	{
		return whole.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer()
	{
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon()
	{
		return whole.getParticleIcon();
	}

	@Override
	public TextureAtlasSprite getParticleIcon(IModelData data)
	{
		return whole.getParticleIcon(data);
	}

	@Override
	public ItemTransforms getTransforms()
	{
		return transforms;
	}

	@Override
	public ItemOverrides getOverrides()
	{
		return ItemOverrides.EMPTY;
	}
}
