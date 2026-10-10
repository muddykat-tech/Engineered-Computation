package com.muddykat.engineeredcomputation.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.network.CoveredCableBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.PneumaticTubeBlock;

public class CoveredTubeModel extends CoveredCableModel
{
	private final boolean showsTube;

	public CoveredTubeModel(BakedModel tube, boolean showsTube)
	{
		super(tube, showsTube, RenderType.translucent());
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

	private boolean drawsCaps(@Nullable BlockState state, IModelData data)
	{
		BlockState cover = data.getData(CoveredCableBlockEntity.COVER);
		return !showsTube&&cover!=null&&!cover.isAir()&&state!=null&&state.getBlock() instanceof PneumaticTubeBlock;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand,
			IModelData data)
	{
		List<BakedQuad> quads = super.getQuads(state, side, rand, data);
		RenderType layer = layer();
		if(side!=null||!drawsCaps(state, data)||layer!=null&&layer!=RenderType.solid())
			return quads;
		quads = new ArrayList<>(quads);
		for(Map.Entry<Direction, BooleanProperty> connection : PneumaticTubeBlock.CONNECTIONS.entrySet())
			if(state.getValue(connection.getValue()))
				quads.addAll(Minecraft.getInstance().getModelManager().getModel(cap(connection.getKey()))
						.getQuads(state, null, rand, EmptyModelData.INSTANCE));
		return quads;
	}
}
