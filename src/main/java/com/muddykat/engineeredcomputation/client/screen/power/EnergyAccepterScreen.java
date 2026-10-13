package com.muddykat.engineeredcomputation.client.screen.power;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.StatusCode;
import com.muddykat.engineeredcomputation.power.EnergyAccepterMenu;

public class EnergyAccepterScreen extends AbstractContainerScreen<EnergyAccepterMenu>
{
	private static final String KEY = "gui.engineered_computation.power.";
	private static final int STATUS_Y = 18;
	private static final int STATUS_HEIGHT = 24;
	private static final int GAUGE_Y = 48;
	private static final int TEXT_Y = 60;
	private static final int LINE = 11;
	private static final int DISABLED = 0x9AA8A0;
	private static final int ENERGY = 0xFFE0B040;

	public EnergyAccepterScreen(EnergyAccepterMenu menu, Inventory inventory, Component title)
	{
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = TEXT_Y+3*LINE+4;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(graphics, x, y, imageWidth, imageHeight);
		int width = imageWidth-16;
		ECGui.sunken(graphics, x+7, y+STATUS_Y-1, width+2, STATUS_HEIGHT+2);
		graphics.fill(x+8, y+STATUS_Y, x+8+width, y+STATUS_Y+STATUS_HEIGHT, 0xFF101814);
		Component title, detail;
		int colour;
		if(!menu.isEnabled())
		{
			title = Component.translatable(KEY+"disabled");
			detail = Component.translatable(KEY+"disabled.detail");
			colour = DISABLED;
		}
		else
		{
			StatusCode code = menu.isPowered()? StatusCode.OK: StatusCode.PAYMENT_REQUIRED;
			title = code.title();
			detail = Component.translatable(KEY+(menu.isPowered()? "ok": "payment_required")+".detail");
			colour = code.colour();
		}
		graphics.drawString(font, ECGui.fit(font, title, width-8), x+12, y+STATUS_Y+3, colour, false);
		graphics.drawString(font, ECGui.fit(font, detail, width-8), x+12, y+STATUS_Y+13, 0xA0B8A8, false);
		ECGui.gauge(graphics, x+8, y+GAUGE_Y, width, 8,
				menu.getCapacity()==0? 0: menu.getStored()/(double)menu.getCapacity(), ENERGY);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
	{
		graphics.drawString(font, title, titleLabelX, titleLabelY, ECGui.LABEL, false);
		int width = imageWidth-16;
		graphics.drawString(font, ECGui.fit(font, Component.translatable(KEY+"stored", number(menu.getStored()),
				number(menu.getCapacity())), width), 8, TEXT_Y, ECGui.LABEL, false);
		graphics.drawString(font, ECGui.fit(font, Component.translatable(KEY+"network", number(menu.getCost())), width),
				8, TEXT_Y+LINE, ECGui.LABEL, false);
		graphics.drawString(font, ECGui.fit(font, Component.translatable(KEY+"network_stored",
				number(menu.getNetworkStored()), number(menu.getNetworkCapacity())), width), 8, TEXT_Y+2*LINE,
				ECGui.LABEL, false);
	}

	static String number(long value)
	{
		return String.format(Locale.ROOT, "%,d", value);
	}
}
