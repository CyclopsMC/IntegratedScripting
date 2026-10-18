package org.cyclops.integratedscripting.client.gui.component.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonExtended;
import org.cyclops.cyclopscore.client.gui.image.IImage;

/**
 * A button with a centered icon, which is dimmed when the button is inactive.
 * @author rubensworks
 */
public class ButtonIcon extends ButtonExtended {

    private final IImage image;

    public ButtonIcon(int x, int y, int width, int height, Component narrationMessage, OnPress onPress, IImage image) {
        super(x, y, width, height, narrationMessage, onPress, true);
        this.image = image;
    }

    @Override
    protected void drawButtonInner(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float color = this.active ? 1F : 0.4F;
        this.image.drawWithColor(guiGraphics, this.getX() + (this.width - this.image.getWidth()) / 2,
                this.getY() + (this.height - this.image.getHeight()) / 2, color, color, color, 1F);
    }
}
