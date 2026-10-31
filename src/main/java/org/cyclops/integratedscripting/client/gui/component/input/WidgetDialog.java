package org.cyclops.integratedscripting.client.gui.component.input;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.integratedscripting.Reference;

import javax.annotation.Nullable;

/**
 * Shows a confirm/cancel dialog
 * @author rubensworks
 */
public class WidgetDialog extends AbstractWidget implements GuiEventListener {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/dialog.png");
    public static final int WIDTH = 216;
    private static final int HEIGHT = 71;

    private final Font font;
    private final Screen parent;
    private final Component title;
    private final Component message;
    private final ButtonText buttonConfirm;
    private final ButtonText buttonCancel;
    @Nullable
    private final EditBox input;
    @Nullable
    private Component warning;

    public WidgetDialog(Font font, int x, int y, Screen parent,
                        Component title, Component message, Component confirm, Component cancel,
                        Button.OnPress confirmCallback, Button.OnPress cancelCallback) {
        this(font, x, y, parent, title, message, confirm, cancel, confirmCallback, cancelCallback, null);
    }

    /**
     * @param inputValue If not null, a text input is shown with this initial value.
     */
    public WidgetDialog(Font font, int x, int y, Screen parent,
                        Component title, Component message, Component confirm, Component cancel,
                        Button.OnPress confirmCallback, Button.OnPress cancelCallback, @Nullable String inputValue) {
        super(x, y, WIDTH, HEIGHT, message);
        this.font = font;
        this.parent = parent;
        this.title = title;
        this.message = message;

        this.buttonConfirm = new ButtonText(x + 50, y + HEIGHT - 15 - 5, 50, 15, confirm, confirm, confirmCallback, true);
        this.buttonCancel = new ButtonText(x + WIDTH - 50 - 50, y + HEIGHT - 15 - 5, 50, 15, cancel, cancel, cancelCallback, true);
        if (inputValue != null) {
            this.input = new EditBox(font, x + 10, y + 31, WIDTH - 20, 12, message);
            this.input.setMaxLength(256);
            this.input.setValue(inputValue);
            this.input.setFocused(true);
        } else {
            this.input = null;
        }
    }

    /**
     * @param warning A short warning to show below the input, or null.
     */
    public void setWarning(@Nullable Component warning) {
        this.warning = warning;
    }

    protected void drawBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Gray-out background
        guiGraphics.fill(0, 0, parent.width, parent.height, IModHelpers.get().getBaseHelpers().RGBAToInt(50, 50, 50, 100));

        // Draw dialog texture
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 0, 0, WIDTH, HEIGHT, 256, 256);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        drawBackground(guiGraphics, mouseX, mouseY, partialTicks);

        this.buttonConfirm.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        this.buttonCancel.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.centeredText(font, title, this.getX() + this.width / 2, this.getY() + 4, ARGB.opaque(16777215));
        FormattedCharSequence formattedcharsequence = message.getVisualOrderText();
        guiGraphics.text(font, message, (this.getX() + this.width / 2 - font.width(formattedcharsequence) / 2), (this.getY() + (this.input != null ? 19 : 25)), ARGB.opaque(4210752), false);
        if (this.input != null) {
            this.input.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (this.warning != null) {
            // Scaled down to fit between the input and the buttons, and further for long translations
            String warning = this.warning.getString();
            float scale = Math.min(0.5F, (float) (WIDTH - 20) / font.width(warning));
            IModHelpers.get().getRenderHelpers().drawScaledString(guiGraphics, font, warning, this.getX() + this.width / 2 - (int) (font.width(warning) * scale / 2),
                    this.getY() + 45, scale, IModHelpers.get().getBaseHelpers().RGBAToInt(140, 30, 30, 255), false, Font.DisplayMode.NORMAL);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouse, boolean isDoubleClick) {
        return this.buttonConfirm.mouseClicked(mouse, isDoubleClick)
                || this.buttonCancel.mouseClicked(mouse, isDoubleClick)
                || (this.input != null && this.input.mouseClicked(mouse, isDoubleClick))
                || super.mouseClicked(mouse, isDoubleClick);
    }

    @Nullable
    public EditBox getInput() {
        return input;
    }

    public void confirm(InputWithModifiers input) {
        this.buttonConfirm.onPress(input);
    }

    public void cancel(InputWithModifiers input) {
        this.buttonCancel.onPress(input);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, message);
    }
}
