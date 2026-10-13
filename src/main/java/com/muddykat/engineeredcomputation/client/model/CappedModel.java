package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.connector.ConnectorBlockEntity;

public class CappedModel extends BakedModelWrapper<BakedModel>
{
	private final String caps;

	public CappedModel(BakedModel model, String caps)
	{
		super(model);
		this.caps = caps;
	}

	public static final String TUBE = "block/tube/cut_cap_%s";
	public static final String CABLE = "block/network_cable_cap_%s";

	public static List<ModelResourceLocation> models()
	{
		return Stream.of(TUBE, CABLE).flatMap(caps -> Arrays.stream(Direction.values()).map(face -> cap(caps, face)))
				.toList();
	}

	private static ModelResourceLocation cap(String caps, Direction face)
	{
		return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID,
				caps.formatted(face.getSerializedName())));
	}

	private static int cut(ModelData data)
	{
		Integer cut = data.get(ConnectorBlockEntity.CUT);
		return cut==null? 0: cut;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
			ModelData data, @Nullable RenderType renderType)
	{
		List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
		int cut = cut(data);
		if(cut==0||side!=null||renderType!=null&&renderType!=RenderType.solid())
			return quads;
		quads = new ArrayList<>(quads);
		for(Direction face : Direction.values())
			if((cut&1<<face.ordinal())!=0)
				quads.addAll(Minecraft.getInstance().getModelManager().getModel(cap(caps, face))
						.getQuads(state, null, rand, ModelData.EMPTY, RenderType.solid()));
		return quads;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
	{
		ChunkRenderTypeSet types = super.getRenderTypes(state, rand, data);
		return cut(data)==0? types: ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(RenderType.solid()));
	}
}
