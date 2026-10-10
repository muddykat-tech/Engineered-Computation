package com.muddykat.engineeredcomputation.client.guide;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import com.muddykat.engineeredcomputation.EngineeredComputation;
import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.era.ComputerEra;

final class PalmGuideScreen extends GuideScreen
{
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(EngineeredComputation.MODID, "textures/gui/palm_pilot.png");
	private static final int WIDTH = 168;
	private static final int HEIGHT = 215;

	private static final int LCD_X = 14;
	private static final int LCD_Y = 20;
	private static final int LCD_WIDTH = 140;
	private static final int CONTENT_X = LCD_X+(LCD_WIDTH-GuidePage.WIDTH)/2;
	private static final int CONTENT_Y = LCD_Y+18;
	private static final int BAR_Y = CONTENT_Y+GuidePage.HEIGHT+5;
	private static final int BAR_HEIGHT = 13;

	private static final int INK = 0x1F2A1A;
	private static final int LINK = 0x27406E;
	private static final int LINK_HOVERED = 0x0E1C3A;
	private static final int FADED = 0x7C8970;
	private static final int SLOT_FACE = 0xFF9DAA8D;

	private static final int BUTTON_Y = 193;
	private static final int BUTTON_RADIUS = 9;
	private static final int POWER_X = 152;
	private static final int POWER_Y = 10;

	private enum Hardware
	{
		CONTENTS(24, "nav.contents"),
		BACK(52, "nav.back"),
		PREVIOUS(116, "nav.previous"),
		NEXT(144, "nav.next");

		final int x;
		final String key;

		Hardware(int x, String key)
		{
			this.x = x;
			this.key = key;
		}
	}

	private enum Soft
	{
		BACK, CONTENTS, PREVIOUS, NEXT
	}

	PalmGuideScreen(ComputerEra era)
	{
		super(era);
	}

	@Override
	protected int pagesShown()
	{
		return 1;
	}

	@Override
	protected int contentX(int index)
	{
		return left()+CONTENT_X;
	}

	@Override
	protected int contentY()
	{
		return top()+CONTENT_Y;
	}

	@Override
	int textColour()
	{
		return INK;
	}

	@Override
	int linkColour(boolean hovered)
	{
		return hovered? LINK_HOVERED: LINK;
	}

	@Override
	void drawSlot(PoseStack pose, int x, int y, int size)
	{
		GuiComponent.fill(pose, x, y, x+size, y+size, 0xFF000000|FADED);
		GuiComponent.fill(pose, x+1, y+1, x+size-1, y+size-1, SLOT_FACE);
	}

	private int left()
	{
		return (width-WIDTH)/2;
	}

	private int top()
	{
		return (height-HEIGHT)/2;
	}

	@Override
	public void renderBackground(PoseStack pose)
	{
		super.renderBackground(pose);
		ECGui.blit(pose, TEXTURE, left(), top(), 0, 0, WIDTH, HEIGHT);
	}

	@Override
	protected void renderControls(PoseStack pose, int mouseX, int mouseY)
	{
		int x = left()+LCD_X;
		int y = top()+LCD_Y;
		Component title = new TranslatableComponent("guide.engineered_computation.palm.tab");
		GuiComponent.fill(pose, x+2, y+3, x+8+font.width(title), y+14, 0xFF000000|INK);
		font.draw(pose, title, x+5, y+5, 0xA8B498);
		String count = (page+1)+"/"+guide.pages().size();
		font.draw(pose, count, x+LCD_WIDTH-4-font.width(count), y+5, INK);
		GuiComponent.fill(pose, x+2, y+14, x+LCD_WIDTH-2, y+15, 0xFF000000|INK);

		for(Soft button : Soft.values())
			drawSoftButton(pose, button, mouseX, mouseY);
		for(Hardware button : Hardware.values())
			if(isOverHardware(button, mouseX, mouseY))
				GuiComponent.fill(pose, left()+button.x-6, top()+BUTTON_Y-6, left()+button.x+6, top()+BUTTON_Y+6,
						0x30FFFFFF);
	}

	private void drawSoftButton(PoseStack pose, Soft button, int mouseX, int mouseY)
	{
		int[] box = softBox(button);
		boolean usable = softUsable(button);
		boolean hovered = usable&&isOver(box, mouseX, mouseY);
		int ink = 0xFF000000|(usable? INK: FADED);
		int x0 = box[0], y0 = box[1], x1 = box[0]+box[2], y1 = box[1]+box[3];
		if(hovered)
			GuiComponent.fill(pose, x0+1, y0+1, x1-1, y1-1, ink);
		GuiComponent.fill(pose, x0+1, y0, x1-1, y0+1, ink);
		GuiComponent.fill(pose, x0+1, y1-1, x1-1, y1, ink);
		GuiComponent.fill(pose, x0, y0+1, x0+1, y1-1, ink);
		GuiComponent.fill(pose, x1-1, y0+1, x1, y1-1, ink);
		Component label = softLabel(button);
		font.draw(pose, label, x0+(box[2]-font.width(label)+1)/2, y0+3, hovered? 0xA8B498: ink&0xFFFFFF);
	}

	private Component softLabel(Soft button)
	{
		return switch(button)
		{
			case BACK -> new TranslatableComponent("guide.engineered_computation.palm.back");
			case CONTENTS -> new TranslatableComponent("guide.engineered_computation.nav.contents");
			case PREVIOUS -> new TextComponent("◀");
			case NEXT -> new TextComponent("▶");
		};
	}

	private boolean softUsable(Soft button)
	{
		return switch(button)
		{
			case BACK -> canGoBack();
			case CONTENTS -> true;
			case PREVIOUS -> canTurn(-1);
			case NEXT -> canTurn(1);
		};
	}

	private int[] softBox(Soft button)
	{
		int x = left()+LCD_X;
		int y = top()+BAR_Y;
		return switch(button)
		{
			case BACK -> new int[]{x+4, y, font.width(softLabel(button))+10, BAR_HEIGHT};
			case CONTENTS -> new int[]{x+8+font.width(softLabel(Soft.BACK))+10,
					y, font.width(softLabel(button))+10, BAR_HEIGHT};
			case PREVIOUS -> new int[]{x+LCD_WIDTH-36, y, 15, BAR_HEIGHT};
			case NEXT -> new int[]{x+LCD_WIDTH-19, y, 15, BAR_HEIGHT};
		};
	}

	private static boolean isOver(int[] box, double mouseX, double mouseY)
	{
		return mouseX>=box[0]&&mouseX<box[0]+box[2]&&mouseY>=box[1]&&mouseY<box[1]+box[3];
	}

	private boolean isOverHardware(Hardware button, double mouseX, double mouseY)
	{
		double dx = mouseX-(left()+button.x), dy = mouseY-(top()+BUTTON_Y);
		return dx*dx+dy*dy<=BUTTON_RADIUS*BUTTON_RADIUS;
	}

	private int rockerAt(double mouseX, double mouseY)
	{
		double x = mouseX-left(), y = mouseY-top();
		if(x<71||x>=98||y<BUTTON_Y-11||y>=BUTTON_Y+11)
			return 0;
		return y<BUTTON_Y? -1: 1;
	}

	private boolean isOverPower(double mouseX, double mouseY)
	{
		double dx = mouseX-(left()+POWER_X), dy = mouseY-(top()+POWER_Y);
		return dx*dx+dy*dy<=25;
	}

	@Override
	protected void renderControlTooltip(PoseStack pose, int mouseX, int mouseY)
	{
		String key = null;
		for(Hardware button : Hardware.values())
			if(isOverHardware(button, mouseX, mouseY))
				key = button.key;
		if(rockerAt(mouseX, mouseY)!=0)
			key = rockerAt(mouseX, mouseY)<0? "nav.previous": "nav.next";
		if(isOverPower(mouseX, mouseY))
			key = "nav.power";
		if(key!=null)
			renderComponentTooltip(pose, List.of(new TranslatableComponent("guide.engineered_computation."+key)),
					mouseX, mouseY);
	}

	@Override
	protected boolean controlClicked(double mouseX, double mouseY)
	{
		if(isOverPower(mouseX, mouseY))
		{
			click();
			onClose();
			return true;
		}
		for(Soft button : Soft.values())
			if(softUsable(button)&&isOver(softBox(button), mouseX, mouseY))
			{
				act(button==Soft.BACK? Hardware.BACK: button==Soft.CONTENTS? Hardware.CONTENTS
						: button==Soft.PREVIOUS? Hardware.PREVIOUS: Hardware.NEXT);
				return true;
			}
		for(Hardware button : Hardware.values())
			if(isOverHardware(button, mouseX, mouseY))
			{
				act(button);
				return true;
			}
		int rocker = rockerAt(mouseX, mouseY);
		if(rocker!=0)
		{
			act(rocker<0? Hardware.PREVIOUS: Hardware.NEXT);
			return true;
		}
		return false;
	}

	private void act(Hardware button)
	{
		click();
		switch(button)
		{
			case CONTENTS -> contents();
			case BACK -> back();
			case PREVIOUS -> turn(-1);
			case NEXT -> turn(1);
		}
	}

	private void click()
	{
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
	}
}
