package org.cyclops.integratedscripting.client.gui.component.input;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.integratedscripting.Reference;

/**
 * Shows a confirm/cancel dialog
 * @author rubensworks
 */
public class WidgetDialog extends AbstractWidget implements GuiEventListener {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/dialog.png");
    public static final int WIDTH = 216;
    private static final int HEIGHT = 71;

    private final Font font;
    private final Screen parent;
    private final Component title;
    private final Component message;
    private final ButtonText buttonConfirm;
    private final ButtonText buttonCancel;

    public WidgetDialog(Font font, int x, int y, Screen parent,
                        Component title, Component message, Component confirm, Component cancel,
                        Button.OnPress confirmCallback, Button.OnPress cancelCallback) {
        super(x, y, WIDTH, HEIGHT, message);
        this.font = font;
        this.parent = parent;
        this.title = title;
        this.message = message;

        this.buttonConfirm = new ButtonText(x + 50, y + HEIGHT - 15 - 5, 50, 15, confirm, confirm, confirmCallback, true);
        this.buttonCancel = new ButtonText(x + WIDTH - 50 - 50, y + HEIGHT - 15 - 5, 50, 15, cancel, cancel, cancelCallback, true);
    }

    protected void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Gray-out background
        RenderSystem.setShaderColor(0F, 0F, 0F, 0.95F);
        guiGraphics.fill(0, 0, parent.width, parent.height, Helpers.RGBAToInt(50, 50, 50, 100));
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // Draw dialog texture
        guiGraphics.blit(TEXTURE, getX(), getY(), 0, 0, WIDTH, HEIGHT);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        drawBackground(guiGraphics, mouseX, mouseY, partialTicks);

        this.buttonConfirm.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.buttonCancel.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(font, title, this.getX() + this.width / 2, this.getY() + 4, 16777215);
        FormattedCharSequence formattedcharsequence = message.getVisualOrderText();
        guiGraphics.drawString(font, message, (this.getX() + this.width / 2 - font.width(formattedcharsequence) / 2), (this.getY() + 25), 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return this.buttonConfirm.mouseClicked(mouseX, mouseY, mouseButton)
                || this.buttonCancel.mouseClicked(mouseX, mouseY, mouseButton)
                || super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, message);
    }
}
