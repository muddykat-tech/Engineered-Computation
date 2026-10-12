package com.muddykat.engineeredcomputation.client.render;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.adapter.ProgramEntry;
import com.muddykat.engineeredcomputation.catalogue.FilterCardItem;
import com.muddykat.engineeredcomputation.client.gui.ECGui;

/**
 * A Filter Card: the card, blank or punched, and in a GUI its filter drawn small in the bottom right of its icon (a
 * tag's items in turn). Forge 1.18.2 has no item decorations, so the card's item model hands its drawing to this.
 */
public final class FilterCardRenderer extends BlockEntityWithoutLevelRenderer
{
	public static final ResourceLocation BLANK =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "item/filter_card_blank");
	public static final ResourceLocation PUNCHED =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "item/filter_card_punched");
	private static final float SCALE = 0.5F;
	/** Made on first use: it needs the client's renderers, which don't exist yet when items are registered. */
	public static final Lazy<FilterCardRenderer> INSTANCE = Lazy.of(FilterCardRenderer::new);

	public FilterCardRenderer()
	{
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	public static List<ResourceLocation> models()
	{
		return List.of(BLANK, PUNCHED);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemTransforms.TransformType transform, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay)
	{
		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer items = minecraft.getItemRenderer();
		ProgramEntry filter = FilterCardItem.filter(stack);
		BakedModel card = minecraft.getModelManager().getModel(filter.isEmpty()? BLANK: PUNCHED);
		items.renderModelLists(card, stack, light, overlay, pose, ItemRenderer.getFoilBufferDirect(buffers,
				ItemBlockRenderTypes.getRenderType(stack, true), true, stack.hasFoil()));
		ItemStack icon = filter.isEmpty()||transform!=ItemTransforms.TransformType.GUI? ItemStack.EMPTY:
				ECGui.entryIcon(filter);
		if(icon.isEmpty())
			return;
		BakedModel model = items.getModel(icon, null, null, 0);
		// The card is lit flat, as items are; a block's icon needs the 3D light it's normally drawn with.
		boolean blockLight = model.usesBlockLight()&&buffers instanceof MultiBufferSource.BufferSource;
		if(blockLight)
		{
			((MultiBufferSource.BufferSource)buffers).endBatch();
			Lighting.setupFor3DItems();
		}
		pose.pushPose();
		// Model space here is the icon's 0..1 square, y up: the middle of its bottom-right quarter, in front of it.
		pose.translate(0.75, 0.25, 1);
		pose.scale(SCALE, SCALE, SCALE);
		items.render(icon, ItemTransforms.TransformType.GUI, false, pose, buffers, light, overlay, model);
		pose.popPose();
		if(blockLight)
		{
			((MultiBufferSource.BufferSource)buffers).endBatch();
			Lighting.setupForFlatItems();
		}
	}
}
