package com.muddykat.engineeredcomputation.adapter;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.muddykat.engineeredcomputation.client.gui.ECGui;

public class DataAdapterScreen extends AbstractContainerScreen<DataAdapterMenu>
{
	private static final int STATUS_X = 42;
	private static final int STATUS_Y = 17;
	private static final int STATUS_WIDTH = 100;
	private static final int STATUS_HEIGHT = 26;
	private static final int ROW_Y = 48;
	private static final int ROW_STEP = 13;
	private static final int STEPPER_X = 58;
	private static final int BUTTON = 11;
	private static final int VALUE_WIDTH = 22;
	private static final float SMALL = 0.75F;
	private static final int POWER_WIDTH = 24;
	private static final int POWER_HEIGHT = 14;
	private static final int POWER_X = 176-8-POWER_WIDTH;
	private static final int POWER_Y = STATUS_Y+(STATUS_HEIGHT-POWER_HEIGHT)/2;
	private static final int MODE_X = DataAdapterMenu.FILTER_X-ECGui.FILTER_MODE_SIZE-2;
	private static final int MODE_Y = DataAdapterMenu.FILTER_Y+(18-ECGui.FILTER_MODE_SIZE)/2;

	public DataAdapterScreen(DataAdapterMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = DataAdapterMenu.INVENTORY_Y+58+18+6;
		this.inventoryLabelY = DataAdapterMenu.INVENTORY_Y-11;
	}

	private AdapterSpeed speed()
	{
		DataAdapterBlock block = menu.block();
		return block==null? new AdapterSpeed(1, 1): AdapterSpeed.of(block.getEra()).orElse(new AdapterSpeed(1, 1));
	}

	private boolean exporter()
	{
		DataAdapterBlock block = menu.block();
		return block!=null&&block.getKind()==AdapterKind.EXPORTER;
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		if(!renderOwnTooltip(pose, mouseX, mouseY))
			renderTooltip(pose, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(pose, x, y, imageWidth, imageHeight);
		for(Slot slot : menu.slots)
			ECGui.slot(pose, x+slot.x-1, y+slot.y-1);

		ECGui.sunken(pose, x+STATUS_X-1, y+STATUS_Y-1, STATUS_WIDTH+2, STATUS_HEIGHT+2);
		GuiComponent.fill(pose, x+STATUS_X, y+STATUS_Y, x+STATUS_X+STATUS_WIDTH, y+STATUS_Y+STATUS_HEIGHT, 0xFF101814);
		AdapterStatus status = menu.getStatus();
		drawFitted(pose, status.title(), x+STATUS_X+4, y+STATUS_Y+3, STATUS_WIDTH-8, status.colour());
		List<FormattedCharSequence> detail = font.split(status.detail(), (int)((STATUS_WIDTH-8)/SMALL));
		for(int line = 0; line<Math.min(2, detail.size()); line++)
			drawSmall(pose, detail.get(line), x+STATUS_X+4, y+STATUS_Y+13+line*6, 0xA0B8A8);

		ECGui.button(pose, x+POWER_X, y+POWER_Y, POWER_WIDTH, POWER_HEIGHT, isOverPower(mouseX, mouseY));
		Component power = new TranslatableComponent(menu.isEnabled()? "gui.engineered_computation.adapter.on"
				: "gui.engineered_computation.adapter.off");
		GuiComponent.drawCenteredString(pose, font, power, x+POWER_X+POWER_WIDTH/2+1, y+POWER_Y+3,
				menu.isEnabled()? 0x6CFF7C: 0xFF6A5A);

		ECGui.filterMode(pose, x+MODE_X, y+MODE_Y, menu.isBlocking(), isOverMode(mouseX, mouseY));

		AdapterSpeed speed = speed();
		stepper(pose, 0, menu.itemsPerMove(), mouseX, mouseY);
		stepper(pose, 1, menu.ticksBetween(), mouseX, mouseY);
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.adapter.max", speed.items()),
				x+STEPPER_X+2*BUTTON+VALUE_WIDTH+6, y+ROW_Y+2, 0x707070);
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.adapter.min", speed.ticks()),
				x+STEPPER_X+2*BUTTON+VALUE_WIDTH+6, y+ROW_Y+ROW_STEP+2, 0x707070);
	}

	private void stepper(PoseStack pose, int row, int value, int mouseX, int mouseY)
	{
		int x = leftPos+STEPPER_X;
		int y = topPos+ROW_Y+row*ROW_STEP;
		for(int side = 0; side<2; side++)
		{
			int buttonX = x+side*(BUTTON+VALUE_WIDTH);
			boolean hovered = isOver(mouseX, mouseY, buttonX-leftPos, y-topPos, BUTTON, BUTTON);
			ECGui.button(pose, buttonX, y, BUTTON, BUTTON, hovered);
			GuiComponent.drawCenteredString(pose, font, side==0? "-": "+", buttonX+BUTTON/2+1, y+2, 0xFFFFFF);
		}
		String text = Integer.toString(value);
		font.draw(pose, text, x+BUTTON+(VALUE_WIDTH-font.width(text))/2+1, y+2, ECGui.LABEL);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY)
	{
		super.renderLabels(pose, mouseX, mouseY);
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.adapter.items"), 8, ROW_Y+2, ECGui.LABEL);
		font.draw(pose, new TranslatableComponent("gui.engineered_computation.adapter.ticks"), 8, ROW_Y+ROW_STEP+2,
				ECGui.LABEL);
	}

	private boolean renderOwnTooltip(PoseStack pose, int mouseX, int mouseY)
	{
		List<Component> lines = new ArrayList<>();
		if(isOver(mouseX, mouseY, DataAdapterMenu.FILTER_X, DataAdapterMenu.FILTER_Y, 18, 18)&&menu.getCarried().isEmpty())
		{
			ItemStack filter = menu.getFilter();
			lines.add(new TranslatableComponent(menu.isBlocking()? "gui.engineered_computation.adapter.filter.block"
					: "gui.engineered_computation.adapter.filter").withStyle(ChatFormatting.YELLOW));
			if(filter.isEmpty())
				lines.add(new TranslatableComponent(menu.isBlocking()? exporter()?
						"gui.engineered_computation.adapter.filter.empty_block_exporter":
						"gui.engineered_computation.adapter.filter.empty_block_importer":
						exporter()? "gui.engineered_computation.adapter.filter.empty_exporter":
						"gui.engineered_computation.adapter.filter.empty_importer").withStyle(ChatFormatting.GRAY));
			else
			{
				lines.add(filter.getHoverName());
				if(AdapterFilter.isProgram(filter))
					filter.getItem().appendHoverText(filter, minecraft.level, lines,
							TooltipFlag.Default.NORMAL);
			}
			lines.add(new TranslatableComponent("gui.engineered_computation.adapter.filter.hint")
					.withStyle(ChatFormatting.DARK_GRAY));
		}
		else if(isOver(mouseX, mouseY, STATUS_X, STATUS_Y, STATUS_WIDTH, STATUS_HEIGHT))
		{
			lines.add(menu.getStatus().title());
			lines.add(menu.getStatus().detail().copy().withStyle(ChatFormatting.GRAY));
		}
		else if(isOverMode(mouseX, mouseY))
		{
			String mode = menu.isBlocking()? "block": "allow";
			lines.add(new TranslatableComponent("gui.engineered_computation.adapter.mode."+mode)
					.withStyle(menu.isBlocking()? ChatFormatting.RED: ChatFormatting.GREEN));
			lines.add(new TranslatableComponent("gui.engineered_computation.adapter.mode."+mode+"."
					+(exporter()? "exporter": "importer")).withStyle(ChatFormatting.GRAY));
			lines.add(new TranslatableComponent(menu.isBlocking()? "gui.engineered_computation.adapter.mode.to_allow"
					: "gui.engineered_computation.adapter.mode.to_block").withStyle(ChatFormatting.DARK_GRAY));
		}
		else if(isOverPower(mouseX, mouseY))
			lines.add(new TranslatableComponent(menu.isEnabled()? "gui.engineered_computation.adapter.switch_off"
					: "gui.engineered_computation.adapter.switch_on").withStyle(ChatFormatting.GRAY));
		else if(buttonAt(mouseX, mouseY)>=0)
			lines.add(new TranslatableComponent("gui.engineered_computation.adapter.step")
					.withStyle(ChatFormatting.GRAY));
		if(lines.isEmpty())
			return false;
		renderComponentTooltip(pose, lines, mouseX, mouseY);
		return true;
	}

	private void drawFitted(PoseStack pose, Component text, int x, int y, int width, int colour)
	{
		float scale = Math.min(1, width/(float)font.width(text));
		pose.pushPose();
		pose.translate(x, y+(font.lineHeight-1)*(1-scale)/2, 0);
		pose.scale(scale, scale, 1);
		font.draw(pose, text, 0, 0, colour);
		pose.popPose();
	}

	private void drawSmall(PoseStack pose, FormattedCharSequence text, int x, int y, int colour)
	{
		pose.pushPose();
		pose.translate(x, y, 0);
		pose.scale(SMALL, SMALL, 1);
		font.draw(pose, text, 0, 0, colour);
		pose.popPose();
	}

	private int buttonAt(double mouseX, double mouseY)
	{
		for(int row = 0; row<2; row++)
			for(int side = 0; side<2; side++)
				if(isOver(mouseX, mouseY, STEPPER_X+side*(BUTTON+VALUE_WIDTH), ROW_Y+row*ROW_STEP, BUTTON, BUTTON))
					return row*2+side;
		return -1;
	}

	private boolean isOverMode(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, MODE_X, MODE_Y, ECGui.FILTER_MODE_SIZE, ECGui.FILTER_MODE_SIZE);
	}

	private boolean isOverPower(double mouseX, double mouseY)
	{
		return isOver(mouseX, mouseY, POWER_X, POWER_Y, POWER_WIDTH, POWER_HEIGHT);
	}

	private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height)
	{
		double relX = mouseX-leftPos-x, relY = mouseY-topPos-y;
		return relX>=0&&relY>=0&&relX<width&&relY<height;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		int id = isOverPower(mouseX, mouseY)? DataAdapterMenu.TOGGLE_POWER:
				isOverMode(mouseX, mouseY)? DataAdapterMenu.TOGGLE_FILTER_MODE: buttonAt(mouseX, mouseY);
		if(id>=0&&button==GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			if(id<DataAdapterMenu.BIG&&hasShiftDown())
				id += DataAdapterMenu.BIG;
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
