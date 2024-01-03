package org.cyclops.integratedscripting.client.gui.component.input;

import net.minecraft.client.gui.font.TextFieldHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author rubensworks
 */
public class TextFieldHelperExtended extends TextFieldHelper {

    private final Supplier<String> getMessageFn;
    private final Consumer<String> setSelectionFn;
    private final Consumer<Integer> setCursorFn;

    public TextFieldHelperExtended(Supplier<String> getMessageFn, Consumer<String> setMessageFn,
                                   Supplier<String> getClipboardFn, Consumer<String> setClipboardFn,
                                   Predicate<String> stringValidator, Consumer<String> setSelectionFn,
                                   Consumer<Integer> setCursorFn) {
        super(getMessageFn, setMessageFn, getClipboardFn, setClipboardFn, stringValidator);
        this.getMessageFn = getMessageFn;
        this.setSelectionFn = setSelectionFn;
        this.setCursorFn = setCursorFn;
    }

    public String getSelected() {
        String value = this.getMessageFn.get();
        int i = Math.min(this.getCursorPos(), this.getSelectionPos());
        int j = Math.max(this.getCursorPos(), this.getSelectionPos());
        return value.substring(i, j);
    }

    @Override
    public boolean charTyped(char p_95144_) {
        String selectedBefore = getSelected();
        boolean ret = super.charTyped(p_95144_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
        return ret;
    }

    @Override
    public boolean keyPressed(int p_95146_) {
        String selectedBefore = getSelected();
        boolean ret = super.keyPressed(p_95146_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
        return ret;
    }

    @Override
    public void selectAll() {
        String selectedBefore = getSelected();
        super.selectAll();
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
    }

    @Override
    public void setSelectionPos(int p_169101_) {
        String selectedBefore = getSelected();
        super.setSelectionPos(p_169101_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
    }

    @Override
    public void setSelectionRange(int p_95148_, int p_95149_) {
        String selectedBefore = getSelected();
        super.setSelectionRange(p_95148_, p_95149_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
    }

    @Override
    public void moveBy(int p_232576_, boolean p_232577_, CursorStep p_232578_) {
        String selectedBefore = getSelected();
        super.moveBy(p_232576_, p_232577_, p_232578_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
    }

    @Override
    public void removeFromCursor(int p_232573_, CursorStep p_232574_) {
        String selectedBefore = getSelected();
        super.removeFromCursor(p_232573_, p_232574_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
    }

    @Override
    public void setCursorPos(int p_95180_, boolean p_95181_) {
        String selectedBefore = getSelected();
        super.setCursorPos(p_95180_, p_95181_);
        String selectedAfter = getSelected();
        if (!selectedBefore.equals(selectedAfter)) {
            this.setSelectionFn.accept(selectedAfter);
        }
        this.setCursorFn.accept(p_95180_);
    }
}
