package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;

public class CoveredCableModel extends BakedModelWrapper<BakedModel>
{
	private final boolean showsCable;
	/** The chunk layer the cable's own quads draw in; the block draws in every layer, for whatever its cover needs. */
	private final RenderType ownLayer;

	public CoveredCableModel(BakedModel cable, boolean showsCable, RenderType ownLayer)
	{
		super(cable);
		this.showsCable = showsCable;
		this.ownLayer = ownLayer;
	}

	@Nullable
	private static BakedModel coverModel(@Nullable BlockState cover)
	{
		return cover==null||cover.isAir()? null: Minecraft.getInstance().getBlockRenderer().getBlockModel(cover);
	}

	/** The layer being drawn, or null outside chunk rendering (all layers at once). */
	@Nullable
	static RenderType layer()
	{
		return MinecraftForgeClient.getRenderType();
	}

	/**
	 * The block entity's data when the caller passes none: 1.18.2 draws the breaking overlay without model data, so
	 * the cracks would otherwise land only on the cable hidden inside the cover.
	 */
	@Override
	public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData data)
	{
		if(data.getData(CoveredCableBlockEntity.COVER)==null
				&&level.getBlockEntity(pos) instanceof CoveredCableBlockEntity covered)
			return covered.getModelData();
		return super.getModelData(level, pos, state, data);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand,
			IModelData data)
	{
		RenderType layer = layer();
		BlockState cover = data.getData(CoveredCableBlockEntity.COVER);
		BakedModel coverModel = coverModel(cover);
		if(coverModel==null)
			return layer==null||layer==ownLayer? super.getQuads(state, side, rand, data): List.of();
		List<BakedQuad> quads = new ArrayList<>();
		if(layer==null||ItemBlockRenderTypes.canRenderInLayer(cover, layer))
			quads.addAll(coverModel.getQuads(cover, side, rand, EmptyModelData.INSTANCE));
		if(showsCable&&(layer==null||layer==ownLayer))
			quads.addAll(super.getQuads(state, side, rand, data));
		return quads;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(IModelData data)
	{
		BakedModel coverModel = coverModel(data.getData(CoveredCableBlockEntity.COVER));
		return coverModel==null? super.getParticleIcon(data): coverModel.getParticleIcon(EmptyModelData.INSTANCE);
	}
}
