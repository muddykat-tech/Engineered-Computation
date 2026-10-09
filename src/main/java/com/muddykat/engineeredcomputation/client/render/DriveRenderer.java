package com.muddykat.engineeredcomputation.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.era.ComputerEra;
import com.muddykat.engineeredcomputation.crafting.CraftingComputerState;
import com.muddykat.engineeredcomputation.crafting.DataProcessorState;
import com.muddykat.engineeredcomputation.era.EraDrive;
import com.muddykat.engineeredcomputation.era.NetworkPort;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockContext;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockOrientation;
import com.muddykat.engineeredcomputation.multiblock.core.MultiblockPartBlockEntity;
import com.muddykat.engineeredcomputation.multiblock.era.EraDriveState;
import com.muddykat.engineeredcomputation.network.ECCapabilities;
import com.muddykat.engineeredcomputation.registration.ECCatalogue;
import com.muddykat.engineeredcomputation.registration.ECMultiblocks;
import com.muddykat.engineeredcomputation.storage.CellStatus;
import com.muddykat.engineeredcomputation.storage.ECDataCells;

public class DriveRenderer implements BlockEntityRenderer<MultiblockPartBlockEntity>
{
	static final List<ResourceLocation> FLOPPY_DISKS = tiered("block/era5_floppy_workstation_disk", ComputerEra.FLOPPY);
	static final ResourceLocation FLOPPY_LIGHT = id("block/era5_floppy_workstation_light");
	static final List<ResourceLocation> BANK_FLOPPY_DISKS = tiered("block/era5_floppy_bank_disk", ComputerEra.FLOPPY);
	static final ResourceLocation BANK_FLOPPY_LIGHT = id("block/era5_floppy_bank_light");
	private static final int BANK_MODELLED_SLOT = 2;
	static final ResourceLocation COMPUTER_DISK = id("block/era5_crafting_computer_disk_1");
	static final ResourceLocation COMPUTER_LIGHT = id("block/era5_crafting_computer_light");
	private static final float COMPUTER_BAY_STEP = 10/16F;
	private static final float COMPUTER_COLUMN_STEP = 32/16F;
	private static final int COMPUTER_COLUMN_BAYS = 4;

	static final List<ResourceLocation> CARD_DECKS = tiered("block/era1_card_deck", ECCatalogue.DECK_TIERS);
	private static final float DECK_STEP_Y = 3/16F;
	private static final float DECK_STEP_Z = 6/16F;
	private static final float DECK_DROP = 8/16F;

	static final List<ResourceLocation> BANK_DRUMS = tiered("block/era2_drum_bank_drum", ComputerEra.DRUM);
	private static final float DRUM_STEP_Y = 9/16F;
	private static final float DRUM_AXIS_Y = 23/16F;
	private static final float DRUM_AXIS_Z = 1+4/16F;
	private static final float DRUM_SLIDE = 12/16F;
	private static final float DRUM_TURNS = 0.5F;

	static final ResourceLocation PROCESSOR_LAMP = id("block/era5_data_processor_light");
	private static final float PROCESSOR_LAMP_STEP = 2/16F;

	private static final float FLOPPY_BAY_STEP = 9/16F;
	private static final float FLOPPY_SLIDE = 5/16F;

	private static final int RENDER_REACH = 5;

	private final DiskPackAnimation diskPack = new DiskPackAnimation();
	private final TapeReels tapeReels = new TapeReels();
	private final FloppyBays floppyBays = new FloppyBays();
	private final Map<BakedModel, Vector3f> centres = new WeakHashMap<>();

	public DriveRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	public static List<ModelResourceLocation> models()
	{
		return Stream.of(FLOPPY_DISKS.stream(), BANK_FLOPPY_DISKS.stream(), CARD_DECKS.stream(), BANK_DRUMS.stream(),
						Stream.of(FLOPPY_LIGHT, BANK_FLOPPY_LIGHT, COMPUTER_DISK, COMPUTER_LIGHT, PROCESSOR_LAMP),
						DiskPackAnimation.models().stream())
				.flatMap(models -> models)
				.map(ModelResourceLocation::standalone).toList();
	}

	@Override
	public void render(
			MultiblockPartBlockEntity part, float partialTick, PoseStack pose, MultiBufferSource buffers,
			int packedLight, int packedOverlay)
	{
		if(!part.isMaster()||part.getLevel()==null)
			return;
		if(part.getDefinition()==ECMultiblocks.CRAFTING_COMPUTER)
		{
			drawComputerBays(part, partialTick, pose, buffers, packedOverlay);
			return;
		}
		if(part.getDefinition()==ECMultiblocks.DATA_PROCESSOR)
		{
			drawProcessorLamps(part, pose, buffers, packedOverlay);
			return;
		}
		EraDrive drive = ECMultiblocks.driveOf(part.getDefinition());
		int filled = part.getClientSyncData().getInt(EraDriveState.FILLED_SLOTS);
		switch(drive)
		{
			case FLOPPY_WORKSTATION -> drawFloppies(part, drive, FLOPPY_DISKS, FLOPPY_LIGHT, 0, filled, partialTick, pose,
					buffers, packedOverlay);
			case FLOPPY_BANK -> drawFloppies(part, drive, BANK_FLOPPY_DISKS, BANK_FLOPPY_LIGHT, BANK_MODELLED_SLOT, filled,
					partialTick, pose, buffers, packedOverlay);
			case DISK_MAINFRAME -> diskPack.render(this, part, DiskPackAnimation.MAINFRAME, filled!=0,
					isNetworked(part, drive), partialTick, pose, buffers, packedOverlay);
			case DISK_BANK -> diskPack.render(this, part, DiskPackAnimation.BANK, filled!=0, isNetworked(part, drive),
					partialTick, pose, buffers, packedOverlay);
			case TAPE_DRIVE -> tapeReels.render(part, isNetworked(part, drive), partialTick, pose, buffers, packedOverlay);
			case PUNCHCARD_VAULT -> drawDecks(part, drive, filled, partialTick, pose, buffers, packedOverlay);
			case DRUM_BANK -> drawDrums(part, drive, filled, partialTick, pose, buffers, packedOverlay);
			case null, default ->
			{
			}
		}
	}

	private void drawFloppies(MultiblockPartBlockEntity part, EraDrive drive, List<ResourceLocation> disks,
			ResourceLocation light, int modelledSlot, int filled, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay)
	{
		int status = part.getClientSyncData().getInt(EraDriveState.CELL_STATUS);
		int tierBits = part.getClientSyncData().getInt(EraDriveState.CELL_TIERS);
		float[] out = floppyBays.slideOut(part, filled, drive.slots(), part.getLevel().getGameTime()+partialTick);
		int[] seen = new int[drive.slots()];
		for(int slot = 0; slot<drive.slots(); slot++)
			seen[slot] = (tierBits>>4*slot&0xF)-1;
		int[] tiers = floppyBays.tiers(part, seen);
		for(int slot = 0; slot<drive.slots(); slot++)
		{
			float bay = FLOPPY_BAY_STEP*(slot-modelledSlot);
			if(out[slot]!=FloppyBays.HIDDEN)
				draw(part, disks.get(Math.min(tiers[slot], disks.size()-1)), new Matrix4f().translation(0, bay, FLOPPY_SLIDE*out[slot]), pose, buffers,
						packedOverlay);
			CellStatus cell = CellStatus.unpack(status, slot);
			draw(part, light, new Matrix4f().translation(0, bay, 0), pose, buffers, packedOverlay,
					quad -> QuadStyle.SOLID, 1, FloppyBays.lightColour(cell), packedLight -> FloppyBays.lightLevel(cell,
							packedLight));
		}
	}

	private void drawDecks(MultiblockPartBlockEntity part, EraDrive drive, int filled, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay)
	{
		int tierBits = part.getClientSyncData().getInt(EraDriveState.CELL_TIERS);
		float[] out = floppyBays.slideOut(part, filled, drive.slots(), part.getLevel().getGameTime()+partialTick);
		int[] seen = new int[drive.slots()];
		for(int slot = 0; slot<drive.slots(); slot++)
			seen[slot] = (tierBits>>4*slot&0xF)-1;
		int[] tiers = floppyBays.tiers(part, seen);
		for(int slot = 0; slot<drive.slots(); slot++)
			if(out[slot]!=FloppyBays.HIDDEN)
				draw(part, CARD_DECKS.get(Mth.clamp(tiers[slot], 0, CARD_DECKS.size()-1)), new Matrix4f().translation(0,
						DECK_STEP_Y*slot+DECK_DROP*out[slot], -DECK_STEP_Z*slot), pose, buffers, packedOverlay);
	}

	private void drawDrums(MultiblockPartBlockEntity part, EraDrive drive, int filled, float partialTick,
			PoseStack pose, MultiBufferSource buffers, int packedOverlay)
	{
		int tierBits = part.getClientSyncData().getInt(EraDriveState.CELL_TIERS);
		double now = part.getLevel().getGameTime()+partialTick;
		float[] out = floppyBays.slideOut(part, filled, drive.slots(), now);
		int[] seen = new int[drive.slots()];
		for(int slot = 0; slot<drive.slots(); slot++)
			seen[slot] = (tierBits>>4*slot&0xF)-1;
		int[] tiers = floppyBays.tiers(part, seen);
		float turn = isNetworked(part, drive)? (float)(now/20*DRUM_TURNS%1*Mth.TWO_PI): 0;
		for(int slot = 0; slot<drive.slots(); slot++)
			if(out[slot]!=FloppyBays.HIDDEN)
				draw(part, BANK_DRUMS.get(Mth.clamp(tiers[slot], 0, BANK_DRUMS.size()-1)), new Matrix4f()
						.translation(0, DRUM_STEP_Y*slot, DRUM_SLIDE*out[slot])
						.translate(0, DRUM_AXIS_Y, DRUM_AXIS_Z).rotateX(turn).translate(0, -DRUM_AXIS_Y, -DRUM_AXIS_Z),
						pose, buffers, packedOverlay);
	}

	private void drawProcessorLamps(MultiblockPartBlockEntity part, PoseStack pose, MultiBufferSource buffers,
			int packedOverlay)
	{
		int filled = part.getClientSyncData().getInt(DataProcessorState.FILLED);
		CellStatus lit = CellStatus.SPACE;
		for(int slot = 0; slot<DataProcessorState.PATTERN_SLOTS; slot++)
			if((filled&1<<slot)!=0)
				draw(part, PROCESSOR_LAMP, new Matrix4f().translation(PROCESSOR_LAMP_STEP*(slot%3),
						-PROCESSOR_LAMP_STEP*(slot/3), 0), pose, buffers, packedOverlay, quad -> QuadStyle.SOLID, 1,
						FloppyBays.lightColour(lit), packedLight -> FloppyBays.lightLevel(lit, packedLight));
	}

	private void drawComputerBays(MultiblockPartBlockEntity part, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay)
	{
		int filled = part.getClientSyncData().getInt(CraftingComputerState.FILLED);
		int bays = 2*COMPUTER_COLUMN_BAYS;
		float[] out = floppyBays.slideOut(part, filled, bays, part.getLevel().getGameTime()+partialTick);
		for(int slot = 0; slot<bays; slot++)
		{
			Matrix4f bay = new Matrix4f().translation(slot<COMPUTER_COLUMN_BAYS? 0: COMPUTER_COLUMN_STEP,
					-COMPUTER_BAY_STEP*(slot%COMPUTER_COLUMN_BAYS), 0);
			if(out[slot]!=FloppyBays.HIDDEN)
				draw(part, COMPUTER_DISK, new Matrix4f(bay).translate(0, 0, FLOPPY_SLIDE*out[slot]), pose, buffers,
						packedOverlay);
			CellStatus light = (filled&1<<slot)!=0? CellStatus.SPACE: CellStatus.EMPTY;
			draw(part, COMPUTER_LIGHT, bay, pose, buffers, packedOverlay, quad -> QuadStyle.SOLID, 1,
					FloppyBays.lightColour(light), packedLight -> FloppyBays.lightLevel(light, packedLight));
		}
	}

	void draw(MultiblockPartBlockEntity part, ResourceLocation modelId, Matrix4f local, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay)
	{
		draw(part, modelId, local, pose, buffers, packedOverlay, quad -> QuadStyle.SOLID, 1);
	}

	enum QuadStyle
	{
		SOLID,
		SEE_THROUGH,
		HIDDEN
	}

	void draw(MultiblockPartBlockEntity part, ResourceLocation modelId, Matrix4f local, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay, Function<BakedQuad, QuadStyle> style, float alpha)
	{
		draw(part, modelId, local, pose, buffers, packedOverlay, style, alpha, 0xFFFFFF, IntUnaryOperator.identity());
	}

	private void draw(MultiblockPartBlockEntity part, ResourceLocation modelId, Matrix4f local, PoseStack pose,
			MultiBufferSource buffers, int packedOverlay, Function<BakedQuad, QuadStyle> style, float alpha, int colour,
			IntUnaryOperator lighting)
	{
		float red = (colour>>16&0xFF)/255F, green = (colour>>8&0xFF)/255F, blue = (colour&0xFF)/255F;
		Minecraft mc = Minecraft.getInstance();
		BakedModel model = mc.getModelManager().getModel(ModelResourceLocation.standalone(modelId));
		Direction facing = part.getFacing();

		Vector3f centre = centres.computeIfAbsent(model, DriveRenderer::centreOf);
		Vector4f moved = local.transform(new Vector4f(centre, 1));
		int light = lighting.applyAsInt(lightAt(part, new Vector3f(moved.x(), moved.y(), moved.z())));

		pose.pushPose();
		rotate(pose, facing);
		pose.mulPose(local);
		List<BakedQuad> quads = allQuads(model);
		VertexConsumer solid = buffers.getBuffer(Sheets.cutoutBlockSheet());
		for(BakedQuad quad : quads)
			if(style.apply(quad)==QuadStyle.SOLID)
				solid.putBulkData(pose.last(), quad, red, green, blue, 1, light, packedOverlay);
		if(quads.stream().anyMatch(quad -> style.apply(quad)==QuadStyle.SEE_THROUGH))
		{
			VertexConsumer translucent = buffers.getBuffer(Sheets.translucentCullBlockSheet());
			for(BakedQuad quad : quads)
				if(style.apply(quad)==QuadStyle.SEE_THROUGH)
					translucent.putBulkData(pose.last(), quad, red, green, blue, alpha, light, packedOverlay);
		}
		pose.popPose();
	}

	static void rotate(PoseStack pose, Direction facing)
	{
		pose.mulPose(BlockModelRotation.by(0, yRotation(facing)).getRotation().blockCenterToCorner().getMatrix());
	}

	static int lightAt(MultiblockPartBlockEntity part, Vector3f point)
	{
		BlockPos cell = BlockPos.containing(point.x(), point.y(), point.z());
		return LevelRenderer.getLightColor(part.getLevel(),
				part.getBlockPos().offset(MultiblockOrientation.toWorldOffset(cell, part.getFacing())));
	}

	static boolean isNetworked(MultiblockPartBlockEntity master, EraDrive drive)
	{
		MultiblockContext context = master.getContext();
		for(NetworkPort port : drive.geometry().ports())
		{
			Direction side = MultiblockOrientation.rotationFor(context.facing()).rotate(port.side());
			BlockPos neighbour = context.toAbsolute(port.pos()).relative(side);
			if(master.getLevel().getCapability(ECCapabilities.NETWORK_MEMBER, neighbour, side.getOpposite())!=null)
				return true;
		}
		return false;
	}

	static int yRotation(Direction facing)
	{
		return switch(facing)
		{
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	static List<BakedQuad> allQuads(BakedModel model)
	{
		RandomSource random = RandomSource.create(42);
		List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random, ModelData.EMPTY, null));
		for(Direction side : Direction.values())
			quads.addAll(model.getQuads(null, side, random, ModelData.EMPTY, null));
		return quads;
	}

	private static Vector3f centreOf(BakedModel model)
	{
		Vector3f sum = new Vector3f();
		int count = 0;
		for(BakedQuad quad : allQuads(model))
			for(int vertex = 0; vertex<4; vertex++)
			{
				int base = vertex*IQuadTransformer.STRIDE+IQuadTransformer.POSITION;
				int[] data = quad.getVertices();
				sum.add(Float.intBitsToFloat(data[base]), Float.intBitsToFloat(data[base+1]),
						Float.intBitsToFloat(data[base+2]));
				count++;
			}
		return count==0? sum: sum.div(count);
	}

	@Override
	public AABB getRenderBoundingBox(MultiblockPartBlockEntity part)
	{
		return part.isMaster()? new AABB(part.getBlockPos()).inflate(RENDER_REACH): new AABB(part.getBlockPos());
	}

	static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, path);
	}

	private static List<ResourceLocation> tiered(String path, ComputerEra era)
	{
		return tiered(path, ECDataCells.cellsOf(era).size());
	}

	private static List<ResourceLocation> tiered(String path, int tiers)
	{
		return IntStream.rangeClosed(1, tiers).mapToObj(tier -> id(path+"_"+tier)).toList();
	}

	static float ease(float t)
	{
		t = Mth.clamp(t, 0, 1);
		return t*t*(3-2*t);
	}
}
