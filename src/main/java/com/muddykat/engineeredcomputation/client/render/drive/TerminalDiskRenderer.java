package com.muddykat.engineeredcomputation.client.render.drive;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import com.muddykat.engineeredcomputation.client.render.AnimationClock;
import com.muddykat.engineeredcomputation.registration.ECDataCells;
import com.muddykat.engineeredcomputation.storage.cell.CellStatus;
import com.muddykat.engineeredcomputation.storage.cell.EraDataCell;
import com.muddykat.engineeredcomputation.terminal.floppy.TerminalDiskBlockEntity;

public class TerminalDiskRenderer implements BlockEntityRenderer<TerminalDiskBlockEntity>
{
	static final List<ResourceLocation> DISKS = IntStream.rangeClosed(1, ECDataCells.terminalDisks().size())
			.mapToObj(tier -> DriveRenderer.id("block/floppy_35_disk_in_bay_"+tier)).toList();
	static final ResourceLocation LIGHT = DriveRenderer.id("block/floppy_35_bay_light");
	private static final float BAY_STEP = -8/16F;
	private static final float SLIDE = -2/16F;

	private final FloppyBays floppyBays = new FloppyBays();

	public TerminalDiskRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	public static List<ModelResourceLocation> models()
	{
		return Stream.concat(DISKS.stream(), Stream.of(LIGHT)).map(ModelResourceLocation::standalone).toList();
	}

	@Override
	public void render(TerminalDiskBlockEntity bays, float partialTick, PoseStack pose, MultiBufferSource buffers,
			int packedLight, int packedOverlay)
	{
		if(bays.getLevel()==null)
			return;
		List<BakedQuad> light = quads(LIGHT);
		VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
		int rotation = DriveRenderer.yRotation(bays.getBlockState().getValue(HorizontalDirectionalBlock.FACING));
		int filled = 0;
		for(int slot = 0; slot<TerminalDiskBlockEntity.SLOTS; slot++)
			if(!bays.getDisks().getStackInSlot(slot).isEmpty())
				filled |= 1<<slot;
		float[] out = floppyBays.slideOut(bays, filled, TerminalDiskBlockEntity.SLOTS,
				AnimationClock.now(bays.getLevel(), partialTick));
		int[] seen = new int[TerminalDiskBlockEntity.SLOTS];
		for(int slot = 0; slot<TerminalDiskBlockEntity.SLOTS; slot++)
			seen[slot] = tierOf(bays.getDisks().getStackInSlot(slot));
		int[] tiers = floppyBays.tiers(bays, seen);
		for(int slot = 0; slot<TerminalDiskBlockEntity.SLOTS; slot++)
		{
			pose.pushPose();
			pose.mulPose(BlockModelRotation.by(0, rotation).getRotation().blockCenterToCorner().getMatrix());
			pose.translate(BAY_STEP*slot, 0, 0);
			CellStatus status = CellStatus.of(bays.getDisks().getStackInSlot(slot));
			int colour = FloppyBays.lightColour(status);
			for(BakedQuad quad : light)
				buffer.putBulkData(pose.last(), quad, (colour>>16&0xFF)/255F, (colour>>8&0xFF)/255F,
						(colour&0xFF)/255F, 1, FloppyBays.lightLevel(status, packedLight), packedOverlay);
			if(out[slot]!=FloppyBays.HIDDEN)
			{
				pose.translate(0, 0, SLIDE*out[slot]);
				for(BakedQuad quad : quads(DISKS.get(tiers[slot])))
					buffer.putBulkData(pose.last(), quad, 1, 1, 1, 1, packedLight, packedOverlay);
			}
			pose.popPose();
		}
	}

	private static int tierOf(ItemStack stack)
	{
		List<DeferredItem<EraDataCell>> disks = ECDataCells.terminalDisks();
		for(int tier = 0; tier<disks.size(); tier++)
			if(stack.is(disks.get(tier).get()))
				return tier;
		return -1;
	}

	private static List<BakedQuad> quads(ResourceLocation model)
	{
		return DriveRenderer.allQuads(Minecraft.getInstance().getModelManager().getModel(
				ModelResourceLocation.standalone(model)));
	}
}
