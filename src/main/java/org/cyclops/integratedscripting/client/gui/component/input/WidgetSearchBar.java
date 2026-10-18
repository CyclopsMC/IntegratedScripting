package org.cyclops.integratedscripting.client.gui.component.input;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.helper.Helpers;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A bar below a text area to find text or go to a line.
 * @author rubensworks
 */
public class WidgetSearchBar extends AbstractWidget {

    public static final int HEIGHT = 12;
    private static final int BUTTON_WIDTH = 10;
    private static final int COLOR_TEXT = Helpers.RGBToInt(224, 224, 224);
    private static final int COLOR_ERROR = Helpers.RGBToInt(255, 85, 85);

    private final Font font;
    private final boolean lineMode;
    private final Component label;
    private final EditBox input;
    private final ButtonText buttonPrevious;
    private final ButtonText buttonNext;
    private final ButtonText buttonClose;
    private final IntConsumer navigateCallback;
    private final Runnable closeCallback;
    private String status = "";
    private boolean error;

    /**
     * @param lineMode If a line number is entered instead of a text to find.
     * @param changeCallback Called when the input changes.
     * @param navigateCallback Called with 1 to go to the next match (or the line), and -1 to go to the previous match.
     * @param closeCallback Called when the bar should be closed.
     */
    public WidgetSearchBar(Font font, int x, int y, int width, boolean lineMode, String initialValue,
                           Consumer<String> changeCallback, IntConsumer navigateCallback, Runnable closeCallback) {
        super(x, y, width, HEIGHT, Component.translatable(lineMode ? "gui.integratedscripting.goto_line" : "gui.integratedscripting.find"));
        this.font = font;
        this.lineMode = lineMode;
        this.label = Component.translatable(lineMode ? "gui.integratedscripting.goto_line.label" : "gui.integratedscripting.find.label");
        this.navigateCallback = navigateCallback;
        this.closeCallback = closeCallback;

        // Layout from right to left: close, next, previous, status
        int right = x + width - 1;
        this.buttonClose = createButton(right - BUTTON_WIDTH, "x", "gui.integratedscripting.find.close", closeCallback::run);
        right -= BUTTON_WIDTH + 1;
        this.buttonNext = createButton(right - BUTTON_WIDTH, ">", "gui.integratedscripting.find.next", () -> navigateCallback.accept(1));
        right -= BUTTON_WIDTH + 1;
        this.buttonPrevious = createButton(right - BUTTON_WIDTH, "<", "gui.integratedscripting.find.previous", () -> navigateCallback.accept(-1));
        if (!lineMode) {
            right -= BUTTON_WIDTH + 1 + font.width("000/000") + 2;
        } else {
            right = x + width - 1 - BUTTON_WIDTH - 2;
        }

        int inputX = x + 3 + font.width(this.label) + 3;
        this.input = new EditBox(font, inputX, y + 1, Math.max(10, right - inputX), HEIGHT - 2, this.getMessage());
        this.input.setMaxLength(256);
        this.input.setValue(initialValue);
        this.input.setResponder(changeCallback);
        this.input.setFocused(true);
    }

    private ButtonText createButton(int x, String text, String tooltip, Runnable callback) {
        ButtonText button = new ButtonText(x, this.getY() + 1, BUTTON_WIDTH, HEIGHT - 2,
                Component.translatable(tooltip), Component.literal(text), (b) -> callback.run(), true);
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        return button;
    }

    public boolean isLineMode() {
        return lineMode;
    }

    public String getValue() {
        return this.input.getValue();
    }

    public void focusAndSelect(String value) {
        if (!value.isEmpty()) {
            this.input.setValue(value);
        }
        this.input.moveCursorToEnd(false);
        this.input.setHighlightPos(0);
        this.setFocused(true);
    }

    /**
     * @param status A short status text, such as the current match and number of matches.
     * @param error If the input is shown as erroneous, for example when nothing is found.
     */
    public void setStatus(String status, boolean error) {
        this.status = status;
        this.error = error;
        this.input.setTextColor(error ? COLOR_ERROR : COLOR_TEXT);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.input.setFocused(focused);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, Helpers.RGBAToInt(60, 60, 60, 255));
        guiGraphics.drawString(this.font, this.label, this.getX() + 3, this.getY() + 2, COLOR_TEXT, false);
        this.input.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (!this.lineMode) {
            int statusRight = this.buttonPrevious.getX() - 2;
            guiGraphics.drawString(this.font, this.status, statusRight - this.font.width(this.status), this.getY() + 2,
                    this.error ? COLOR_ERROR : COLOR_TEXT, false);
            this.buttonPrevious.render(guiGraphics, mouseX, mouseY, partialTicks);
            this.buttonNext.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        this.buttonClose.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (this.buttonClose.mouseClicked(mouseX, mouseY, mouseButton)
                || (!this.lineMode && (this.buttonPrevious.mouseClicked(mouseX, mouseY, mouseButton)
                || this.buttonNext.mouseClicked(mouseX, mouseY, mouseButton)))) {
            return true;
        }
        this.input.mouseClicked(mouseX, mouseY, mouseButton);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.closeCallback.run();
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.navigateCallback.accept((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1);
        } else {
            this.input.keyPressed(key, scanCode, modifiers);
        }
        // Don't let typing trigger screen actions, such as closing the screen
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return this.input.charTyped(c, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
    }
}
