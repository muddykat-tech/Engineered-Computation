package com.muddykat.engineeredcomputation.power;

import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;

import com.muddykat.engineeredcomputation.client.gui.ECGui;
import com.muddykat.engineeredcomputation.network.StatusCode;

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
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick)
	{
		renderBackground(pose);
		super.render(pose, mouseX, mouseY, partialTick);
		renderTooltip(pose, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY)
	{
		int x = leftPos, y = topPos;
		ECGui.panel(pose, x, y, imageWidth, imageHeight);
		int width = imageWidth-16;
		ECGui.sunken(pose, x+7, y+STATUS_Y-1, width+2, STATUS_HEIGHT+2);
		GuiComponent.fill(pose, x+8, y+STATUS_Y, x+8+width, y+STATUS_Y+STATUS_HEIGHT, 0xFF101814);
		Component title, detail;
		int colour;
		if(!menu.isEnabled())
		{
			title = new TranslatableComponent(KEY+"disabled");
			detail = new TranslatableComponent(KEY+"disabled.detail");
			colour = DISABLED;
		}
		else
		{
			StatusCode code = menu.isPowered()? StatusCode.OK: StatusCode.PAYMENT_REQUIRED;
			title = code.title();
			detail = new TranslatableComponent(KEY+(menu.isPowered()? "ok": "payment_required")+".detail");
			colour = code.colour();
		}
		font.draw(pose, ECGui.fit(font, title, width-8), x+12, y+STATUS_Y+3, colour);
		font.draw(pose, ECGui.fit(font, detail, width-8), x+12, y+STATUS_Y+13, 0xA0B8A8);
		ECGui.gauge(pose, x+8, y+GAUGE_Y, width, 8,
				menu.getCapacity()==0? 0: menu.getStored()/(double)menu.getCapacity(), ENERGY);
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY)
	{
		font.draw(pose, title, titleLabelX, titleLabelY, ECGui.LABEL);
		int width = imageWidth-16;
		font.draw(pose, ECGui.fit(font, new TranslatableComponent(KEY+"stored", number(menu.getStored()),
				number(menu.getCapacity())), width), 8, TEXT_Y, ECGui.LABEL);
		font.draw(pose, ECGui.fit(font, new TranslatableComponent(KEY+"network", number(menu.getCost())), width),
				8, TEXT_Y+LINE, ECGui.LABEL);
		font.draw(pose, ECGui.fit(font, new TranslatableComponent(KEY+"network_stored",
				number(menu.getNetworkStored()), number(menu.getNetworkCapacity())), width), 8, TEXT_Y+2*LINE,
				ECGui.LABEL);
	}

	static String number(long value)
	{
		return String.format(Locale.ROOT, "%,d", value);
	}
}
