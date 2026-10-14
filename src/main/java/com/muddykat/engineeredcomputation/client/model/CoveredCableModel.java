package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import com.muddykat.engineeredcomputation.network.cable.CoveredCableBlockEntity;

public class CoveredCableModel extends BakedModelWrapper<BakedModel>
{
	private final boolean showsCable;

	public CoveredCableModel(BakedModel cable, boolean showsCable)
	{
		super(cable);
		this.showsCable = showsCable;
	}

	@Nullable
	private static BakedModel coverModel(@Nullable BlockState cover)
	{
		return cover==null||cover.isAir()? null: Minecraft.getInstance().getBlockRenderer().getBlockModel(cover);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
			ModelData data, @Nullable RenderType renderType)
	{
		BlockState cover = data.get(CoveredCableBlockEntity.COVER);
		BakedModel coverModel = coverModel(cover);
		if(coverModel==null)
			return super.getQuads(state, side, rand, data, renderType);
		List<BakedQuad> quads = new ArrayList<>();
		if(renderType==null||coverModel.getRenderTypes(cover, rand, ModelData.EMPTY).contains(renderType))
			quads.addAll(coverModel.getQuads(cover, side, rand, ModelData.EMPTY, renderType));
		if(showsCable&&(renderType==null||super.getRenderTypes(state, rand, data).contains(renderType)))
			quads.addAll(super.getQuads(state, side, rand, data, renderType));
		return quads;
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data)
	{
		return super.getModelData(level, pos, state, BlockEntityModelData.orFromBlockEntity(level, pos, data, CoveredCableBlockEntity.COVER));
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
	{
		BlockState cover = data.get(CoveredCableBlockEntity.COVER);
		BakedModel coverModel = coverModel(cover);
		if(coverModel==null)
			return super.getRenderTypes(state, rand, data);
		ChunkRenderTypeSet covers = coverModel.getRenderTypes(cover, rand, ModelData.EMPTY);
		return showsCable? ChunkRenderTypeSet.union(covers, super.getRenderTypes(state, rand, data)): covers;
	}

	@Override
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
	{
		BlockState cover = data.get(CoveredCableBlockEntity.COVER);
		BakedModel coverModel = coverModel(cover);
		return coverModel==null? super.useAmbientOcclusion(state, data, renderType)
				: coverModel.useAmbientOcclusion(cover, ModelData.EMPTY, renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data)
	{
		BakedModel coverModel = coverModel(data.get(CoveredCableBlockEntity.COVER));
		return coverModel==null? super.getParticleIcon(data): coverModel.getParticleIcon(ModelData.EMPTY);
	}
}
