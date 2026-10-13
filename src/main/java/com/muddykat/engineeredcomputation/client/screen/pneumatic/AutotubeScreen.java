package com.muddykat.engineeredcomputation.client.screen.pneumatic;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeBlockEntity;
import com.muddykat.engineeredcomputation.pneumatic.autotube.AutotubeMenu;

public class AutotubeScreen extends AbstractContainerScreen<AutotubeMenu>
{
	private static final int LABEL_Y = 20;
	private static final int TRACK_X = 8;
	private static final int TRACK_Y = 38;
	private static final int TRACK_WIDTH = 160;
	private static final int TRACK_HEIGHT = 4;
	private static final int HANDLE_WIDTH = 7;
	private static final int HANDLE_HEIGHT = 14;
	private static final int FILL = 0xFFCDA54B;
	private static final int SPLIT_Y = 52;
	private static final int SWITCH_WIDTH = 24;
	private static final int SWITCH_HEIGHT = 14;
	private static final int TOOLTIP_WIDTH = 200;

	/** Shown before the server replies, so dragging feels instant; 0 when not dragging. */
	private int dragged;

	public AutotubeScreen(AutotubeMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 72;
	}

	private int amount()
	{
		return dragged>0? dragged: menu.getAmount();
	}

	private int handleX(int amount)
	{
		return leftPos+TRACK_X+(amount-1)*(TRACK_WIDTH-HANDLE_WIDTH)/(AutotubeBlockEntity.MAX_AMOUNT-1);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		ECGui.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		int amount = amount();
		graphics.drawString(font, Component.translatable("gui.engineered_computation.autotube.amount"), leftPos+8,
				topPos+LABEL_Y, ECGui.LABEL, false);
		String count = Integer.toString(amount);
		graphics.drawString(font, count, leftPos+imageWidth-8-font.width(count), topPos+LABEL_Y, ECGui.LABEL, false);
		int trackY = topPos+TRACK_Y;
		ECGui.sunken(graphics, leftPos+TRACK_X, trackY, TRACK_WIDTH, TRACK_HEIGHT);
		int handleX = handleX(amount);
		graphics.fill(leftPos+TRACK_X+1, trackY+1, handleX+HANDLE_WIDTH/2, trackY+TRACK_HEIGHT-1, FILL);
		ECGui.button(graphics, handleX, trackY+TRACK_HEIGHT/2-HANDLE_HEIGHT/2, HANDLE_WIDTH, HANDLE_HEIGHT,
				dragged>0||isOverBar(mouseX, mouseY));
		graphics.drawString(font, Component.translatable("gui.engineered_computation.autotube.split"), leftPos+8,
				topPos+SPLIT_Y+3, ECGui.LABEL, false);
		int switchX = leftPos+imageWidth-8-SWITCH_WIDTH;
		boolean on = menu.splits();
		ECGui.button(graphics, switchX, topPos+SPLIT_Y, SWITCH_WIDTH, SWITCH_HEIGHT, isOverSwitch(mouseX, mouseY));
		graphics.drawCenteredString(font, Component.translatable(on? "gui.engineered_computation.adapter.on"
				: "gui.engineered_computation.adapter.off"), switchX+SWITCH_WIDTH/2+1, topPos+SPLIT_Y+3,
				on? 0x6CFF7C: 0xFF6A5A);
	}

	private boolean isOverSwitch(double mouseX, double mouseY)
	{
		double x = mouseX-(leftPos+imageWidth-8-SWITCH_WIDTH), y = mouseY-topPos-SPLIT_Y;
		return x>=0&&x<SWITCH_WIDTH&&y>=0&&y<SWITCH_HEIGHT;
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		if(isOverSwitch(mouseX, mouseY))
			graphics.renderTooltip(font, font.split(Component.translatable(
					"gui.engineered_computation.autotube.split.detail").withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH),
					mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	private boolean isOverBar(double mouseX, double mouseY)
	{
		double relX = mouseX-leftPos-TRACK_X, relY = mouseY-topPos-TRACK_Y-TRACK_HEIGHT/2.0;
		return relX>=0&&relX<TRACK_WIDTH&&Math.abs(relY)<=HANDLE_HEIGHT/2.0;
	}

	private int amountAt(double mouseX)
	{
		double fraction = (mouseX-leftPos-TRACK_X-HANDLE_WIDTH/2.0)/(TRACK_WIDTH-HANDLE_WIDTH);
		return Mth.clamp(1+(int)Math.round(fraction*(AutotubeBlockEntity.MAX_AMOUNT-1)), 1,
				AutotubeBlockEntity.MAX_AMOUNT);
	}

	private void set(int amount)
	{
		if(amount==amount())
			return;
		if(dragged>0)
			dragged = amount;
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, amount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverSwitch(mouseX, mouseY))
		{
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AutotubeMenu.TOGGLE_SPLIT);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
			return true;
		}
		if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&isOverBar(mouseX, mouseY))
		{
			dragged = menu.getAmount();
			set(amountAt(mouseX));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		if(dragged>0)
		{
			set(amountAt(mouseX));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(dragged>0)
		{
			dragged = 0;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		if(scrollY!=0&&isOverBar(mouseX, mouseY))
		{
			set(Mth.clamp(amount()+(int)Math.signum(scrollY), 1, AutotubeBlockEntity.MAX_AMOUNT));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
