package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;

public class CoveredTubeModel extends CoveredCableModel
{
	private final boolean showsTube;

	public CoveredTubeModel(BakedModel tube, boolean showsTube)
	{
		super(tube, showsTube);
		this.showsTube = showsTube;
	}

	public static List<ResourceLocation> models()
	{
		return Arrays.stream(Direction.values()).map(CoveredTubeModel::cap).toList();
	}

	private static ResourceLocation cap(Direction face)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID,
				"block/tube/end_cap_"+face.getSerializedName());
	}

	private boolean drawsCaps(@Nullable BlockState state, ModelData data)
	{
		BlockState cover = data.get(CoveredCableBlockEntity.COVER);
		return !showsTube&&cover!=null&&!cover.isAir()&&state!=null&&state.getBlock() instanceof PneumaticTubeBlock;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
			ModelData data, @Nullable RenderType renderType)
	{
		List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
		if(side!=null||!drawsCaps(state, data)||renderType!=null&&renderType!=RenderType.solid())
			return quads;
		quads = new ArrayList<>(quads);
		for(Map.Entry<Direction, BooleanProperty> connection : PneumaticTubeBlock.CONNECTIONS.entrySet())
			if(state.getValue(connection.getValue()))
				quads.addAll(Minecraft.getInstance().getModelManager().getModel(cap(connection.getKey()))
						.getQuads(state, null, rand, ModelData.EMPTY, RenderType.solid()));
		return quads;
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
	{
		ChunkRenderTypeSet types = super.getRenderTypes(state, rand, data);
		return drawsCaps(state, data)? ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(RenderType.solid())): types;
	}
}
