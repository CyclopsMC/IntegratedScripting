package org.cyclops.integratedscripting.client.gui.component.input;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.ARGB;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2i;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.component.input.IInputListener;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.integratedscripting.api.language.ScriptCompletions;
import org.cyclops.integratedscripting.api.language.ScriptSyntaxError;
import org.cyclops.integratedscripting.client.key.Keys;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A widget to edit multi-line text.
 * Inspired by {@link MultilineTextField}.
 *
 * The using screen must add this as a child and call the following method from its respective method:
 * * {@link #tick()}
 * * {@link #mouseClicked(MouseButtonEvent, boolean)}
 * * {@link #mouseDragged(MouseButtonEvent, double, double)}
 * * {@link #mouseScrolled(double, double, double, double)}
 * * {@link #keyPressed(KeyEvent)}
 * * {@link #charTyped(CharacterEvent)}
 *
 * @author rubensworks
 */
public class WidgetTextArea extends EditBox implements GuiEventListener {

    public static final int ROW_HEIGHT = 9;
    private static final int COMPLETION_ROWS = 6;
    // Colors of the completion and signature popups
    private static final int POPUP_BACKGROUND = 0xFF202020;
    private static final int POPUP_BORDER = 0xFFA0A0A0;
    // Neutral, so the colors of value types stay readable
    private static final int POPUP_SELECTED = 0xFF505050;
    private static final int POPUP_TEXT = 0xFFE0E0E0;
    private static final int POPUP_DETAIL = 0xFF909090;
    public static final int SCROLLBAR_WIDTH = 7;
    // Same height as the vanilla scroller, which the scrollbar's drag handling assumes
    private static final int SCROLLBAR_HANDLE_HEIGHT = 15;
    // Height of the arrow between a syntax error tooltip and its error
    private static final int TOOLTIP_ARROW = 4;
    // How far vanilla tooltip backgrounds extend beyond their contents
    private static final int TOOLTIP_BORDER = 4;
    // Colors of vanilla tooltips
    private static final int TOOLTIP_BACKGROUND = 0xF0100010;
    private static final int TOOLTIP_BORDER_COLOR = 0x505000FF;

    private final TextFieldHelperExtended textFieldHelper;
    private final Font font;
    private final boolean showLineNumbers;

    private int frameTick;
    private String value = "";
    private String selected = "";
    @Nullable
    private IInputListener listener;
    @Nullable
    private IInputListener listenerSelection;
    @Nullable
    private IInputListener listenerCursor;
    @Nullable
    private IMarkupProvider markupProvider;
    @Nullable
    private BiFunction<String, Integer, ScriptCompletions> completionProvider;
    @Nullable
    private BiFunction<String, Integer, Component> signatureProvider;
    @Nullable
    private ScriptCompletions completions;
    private int completionIndex;
    // Where the completions were last drawn, to handle clicks and scrolling on them
    private int completionsX;
    private int completionsY;
    private int completionsWidth;
    private int completionsFirstItem;
    private int completionsRows;
    private WidgetTextArea.DisplayCache emptyDisplayCache = createEmptyDisplayCache();
    @Nullable
    private WidgetTextArea.DisplayCache displayCache = emptyDisplayCache;
    private long lastClickTime;
    private int lastIndex = -1;
    @Nullable
    private WidgetScrollBar scrollBar;
    private int firstRow;
    @Nullable
    private TextLayout layout;
    private TextHistory history = new TextHistory();
    private boolean draggingSelection;
    private double dragMouseX;
    private double dragMouseY;
    private float dragScrollProgress;
    @Nullable
    private String searchQuery;
    @Nullable
    private String searchMatchesValue;
    @Nullable
    private String searchMatchesQuery;
    private int[] searchMatches = new int[0];
    private String indent = "  ";
    @Nullable
    private ShortcutListener shortcutListener;
    @Nullable
    private ScriptSyntaxError syntaxError;

    public WidgetTextArea(Font font, int x, int y, int width, int height, Component narrationMessage, boolean scrollBar, boolean showLineNumbers) {
        super(font, x, y, width, height, narrationMessage);
        this.font = font;
        this.showLineNumbers = showLineNumbers;
        this.textFieldHelper = new TextFieldHelperExtended(this::getValue, this::setValuePassive, this::getClipboard, this::setClipboard, s -> true, this::setSelected, this::onCursorPosChanged);
        if (scrollBar) {
            this.scrollBar = this.createScrollBar();
        }
    }

    private WidgetScrollBar createScrollBar() {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        return new WidgetScrollBar(x + width - SCROLLBAR_WIDTH, y, height,
                Component.translatable("gui.cyclopscore.scrollbar"), firstRow -> setFirstRow(firstRow, false), height / ROW_HEIGHT) {
            @Override
            public int getTotalRows() {
                return getDisplayCache().linesTotal;
            }

            @Override
            public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
                // Only show scrollbar if needed, and draw a narrower handle than the vanilla scroller to leave more room for text
                if (this.needsScrollBars()) {
                    int x = this.getX();
                    int y = this.getY() + (int) ((this.getHeight() - SCROLLBAR_HANDLE_HEIGHT - 2) * this.getCurrentScroll());
                    int x2 = x + SCROLLBAR_WIDTH - 1;
                    int y2 = y + SCROLLBAR_HANDLE_HEIGHT;
                    guiGraphics.fill(x, y, x2, y2, IModHelpers.get().getBaseHelpers().RGBAToInt(85, 85, 85, 255));
                    guiGraphics.fill(x, y, x2 - 1, y2 - 1, IModHelpers.get().getBaseHelpers().RGBAToInt(255, 255, 255, 255));
                    guiGraphics.fill(x + 1, y + 1, x2 - 1, y2 - 1, IModHelpers.get().getBaseHelpers().RGBAToInt(198, 198, 198, 255));
                }
            }
        };
    }

    /**
     * Change the height, for example to make room for other widgets, while keeping the scroll and cursor position.
     */
    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        if (this.scrollBar != null) {
            this.scrollBar = this.createScrollBar();
            this.setFirstRow(this.firstRow, true);
        }
        this.clearDisplayCache();
        this.ensureCursorVisible();
    }

    private DisplayCache createEmptyDisplayCache() {
        Pos2i pos = convertLocalToScreen(new Pos2i(0, 0));
        TextLayout layout = new TextLayout("", new int[]{0}, new String[]{""}, new int[]{0}, new Markup(Collections.emptyList()));
        return new DisplayCache("", pos, true, new int[]{0}, new LineInfo[]{new LineInfo(layout, 0, pos.x, pos.y, false)}, new LineInfo[]{new LineInfo(layout, 0, pos.x, pos.y, false)}, new Rect2i[0], 0, 0);
    }

    public void setFirstRow(int firstRow, boolean propagateToScrollbar) {
        this.firstRow = firstRow;

        if (this.scrollBar != null) {
            int linesTotal = getDisplayCache().linesTotal;
            if (this.firstRow > linesTotal - this.scrollBar.getVisibleRows()) {
                this.firstRow = linesTotal - this.scrollBar.getVisibleRows();
            }
            if (this.firstRow < 0) {
                this.firstRow = 0;
            }
            if (propagateToScrollbar) {
                this.scrollBar.setFirstRow(this.firstRow, false);
            }
        }

        this.clearDisplayCache();
    }

    public void setListener(@Nullable IInputListener listener) {
        this.listener = listener;
    }

    public void setListenerSelection(@Nullable IInputListener listenerSelection) {
        this.listenerSelection = listenerSelection;
    }

    public void setListenerCursor(@Nullable IInputListener listenerCursor) {
        this.listenerCursor = listenerCursor;
    }

    /**
     * @param shortcutListener Called with each key press before the editor handles it, returns true if it handled the key.
     */
    public void setShortcutListener(@Nullable ShortcutListener shortcutListener) {
        this.shortcutListener = shortcutListener;
    }

    /**
     * Select a range and scroll to it.
     * @param start The start position, where the selection is anchored.
     * @param end The end position, where the cursor is placed.
     */
    public void selectRange(int start, int end) {
        this.textFieldHelper.setSelectionRange(end, start);
        this.clearDisplayCache();
        this.ensureCursorVisible();
    }

    public void setSyntaxError(@Nullable ScriptSyntaxError syntaxError) {
        this.syntaxError = syntaxError;
    }

    /**
     * @param indent The string that is inserted for one indentation level.
     */
    public void setIndent(String indent) {
        this.indent = indent;
    }

    /**
     * @param searchQuery A query of which all occurrences are highlighted, or null.
     */
    public void setSearchQuery(@Nullable String searchQuery) {
        this.searchQuery = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery;
    }

    /**
     * @return The start positions of all occurrences of the search query.
     */
    public int[] getSearchMatches() {
        if (this.searchQuery == null) {
            return new int[0];
        }
        String value = this.getValue();
        if (!value.equals(this.searchMatchesValue) || !this.searchQuery.equals(this.searchMatchesQuery)) {
            this.searchMatches = TextEditing.findAll(value, this.searchQuery);
            this.searchMatchesValue = value;
            this.searchMatchesQuery = this.searchQuery;
        }
        return this.searchMatches;
    }

    public void setHistory(TextHistory history) {
        this.history = history;
    }

    public void setMarkupProvider(@Nullable IMarkupProvider markupProvider) {
        this.markupProvider = markupProvider;
        this.layout = null;
    }

    public void setCompletionProvider(@Nullable BiFunction<String, Integer, ScriptCompletions> completionProvider) {
        this.completionProvider = completionProvider;
    }

    public void setSignatureProvider(@Nullable BiFunction<String, Integer, Component> signatureProvider) {
        this.signatureProvider = signatureProvider;
    }

    /**
     * Only show completions while typing, not when moving the cursor.
     */
    private void updateCompletions(boolean textChanged) {
        if (textChanged && this.completionProvider != null) {
            ScriptCompletions completionsNew = this.completionProvider.apply(this.getValue(), this.getCursorPos());
            if (completionsNew == null || this.completions == null || !completionsNew.items().equals(this.completions.items())) {
                this.completionIndex = 0;
            }
            this.completions = completionsNew;
        } else {
            this.completions = null;
        }
    }

    /**
     * @return If completions were shown.
     */
    public boolean dismissCompletions() {
        boolean shown = this.completions != null;
        this.completions = null;
        return shown;
    }

    private boolean completionKeyPressed(int key) {
        int count = this.completions.items().size();
        switch (key) {
            case GLFW.GLFW_KEY_DOWN:
                this.completionIndex = (this.completionIndex + 1) % count;
                return true;
            case GLFW.GLFW_KEY_UP:
                this.completionIndex = (this.completionIndex + count - 1) % count;
                return true;
            case GLFW.GLFW_KEY_TAB:
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                this.acceptCompletion();
                return true;
            case GLFW.GLFW_KEY_ESCAPE:
                this.completions = null;
                return true;
            default:
                return false;
        }
    }

    /**
     * @return The row of the shown completions at the given position, or -1 if there is none.
     */
    private int getCompletionRowAt(double mouseX, double mouseY) {
        if (this.completions == null || this.completionsRows == 0 || mouseX < this.completionsX || mouseX >= this.completionsX + this.completionsWidth
                || mouseY < this.completionsY + 1 || mouseY >= this.completionsY + 1 + this.completionsRows * ROW_HEIGHT) {
            return -1;
        }
        return Math.min((int) (mouseY - this.completionsY - 1) / ROW_HEIGHT, this.completionsRows - 1);
    }

    private void acceptCompletion() {
        TextHistory.State before = this.getState();
        // Replace the text directly instead of selecting it, as a selection would change the bound member
        String insert = this.completions.items().get(this.completionIndex).name() + "(";
        String value = this.getValue();
        int cursorPos = this.getCursorPos();
        int start = this.completions.start();
        int newCursorPos = start + insert.length();
        this.completions = null;
        this.setState(new TextHistory.State(value.substring(0, start) + insert + value.substring(cursorPos), newCursorPos, newCursorPos));
        this.recordHistory(before, false);
        this.clearDisplayCache();
        this.ensureCursorVisible();
    }

    public void setValue(String value) {
        this.firstRow = 0;
        // Another script may have the same contents but another language
        this.layout = null;
        this.setValuePassive(value);

        textFieldHelper.setCursorToStart();
        textFieldHelper.setSelectionPos(textFieldHelper.getCursorPos());
        if (this.scrollBar != null) {
            scrollBar.scrollTo(0);
        }
    }

    public void setValuePassive(String value) {
        this.value = value;
        this.clearDisplayCache();

        if (listener != null) {
            listener.onChanged();
        }
    }

    public String getValue() {
        return this.value;
    }

    public void setSelected(String value) {
        this.selected = value;

        if (listenerSelection != null) {
            listenerSelection.onChanged();
        }
    }

    public String getSelected() {
        return selected;
    }

    public int getCursorPos() {
        return this.textFieldHelper.getCursorPos();
    }

    public void setCursorPos(int pos) {
        this.textFieldHelper.setCursorPos(pos, true);
    }

    private void onCursorPosChanged(int cursorPos) {
        if (listenerCursor != null) {
            listenerCursor.onChanged();
        }
    }

    public int getSelectionPos() {
        return this.textFieldHelper.getSelectionPos();
    }

    public void setSelectionPos(int pos) {
        this.textFieldHelper.setSelectionPos(pos);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", this.getValue()));
    }

    private void setClipboard(String value) {
        TextFieldHelper.setClipboardContents(Minecraft.getInstance(), value);

    }

    private String getClipboard() {
        return TextFieldHelper.getClipboardContents(Minecraft.getInstance());
    }

    public void tick() {
        ++this.frameTick;

        // Keep scrolling while a selection is dragged above or below the editor, faster when further away
        if (this.draggingSelection) {
            double distance = this.dragMouseY < this.getY() ? this.getY() - this.dragMouseY
                    : this.dragMouseY - this.clampToVisibleRows(this.dragMouseY);
            if (distance > 0) {
                this.dragScrollProgress += (float) Math.min(2.5, 0.15 + distance / 24);
                int rows = (int) this.dragScrollProgress;
                this.dragScrollProgress -= rows;
                if (rows > 0) {
                    this.setFirstRow(this.firstRow + (this.dragMouseY < this.getY() ? -rows : rows), true);
                    this.updateDragSelection();
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouse, boolean isDoubleClick) {
        double mouseX = mouse.x();
        double mouseY = mouse.y();
        int mouseButton = mouse.button();
        // Insert a clicked completion
        int completionRow = this.getCompletionRowAt(mouseX, mouseY);
        if (completionRow >= 0 && mouseButton == 0) {
            this.completionIndex = this.completionsFirstItem + completionRow;
            this.acceptCompletion();
            return true;
        }
        this.completions = null;
        if (mouseButton == 1 && mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            // Don't select everything on right-click, as typing would then replace the whole script
            this.setFocused(true);
            return true;
        } else {
            this.draggingSelection = false;
            if (mouseButton == 0 && mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                this.draggingSelection = this.scrollBar == null || mouseX < this.getX() + this.width - SCROLLBAR_WIDTH;
                this.dragMouseX = mouseX;
                this.dragMouseY = mouseY;
                this.dragScrollProgress = 0;
                this.setFocused(true);
                long i = Util.getMillis();
                DisplayCache displayCache = this.getDisplayCache();
                int j = displayCache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i((int)mouseX, this.clampToVisibleRows(mouseY))));
                if (j >= 0) {
                    if (j == this.lastIndex && i - this.lastClickTime < 250L) {
                        if (!this.textFieldHelper.isSelecting()) {
                            this.selectWord(j);
                        } else {
                            this.textFieldHelper.selectAll();
                        }
                    } else {
                        this.textFieldHelper.setCursorPos(j, mouse.hasShiftDown());
                    }

                    this.clearDisplayCache();
                }

                this.lastIndex = j;
                this.lastClickTime = i;
                this.setFocused(true);
            } else {
                this.setFocused(false);
            }

            return this.isFocused();
        }
    }

    private void selectWord(int p_98142_) {
        String value = this.getValue();
        this.textFieldHelper.setSelectionRange(
                StringSplitter.getWordPosition(value, -1, p_98142_, false),
                StringSplitter.getWordPosition(value, 1, p_98142_, false)
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseZ, double scroll) {
        // Move through the completions when scrolling on them
        if (scroll != 0 && this.getCompletionRowAt(mouseX, mouseY) >= 0) {
            this.completionIndex = Mth.clamp(this.completionIndex - (int) Math.signum(scroll), 0, this.completions.items().size() - 1);
            return true;
        }
        if (this.scrollBar != null && mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height
                && this.scrollBar.mouseScrolled(mouseX, mouseY, mouseZ, scroll)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouse, double offsetX, double offsetY) {
        double mouseX = mouse.x();
        double mouseY = mouse.y();
        int mouseButton = mouse.button();
        if (!this.draggingSelection && this.scrollBar != null && mouseX >= this.getX() + this.width - 12 && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            return this.scrollBar.mouseDragged(mouse, offsetX, offsetY);
        }

        if (mouseButton == 0 && this.draggingSelection) {
            // Scrolling beyond the top or bottom happens in tick, so it continues while the mouse is held still
            this.dragMouseX = mouseX;
            this.dragMouseY = mouseY;
            this.updateDragSelection();
            return true;
        }
        // Let other drags through, such as spreading items over inventory slots
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouse) {
        if (mouse.button() == 0) {
            this.draggingSelection = false;
            this.dragScrollProgress = 0;
        }
        return super.mouseReleased(mouse);
    }

    private void updateDragSelection() {
        int x = (int) Math.max(this.getX(), Math.min(this.getX() + this.width - 1, this.dragMouseX));
        int y = this.clampToVisibleRows(Math.max(this.getY(), this.dragMouseY));
        DisplayCache bookeditscreen$displaycache = this.getDisplayCache();
        int i = bookeditscreen$displaycache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i(x, y)));
        this.textFieldHelper.setCursorPos(i, true);
        this.clearDisplayCache();
        this.setFocused(true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int typedChar = event.key();
        int keyCode = event.scancode();
        int modifiers = event.modifiers();
        if (this.completions != null && this.completionKeyPressed(typedChar)) {
            return true;
        }
        if (this.shortcutListener != null && this.shortcutListener.keyPressed(typedChar, keyCode, modifiers)) {
            return true;
        }
        TextHistory.State before = this.getState();
        boolean history = this.historyKeyPressed(typedChar, keyCode, modifiers);
        boolean flag = history || this.textFieldKeyPressed(typedChar, keyCode, modifiers);
        if (flag) {
            if (!history) {
                this.recordHistory(before, false);
            }
            this.updateCompletions(!history && !before.value().equals(this.getValue()));
            this.clearDisplayCache();
            this.setFocused(true);
            this.ensureCursorVisible();
            return true;
        } else if (typedChar == GLFW.GLFW_KEY_ESCAPE) {
            this.setFocused(false);
            return false;
        } else {
            // Consume all other keys, so they don't trigger screen actions (such as closing the screen) while typing
            return true;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // Don't delegate to EditBox, as that would consume the character into its own (unused) value
        if (event.isAllowedChatCharacter()) {
            TextHistory.State before = this.getState();
            this.textFieldHelper.insertText(event.codepointAsString());
            this.recordHistory(before, true);
            this.updateCompletions(true);
            this.clearDisplayCache();
            this.setFocused(true);
            this.ensureCursorVisible();
            return true;
        } else {
            return false;
        }
    }

    private TextHistory.State getState() {
        return new TextHistory.State(this.getValue(), this.textFieldHelper.getCursorPos(), this.textFieldHelper.getSelectionPos());
    }

    private void setState(TextHistory.State state) {
        this.setValuePassive(state.value());
        this.textFieldHelper.setSelectionRange(state.cursorPos(), state.selectionPos());
    }

    private void recordHistory(TextHistory.State before, boolean typing) {
        if (!before.value().equals(this.getValue())) {
            this.history.record(before, typing, Util.getMillis());
        }
    }

    /**
     * Replace the value as an undoable edit, keeping the cursor position where possible.
     * @param value The new value.
     */
    public void replaceValue(String value) {
        TextHistory.State before = this.getState();
        this.setState(new TextHistory.State(value, before.cursorPos(), before.selectionPos()));
        this.recordHistory(before, false);
        this.clearDisplayCache();
    }

    // Modifiers are read from the key event instead of the global keyboard state
    private static boolean hasControlDown(int modifiers) {
        return (modifiers & (InputQuirks.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL)) != 0;
    }

    public boolean canUndo() {
        return this.history.canUndo();
    }

    public boolean canRedo() {
        return this.history.canRedo();
    }

    public void undo() {
        this.applyEdit(this.history.undo(this.getState()));
    }

    public void redo() {
        this.applyEdit(this.history.redo(this.getState()));
    }

    /**
     * Toggle line comments on the lines touched by the cursor or selection, as an undoable edit.
     */
    public void toggleComment() {
        TextHistory.State before = this.getState();
        this.setState(TextEditing.toggleComment(before));
        this.recordHistory(before, false);
        this.clearDisplayCache();
    }

    private void applyEdit(@Nullable TextHistory.State state) {
        if (state != null) {
            this.setState(state);
            this.clearDisplayCache();
            this.ensureCursorVisible();
        }
    }

    // Ctrl+Shift+Z, which needs two modifiers, so it can not be a key mapping
    private static boolean isRedoAlternative(int key, int modifiers) {
        return key == GLFW.GLFW_KEY_Z && hasControlDown(modifiers) && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && (modifiers & GLFW.GLFW_MOD_ALT) == 0;
    }

    private boolean historyKeyPressed(int key, int scanCode, int modifiers) {
        TextHistory.State state;
        if (Keys.matches(Keys.UNDO, key, scanCode, modifiers)) {
            state = this.history.undo(this.getState());
        } else if (Keys.matches(Keys.REDO, key, scanCode, modifiers) || isRedoAlternative(key, modifiers)) {
            state = this.history.redo(this.getState());
        } else {
            return false;
        }
        if (state != null) {
            this.setState(state);
        }
        return true;
    }

    private boolean textFieldKeyPressed(int p_98153_, int p_98154_, int p_98155_) {
        KeyEvent event = new KeyEvent(p_98153_, p_98154_, p_98155_);
        boolean select = event.hasShiftDown();
        if (Keys.matches(Keys.COMMENT, p_98153_, p_98154_, p_98155_)) {
            this.setState(TextEditing.toggleComment(this.getState()));
            return true;
        } else if (Keys.matches(Keys.MOVE_LINES_UP, p_98153_, p_98154_, p_98155_)) {
            this.setState(TextEditing.moveLines(this.getState(), -1));
            return true;
        } else if (Keys.matches(Keys.MOVE_LINES_DOWN, p_98153_, p_98154_, p_98155_)) {
            this.setState(TextEditing.moveLines(this.getState(), 1));
            return true;
        } else if (event.isSelectAll()) {
            this.textFieldHelper.selectAll();
            return true;
        } else if (event.isCopy()) {
            this.textFieldHelper.copy();
            return true;
        } else if (event.isPaste()) {
            this.textFieldHelper.paste();
            return true;
        } else if (event.isCut()) {
            this.textFieldHelper.cut();
            return true;
        } else {
            TextFieldHelper.CursorStep cursorStep = EditorKeys.isWordModifier(p_98155_, InputQuirks.ON_OSX) ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
            boolean lineModifier = EditorKeys.isLineModifier(p_98155_, InputQuirks.ON_OSX);
            switch (p_98153_) {
                case 257:
                case 335:
                    this.setState(TextEditing.newline(this.getState(), this.indent));
                    return true;
                case GLFW.GLFW_KEY_TAB:
                    this.setState((p_98155_ & GLFW.GLFW_MOD_SHIFT) != 0 ? TextEditing.unindent(this.getState(), this.indent) : TextEditing.indent(this.getState(), this.indent));
                    return true;
                case 259:
                    this.textFieldHelper.removeFromCursor(-1, cursorStep);
                    return true;
                case 261:
                    this.textFieldHelper.removeFromCursor(1, cursorStep);
                    return true;
                case 262:
                    if (lineModifier) {
                        this.keyEnd(false, select);
                    } else {
                        this.textFieldHelper.moveBy(1, select, cursorStep);
                    }
                    return true;
                case 263:
                    if (lineModifier) {
                        this.keyHome(false, select);
                    } else {
                        this.textFieldHelper.moveBy(-1, select, cursorStep);
                    }
                    return true;
                case 264:
                    if (lineModifier) {
                        this.keyEnd(true, select);
                    } else {
                        this.changeLine(1, select);
                    }
                    return true;
                case 265:
                    if (lineModifier) {
                        this.keyHome(true, select);
                    } else {
                        this.changeLine(-1, select);
                    }
                    return true;
                case 266:
                    this.keyPage(-1, select);
                    return true;
                case 267:
                    this.keyPage(1, select);
                    return true;
                case 268:
                    this.keyHome(event.hasControlDownWithQuirk(), select);
                    return true;
                case 269:
                    this.keyEnd(event.hasControlDownWithQuirk(), select);
                    return true;
                default:
                    return false;
            }
        }
    }

    private void changeLine(int p_98098_, boolean select) {
        int cursorPos = this.textFieldHelper.getCursorPos();
        DisplayCache displayCache = this.getDisplayCache();
        int j = displayCache.changeLine(cursorPos, p_98098_);
        this.textFieldHelper.setCursorPos(j, select);
    }

    /**
     * @return The y position, moved up to the last fully visible row if it is below it.
     */
    private int clampToVisibleRows(double mouseY) {
        int rows = this.scrollBar != null ? this.scrollBar.getVisibleRows() : this.getHeight() / ROW_HEIGHT;
        return (int) Math.min(mouseY, this.getY() + rows * ROW_HEIGHT - 1);
    }

    private void keyPage(int direction, boolean select) {
        int rows = this.scrollBar != null ? this.scrollBar.getVisibleRows() : this.getHeight() / ROW_HEIGHT;
        this.changeLine(direction * rows, select);
        this.setFirstRow(this.firstRow + direction * rows, true);
    }

    /**
     * Scroll the viewport so that the cursor is visible.
     */
    public void ensureCursorVisible() {
        if (this.scrollBar != null) {
            int cursorLine = findLineFromPos(this.getDisplayCache().lineStarts, this.textFieldHelper.getCursorPos());
            int visibleRows = this.scrollBar.getVisibleRows();
            if (cursorLine < this.firstRow) {
                this.setFirstRow(cursorLine, true);
            } else if (cursorLine >= this.firstRow + visibleRows) {
                this.setFirstRow(cursorLine - visibleRows + 1, true);
            }
        }
    }

    private void keyHome(boolean document, boolean select) {
        if (document) {
            this.textFieldHelper.setCursorToStart(select);
        } else {
            int i = this.textFieldHelper.getCursorPos();
            int j = this.getDisplayCache().findLineStart(i);
            this.textFieldHelper.setCursorPos(j, select);
        }

    }

    private void keyEnd(boolean document, boolean select) {
        if (document) {
            this.textFieldHelper.setCursorToEnd(select);
        } else {
            DisplayCache displayCache = this.getDisplayCache();
            int i = this.textFieldHelper.getCursorPos();
            int j = displayCache.findLineEnd(i);
            this.textFieldHelper.setCursorPos(j, select);
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Determine lines to show
        DisplayCache displayCache = this.getDisplayCache();
        List<LineInfo> lines = Arrays.asList(displayCache.lines);
        int offsetY = 0;

        // Highlight all visible search matches
        if (this.searchQuery != null) {
            String value = this.getValue();
            int visibleStart = displayCache.lineStarts[Math.min(this.firstRow, displayCache.lineStarts.length - 1)];
            int visibleEnd = this.firstRow + this.getHeight() / ROW_HEIGHT < displayCache.lineStarts.length ? displayCache.lineStarts[this.firstRow + this.getHeight() / ROW_HEIGHT] : value.length();
            for (int match : this.getSearchMatches()) {
                if (match > visibleEnd) {
                    break;
                }
                for (int pos = Math.max(match, visibleStart); pos < Math.min(match + this.searchQuery.length(), visibleEnd); pos++) {
                    if (value.charAt(pos) != '\n') {
                        this.renderCharBackground(guiGraphics, displayCache, pos, IModHelpers.get().getBaseHelpers().RGBAToInt(255, 225, 120, 255));
                    }
                }
            }
        }

        // Highlight the bracket pair around the cursor
        if (this.isFocused()) {
            int[] brackets = TextEditing.findMatchingBrackets(this.getValue(), this.textFieldHelper.getCursorPos());
            if (brackets != null) {
                for (int bracket : brackets) {
                    this.renderCharBackground(guiGraphics, displayCache, bracket, IModHelpers.get().getBaseHelpers().RGBAToInt(160, 200, 255, 255));
                }
            }
        }

        // Mark the gutter of the line with a syntax error
        if (this.syntaxError != null && this.showLineNumbers) {
            for (LineInfo line : lines) {
                if (line.lineNumber + 1 == this.syntaxError.line()) {
                    // Fill the space between the editor border and the gutter separator
                    guiGraphics.fill(this.getX() - 1, line.y - 1, this.getX() + getLinesXOffset() - 2, line.y + ROW_HEIGHT - 1, IModHelpers.get().getBaseHelpers().RGBAToInt(255, 160, 160, 255));
                }
            }
        }

        // Draw lines
        int lastLineNumber = -1;
        for(LineInfo line : lines) {
            guiGraphics.text(this.font, line.getComponent(), line.x, line.y - offsetY, -16777216, false);
            // Draw line number
            if (this.showLineNumbers && lastLineNumber != line.lineNumber) {
                IModHelpers.get().getRenderHelpers().drawScaledString(
                        guiGraphics,
                        font,
                        String.valueOf(line.lineNumber + 1), // Line numbers start at 1, like in error messages
                        this.getX(),
                        line.y - offsetY + 2,
                        0.5f,
                        this.syntaxError != null && this.syntaxError.line() == line.lineNumber + 1 ? IModHelpers.get().getBaseHelpers().RGBAToInt(170, 0, 0, 255) : (line.hasCursor ? ARGB.opaque(0) : IModHelpers.get().getBaseHelpers().RGBAToInt(120, 120, 120, 255)),
                        false,
                        Font.DisplayMode.NORMAL
                );
            } else if (this.showLineNumbers) {
                // Mark wrapped continuation lines
                IModHelpers.get().getRenderHelpers().drawScaledString(guiGraphics, font, "\u21B3", this.getX() + 4, line.y - offsetY + 2, 0.5f,
                        IModHelpers.get().getBaseHelpers().RGBAToInt(160, 160, 160, 255), false, Font.DisplayMode.NORMAL);
            }
            lastLineNumber = line.lineNumber;
        }

        if (this.syntaxError != null) {
            this.renderSyntaxError(guiGraphics, displayCache, lines, mouseX, mouseY);
        }

        // Show highlighting and cursor
        this.renderHighlight(guiGraphics, displayCache.selection);
        if (displayCache.cursor != null) {
            this.renderCursor(guiGraphics, displayCache.cursor, displayCache.cursorAtEnd);
        }

        // Render scrollbar
        if (this.scrollBar != null) {
            // Mark the position of the syntax error on the scrollbar, so it can be found when scrolled out of view.
            // This is drawn below the handle, which only covers it when the error is roughly in view.
            if (this.syntaxError != null && displayCache.linesTotal > this.scrollBar.getVisibleRows()) {
                int errorRow = findLineFromPos(displayCache.lineStarts, TextEditing.lineOffset(this.getValue(), this.syntaxError.line()));
                int y = this.getY() + (int) ((long) errorRow * (this.getHeight() - 2) / displayCache.linesTotal);
                int x = this.getX() + this.getWidth() - SCROLLBAR_WIDTH;
                guiGraphics.fill(x, y, x + SCROLLBAR_WIDTH - 1, y + 2, IModHelpers.get().getBaseHelpers().RGBAToInt(220, 0, 0, 255));

                // Show the message when hovering the mark
                if (mouseX >= x && mouseX < x + SCROLLBAR_WIDTH - 1 && mouseY >= y - 1 && mouseY < y + 3) {
                    guiGraphics.setTooltipForNextFrame(this.font, this.font.split(Component.literal(this.syntaxError.message()), 200), mouseX, mouseY);
                }
            }

            this.scrollBar.extractRenderState(guiGraphics, getX(), getY(), partialTicks);
        }
    }

    /**
     * Draw the completions while typing, or else the signature of the call at the cursor.
     * The using screen must call this after drawing everything else that may overlap them, such as slots.
     */
    public void extractPopups(GuiGraphicsExtractor guiGraphics) {
        DisplayCache displayCache = this.getDisplayCache();
        if (displayCache.cursor != null && this.isFocused() && this.visible) {
            Pos2i cursor = this.convertLocalToScreen(displayCache.cursor);
            if (this.completions != null) {
                this.renderCompletions(guiGraphics, cursor);
            } else if (this.signatureProvider != null) {
                Component signature = this.signatureProvider.apply(this.getValue(), this.getCursorPos());
                if (signature != null) {
                    this.renderSignature(guiGraphics, cursor, signature);
                }
            }
        }
    }

    private void renderSyntaxError(GuiGraphicsExtractor guiGraphics, DisplayCache displayCache, List<LineInfo> lines, int mouseX, int mouseY) {
        String value = this.getValue();
        // Underline the character at the error position
        int pos = Math.min(value.length(), TextEditing.lineOffset(value, this.syntaxError.line()) + Math.max(0, this.syntaxError.column() - 1));
        int line = findLineFromPos(displayCache.lineStarts, pos);
        int row = line - this.firstRow;
        int errorCenterX = -1;
        if (row >= 0 && row < this.getHeight() / ROW_HEIGHT) {
            int x = this.font.width(value.substring(displayCache.lineStarts[line], pos));
            int width = pos < value.length() && value.charAt(pos) != '\n' ? this.font.width(value.substring(pos, pos + 1)) : 4;
            Pos2i screenPos = this.convertLocalToScreen(new Pos2i(x, row * ROW_HEIGHT));
            guiGraphics.fill(screenPos.x, screenPos.y + ROW_HEIGHT - 2, screenPos.x + width, screenPos.y + ROW_HEIGHT - 1, IModHelpers.get().getBaseHelpers().RGBAToInt(220, 0, 0, 255));
            errorCenterX = screenPos.x + width / 2;
        }

        // Show the message when hovering the error line, which may be wrapped over multiple rows
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        boolean hovering = false;
        for (LineInfo lineInfo : lines) {
            if (lineInfo.lineNumber + 1 == this.syntaxError.line()) {
                top = Math.min(top, lineInfo.y);
                bottom = Math.max(bottom, lineInfo.y + ROW_HEIGHT);
                hovering |= mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                        && mouseY >= lineInfo.y && mouseY < lineInfo.y + ROW_HEIGHT;
            }
        }
        if (hovering) {
            List<FormattedCharSequence> message = this.font.split(Component.literal(this.syntaxError.message()), 200);
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            // Same height as vanilla tooltips of text lines
            int tooltipHeight = message.size() * 10 - (message.size() == 1 ? 2 : 0);

            // Like in other editors, show the message below the line, or above it if there is no room
            boolean below = bottom + TOOLTIP_ARROW + TOOLTIP_BORDER + tooltipHeight + TOOLTIP_BORDER <= screenHeight
                    || top - TOOLTIP_ARROW - TOOLTIP_BORDER - tooltipHeight - TOOLTIP_BORDER < 0;
            int tooltipY = below ? bottom + TOOLTIP_ARROW + TOOLTIP_BORDER : top - TOOLTIP_ARROW - TOOLTIP_BORDER - tooltipHeight;
            int anchorX = errorCenterX >= 0 ? errorCenterX - 12 : mouseX + 12;
            guiGraphics.setTooltipForNextFrame(this.font, message, (screenWidth, screenHeightIgnored, mouseXIgnored, mouseYIgnored, width, height) ->
                    new Vector2i(Math.max(TOOLTIP_BORDER, Math.min(anchorX, screenWidth - width - TOOLTIP_BORDER)), tooltipY), mouseX, mouseY, true);

            // Point at the column of the error
            if (errorCenterX >= 0) {
                this.renderTooltipArrow(guiGraphics, errorCenterX, below ? bottom : top - 1, below);
            }
        }
    }

    /**
     * Draw an arrow between a tooltip and the given position, in the style of vanilla tooltips.
     */
    private void renderTooltipArrow(GuiGraphicsExtractor guiGraphics, int x, int tipY, boolean pointUp) {
        for (int i = 0; i < TOOLTIP_ARROW; i++) {
            int y = pointUp ? tipY + i : tipY - i;
            guiGraphics.fill(x - i, y, x + i + 1, y + 1, TOOLTIP_BACKGROUND);
            guiGraphics.fill(x - i, y, x - i + 1, y + 1, TOOLTIP_BORDER_COLOR);
            guiGraphics.fill(x + i, y, x + i + 1, y + 1, TOOLTIP_BORDER_COLOR);
        }
    }

    private void renderCharBackground(GuiGraphicsExtractor guiGraphics, DisplayCache displayCache, int pos, int color) {
        int line = findLineFromPos(displayCache.lineStarts, pos);
        int row = line - this.firstRow;
        if (row < 0 || row >= this.getHeight() / ROW_HEIGHT) {
            return;
        }
        String value = this.getValue();
        int x = this.font.width(value.substring(displayCache.lineStarts[line], pos));
        int width = this.font.width(value.substring(pos, pos + 1));
        Pos2i screenPos = this.convertLocalToScreen(new Pos2i(x, row * ROW_HEIGHT));
        guiGraphics.fill(screenPos.x, screenPos.y - 1, screenPos.x + width, screenPos.y + ROW_HEIGHT - 1, color);
    }

    private void renderCompletions(GuiGraphicsExtractor guiGraphics, Pos2i cursor) {
        List<ScriptCompletions.Item> items = this.completions.items();
        int rows = Math.min(items.size(), COMPLETION_ROWS);
        int firstItem = Math.max(0, Math.min(this.completionIndex - rows / 2, items.size() - rows));
        // Grow beyond the editor if needed, as signatures can be long
        int width = getTextWidth();
        for (int i = 0; i < rows; i++) {
            ScriptCompletions.Item item = items.get(firstItem + i);
            width = Math.max(width, this.font.width(item.name()) + 4 + this.font.width(item.detail()) + 2);
        }
        Pair<Integer, Integer> xAndWidth = this.fitHorizontally(width);
        int x = xAndWidth.getLeft();
        width = xAndWidth.getRight();
        int height = rows * ROW_HEIGHT + 2;
        // Show below the line of the cursor if possible, otherwise above it
        int y = cursor.y + ROW_HEIGHT;
        if (y + height > this.getY() + this.getHeight() && cursor.y - height - 1 >= this.getY()) {
            y = cursor.y - height - 1;
        }
        this.completionsX = x;
        this.completionsY = y;
        this.completionsWidth = width;
        this.completionsFirstItem = firstItem;
        this.completionsRows = rows;
        // Dark, like tooltips, as the colors of value types are made for those
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, POPUP_BORDER);
        guiGraphics.fill(x, y, x + width, y + height, POPUP_BACKGROUND);
        for (int i = 0; i < rows; i++) {
            ScriptCompletions.Item item = items.get(firstItem + i);
            int rowY = y + 1 + i * ROW_HEIGHT;
            if (firstItem + i == this.completionIndex) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, POPUP_SELECTED);
            }
            String name = this.font.plainSubstrByWidth(item.name(), width - 2);
            guiGraphics.text(this.font, name, x + 1, rowY + 1, POPUP_TEXT, false);
            int detailX = x + 1 + this.font.width(name) + 4;
            int detailWidth = x + width - 1 - detailX;
            if (detailWidth > 10) {
                guiGraphics.text(this.font, this.truncate(item.detail(), detailWidth), detailX, rowY + 1, POPUP_DETAIL, false);
            }
        }
    }

    /**
     * @return The x position and width of a popup at the start of the text, moved left if it would leave the screen.
     */
    private Pair<Integer, Integer> fitHorizontally(int width) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        width = Math.min(width, screenWidth - 4);
        int x = Math.min(this.getX() + getLinesXOffset(), screenWidth - 2 - width);
        return Pair.of(x, width);
    }

    private void renderSignature(GuiGraphicsExtractor guiGraphics, Pos2i cursor, Component signature) {
        Pair<Integer, Integer> xAndWidth = this.fitHorizontally(this.font.width(signature) + 2);
        int x = xAndWidth.getLeft();
        int width = xAndWidth.getRight();
        // Show above the line of the cursor if possible, otherwise below it
        int y = cursor.y - ROW_HEIGHT - 1;
        if (y < this.getY() - 1) {
            y = cursor.y + ROW_HEIGHT + 1;
        }
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + ROW_HEIGHT + 1, POPUP_BORDER);
        guiGraphics.fill(x, y, x + width, y + ROW_HEIGHT, POPUP_BACKGROUND);
        guiGraphics.text(this.font, this.truncate(signature, width - 2), x + 1, y + 1, POPUP_TEXT, false);
    }

    /**
     * @return The text, cut off with an ellipsis if it is wider than the given width.
     */
    private FormattedCharSequence truncate(Component text, int width) {
        if (this.font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText truncated = FormattedText.composite(this.font.substrByWidth(text, Math.max(0, width - this.font.width("\u2026"))), FormattedText.of("\u2026"));
        return Language.getInstance().getVisualOrder(truncated);
    }

    private void renderCursor(GuiGraphicsExtractor guiGraphics, Pos2i pos, boolean cursorAtEnd) {
        if (this.isFocused() && this.frameTick / 6 % 2 == 0) {
            pos = this.convertLocalToScreen(pos);
            if (!cursorAtEnd) {
                guiGraphics.fill(pos.x, pos.y - 1, pos.x + 1, pos.y + 9, -16777216);
            } else {
                guiGraphics.text(this.font, "_", pos.x, pos.y, ARGB.opaque(0), false);
            }
        }

    }

    private void renderHighlight(GuiGraphicsExtractor guiGraphics, Rect2i[] highlightAreas) {
        for (Rect2i rect2i : highlightAreas) {
            int i = rect2i.getX();
            int j = rect2i.getY();
            int k = i + rect2i.getWidth();
            int l = j + rect2i.getHeight();
            // Dimmer when not focused
            guiGraphics.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, i, j, k, l, this.isFocused() ? 0xFF0000FF : 0xFF000064);
        }
    }

    private int getTextWidth() {
        // Don't render text below the scrollbar
        return this.getWidth() - getLinesXOffset() - (this.scrollBar != null ? SCROLLBAR_WIDTH : 0);
    }

    private int getLinesXOffset() {
        return this.showLineNumbers ? 14 : 0;
    }

    private Pos2i convertScreenToLocal(Pos2i posScreen) {
        return new Pos2i(posScreen.x - this.getX() - getLinesXOffset(), posScreen.y - this.getY());
    }

    private Pos2i convertLocalToScreen(Pos2i posLocal) {
        return new Pos2i(this.getX() + posLocal.x + getLinesXOffset(), this.getY() + posLocal.y);
    }

    private DisplayCache getDisplayCache() {
        if (this.displayCache == null) {
            this.displayCache = this.rebuildDisplayCache();
        }

        return this.displayCache;
    }

    private void clearDisplayCache() {
        this.displayCache = null;
    }

    private DisplayCache rebuildDisplayCache() {
        String s = this.getValue();
        if (s.isEmpty()) {
            return emptyDisplayCache;
        } else {
            int cursorPos = this.textFieldHelper.getCursorPos();
            int selectionPos = this.textFieldHelper.getSelectionPos();
            TextLayout layout = this.getLayout(s);

            // Keep scroll position within bounds, as the text may have shrunk
            if (this.scrollBar != null) {
                int maxFirstRow = Math.max(0, layout.lineStarts.length - this.scrollBar.getVisibleRows());
                if (this.firstRow > maxFirstRow) {
                    this.firstRow = maxFirstRow;
                    this.scrollBar.setFirstRow(this.firstRow, false);
                }
            }

            int cursorLineNumber = layout.lineNumbers[findLineFromPos(layout.lineStarts, cursorPos)];
            List<LineInfo> linesAll = Lists.newArrayList();
            for (int k3 = 0; k3 < layout.lineStarts.length; k3++) {
                Pos2i bookeditscreen$pos2i1 = this.convertLocalToScreen(new Pos2i(0, (k3 - this.firstRow) * 9)); // Offset firstRow!
                linesAll.add(new LineInfo(layout, k3, bookeditscreen$pos2i1.x, bookeditscreen$pos2i1.y, layout.lineNumbers[k3] == cursorLineNumber));
            }
            IntList linePositionsOld = IntArrayList.wrap(layout.lineStarts);
            StringSplitter stringsplitter = this.font.getSplitter();

            // --- Changed ---
            // Slice lines based on scroll position
            int linesTotal = linesAll.size();
            List<LineInfo> lines = linesAll;
            IntList lineStarts = linePositionsOld;
            if (this.scrollBar != null) {
                lines = lines.subList(this.firstRow, Math.min(this.firstRow + this.scrollBar.getVisibleRows(), lines.size()));
            }

            int[] lineStartsArr = lineStarts.toIntArray();
            boolean flag = cursorPos == s.length();
            int cursorLine = findLineFromPos(lineStartsArr, cursorPos);
            int cursorX = this.font.width(s.substring(lineStartsArr[cursorLine], cursorPos));
            Pos2i cursor = new Pos2i(cursorX, (cursorLine - this.firstRow) * 9); // We have to offset the firstRow here!
            // Hide cursor if out of view, including the partially visible row below the last line
            if (cursor.y < 0 || cursor.y + ROW_HEIGHT > getHeight()) {
                cursor = null;
            }

            List<Rect2i> selection = Lists.newArrayList();
            if (cursorPos != selectionPos) {
                int l2 = Math.min(cursorPos, selectionPos);
                int i1 = Math.max(cursorPos, selectionPos);
                int lineStart = findLineFromPos(lineStartsArr, l2);
                int lineEnd = findLineFromPos(lineStartsArr, i1);
                if (lineStart == lineEnd) {
                    int l1 = lineStart * 9;
                    int i2 = lineStartsArr[lineStart];
                    selection.add(this.createPartialLineSelection(s, stringsplitter, l2, i1, l1, i2));
                } else {
                    int i3 = lineStart + 1 > lineStartsArr.length ? s.length() : lineStartsArr[lineStart + 1];
                    selection.add(this.createPartialLineSelection(s, stringsplitter, l2, i3, lineStart * 9, lineStartsArr[lineStart]));

                    for(int j3 = lineStart + 1; j3 < lineEnd; ++j3) {
                        int j2 = j3 * 9;
                        String s1 = s.substring(lineStartsArr[j3], lineStartsArr[j3 + 1]);
                        int k2 = (int)stringsplitter.stringWidth(s1);
                        selection.add(this.createSelection(new Pos2i(0, j2), new Pos2i(k2, j2 + 9)));
                    }

                    selection.add(this.createPartialLineSelection(s, stringsplitter, lineStartsArr[lineEnd], i1, lineEnd * 9, lineStartsArr[lineEnd]));
                }
            }
            // Hide out-of-view selections and offset them, including the partially visible row below the last line
            List<Rect2i> selectionNew = Lists.newArrayList();
            for (Rect2i rect2i : selection) {
                Rect2i rect2iNew = new Rect2i(rect2i.getX(), rect2i.getY() - this.firstRow * ROW_HEIGHT, rect2i.getWidth(), rect2i.getHeight());
                if (rect2iNew.getY() - this.getY() >= 0 && rect2iNew.getY() + rect2iNew.getHeight() - this.getY() <= getHeight()) {
                    selectionNew.add(rect2iNew);
                }
            }

            return new DisplayCache(s, cursor, flag, lineStartsArr, lines.toArray(new LineInfo[0]), linesAll.toArray(new LineInfo[0]), selectionNew.toArray(new Rect2i[0]), linesTotal, this.firstRow);
        }
    }

    /**
     * @return The wrapped and marked up lines of a text, which is only recomputed when the text changes.
     */
    private TextLayout getLayout(String s) {
        if (this.layout == null || !this.layout.text.equals(s)) {
            IntList lineStarts = new IntArrayList();
            List<String> contents = Lists.newArrayList();
            IntList lineNumbers = new IntArrayList();
            MutableBoolean trailingNewline = new MutableBoolean();
            MutableInt lineNumber = new MutableInt();
            StringSplitter splitter = this.font.getSplitter();
            int[] starts = TextEditing.wrapLines(s, getTextWidth(), c -> splitter.stringWidth(String.valueOf((char) c)));
            for (int i = 0; i < starts.length; i++) {
                String stringPart = s.substring(starts[i], i + 1 < starts.length ? starts[i + 1] : s.length());
                boolean hasNewLine = stringPart.endsWith("\n");
                trailingNewline.setValue(hasNewLine);
                lineStarts.add(starts[i]);
                contents.add(StringUtils.stripEnd(stringPart, " \n"));
                lineNumbers.add(lineNumber.intValue());
                if (hasNewLine) {
                    lineNumber.increment();
                }
            }
            // Add an empty line after a trailing newline, so the cursor can be placed and scrolled there
            if (trailingNewline.isTrue()) {
                lineStarts.add(s.length());
                contents.add("");
                lineNumbers.add(lineNumber.intValue());
            }
            // Mark up the full text at once, so that constructs such as block comments can span multiple lines
            this.layout = new TextLayout(s, lineStarts.toIntArray(), contents.toArray(new String[0]), lineNumbers.toIntArray(), new Markup(this.markup(s)));
        }
        return this.layout;
    }

    private List<Pair<Style, String>> markup(String text) {
        if (this.markupProvider != null) {
            List<Pair<Style, String>> segments = this.markupProvider.markup(text);
            // Ignore markup that doesn't match the text
            if (segments.stream().mapToInt(segment -> segment.getRight().length()).sum() == text.length()) {
                return segments;
            }
        }
        return Stream.of(Pair.of(Style.EMPTY, text)).collect(Collectors.toList());
    }

    /**
     * Styled segments of a text, which can be sliced by position.
     */
    static class Markup {
        private final int[] starts;
        private final Style[] styles;

        Markup(List<Pair<Style, String>> segments) {
            this.starts = new int[segments.size()];
            this.styles = new Style[segments.size()];
            int pos = 0;
            for (int i = 0; i < segments.size(); i++) {
                this.starts[i] = pos;
                this.styles[i] = segments.get(i).getLeft();
                pos += segments.get(i).getRight().length();
            }
        }

        List<Pair<Style, String>> slice(String text, int from, int to) {
            List<Pair<Style, String>> result = Lists.newArrayList();
            if (from >= to) {
                return result;
            }
            int i = Math.max(0, findLineFromPos(this.starts, from));
            while (i < this.starts.length && this.starts[i] < to) {
                int segmentEnd = i + 1 < this.starts.length ? this.starts[i + 1] : text.length();
                int start = Math.max(from, this.starts[i]);
                int end = Math.min(to, segmentEnd);
                if (start < end) {
                    result.add(Pair.of(this.styles[i], text.substring(start, end)));
                }
                i++;
            }
            return result;
        }
    }

    static int findLineFromPos(int[] p_98150_, int p_98151_) {
        int i = Arrays.binarySearch(p_98150_, p_98151_);
        return i < 0 ? -(i + 2) : i;
    }

    private Rect2i createPartialLineSelection(String p_98120_, StringSplitter p_98121_, int p_98122_, int p_98123_, int p_98124_, int p_98125_) {
        String s = p_98120_.substring(p_98125_, p_98122_);
        String s1 = p_98120_.substring(p_98125_, p_98123_);
        Pos2i bookeditscreen$pos2i = new Pos2i((int)p_98121_.stringWidth(s), p_98124_);
        Pos2i bookeditscreen$pos2i1 = new Pos2i((int)p_98121_.stringWidth(s1), p_98124_ + 9);
        return this.createSelection(bookeditscreen$pos2i, bookeditscreen$pos2i1);
    }

    private Rect2i createSelection(Pos2i p_98117_, Pos2i p_98118_) {
        Pos2i bookeditscreen$pos2i = this.convertLocalToScreen(p_98117_);
        Pos2i bookeditscreen$pos2i1 = this.convertLocalToScreen(p_98118_);
        int i = Math.min(bookeditscreen$pos2i.x, bookeditscreen$pos2i1.x);
        int j = Math.max(bookeditscreen$pos2i.x, bookeditscreen$pos2i1.x);
        int k = Math.min(bookeditscreen$pos2i.y, bookeditscreen$pos2i1.y);
        int l = Math.max(bookeditscreen$pos2i.y, bookeditscreen$pos2i1.y);
        return new Rect2i(i, k, j - i, l - k);
    }

    static class DisplayCache {
        private final String fullText;
        @Nullable
        final Pos2i cursor;
        final boolean cursorAtEnd;
        private final int[] lineStarts;
        final LineInfo[] lines;
        final LineInfo[] linesAll;
        final Rect2i[] selection;
        final int linesTotal;
        final int firstRow;

        public DisplayCache(String p_98201_, Pos2i cursor, boolean p_98203_, int[] p_98204_, LineInfo[] lines, LineInfo[] linesAll, Rect2i[] selection, int linesTotal, int firstRow) {
            this.fullText = p_98201_;
            this.cursor = cursor;
            this.cursorAtEnd = p_98203_;
            this.lineStarts = p_98204_;
            this.lines = lines;
            this.linesAll = linesAll;
            this.selection = selection;
            this.linesTotal = linesTotal;
            this.firstRow = firstRow;
        }

        public int getIndexAtPosition(Font p_98214_, Pos2i p_98215_) {
            int i = p_98215_.y / 9 + this.firstRow;
            if (i < 0) {
                return 0;
            } else if (i >= this.linesAll.length) {
                return this.fullText.length();
            } else {
                LineInfo bookeditscreen$lineinfo = this.linesAll[i];
                return this.lineStarts[i] + p_98214_.getSplitter().plainIndexAtWidth(bookeditscreen$lineinfo.contents, p_98215_.x, Style.EMPTY);
            }
        }

        public int changeLine(int p_98211_, int p_98212_) {
            int i = WidgetTextArea.findLineFromPos(this.lineStarts, p_98211_);
            int j = Math.max(0, Math.min(this.lineStarts.length - 1, i + p_98212_));
            int l = p_98211_ - this.lineStarts[i];
            int i1 = this.linesAll[j].contents.length();
            return this.lineStarts[j] + Math.min(l, i1);
        }

        public int findLineStart(int p_98209_) {
            int i = WidgetTextArea.findLineFromPos(this.lineStarts, p_98209_);
            return this.lineStarts[i];
        }

        public int findLineEnd(int p_98219_) {
            int i = WidgetTextArea.findLineFromPos(this.lineStarts, p_98219_);
            return this.lineStarts[i] + this.linesAll[i].contents.length();
        }
    }

    /**
     * The wrapped lines of a text, independent of the scroll and cursor position.
     */
    static class TextLayout {
        final String text;
        final int[] lineStarts;
        final String[] contents;
        final int[] lineNumbers;
        final Markup markup;
        final Component[] components;

        TextLayout(String text, int[] lineStarts, String[] contents, int[] lineNumbers, Markup markup) {
            this.text = text;
            this.lineStarts = lineStarts;
            this.contents = contents;
            this.lineNumbers = lineNumbers;
            this.markup = markup;
            this.components = new Component[lineStarts.length];
        }

        /**
         * Lazily create the styled component of a line, as only visible lines are rendered.
         */
        Component getComponent(int line) {
            if (this.components[line] == null) {
                MutableComponent component = Component.literal("");
                for (Pair<Style, String> value : this.markup.slice(this.text, this.lineStarts[line], this.lineStarts[line] + this.contents[line].length())) {
                    component = component.append(Component.literal(value.getRight()).setStyle(value.getLeft()));
                }
                this.components[line] = component;
            }
            return this.components[line];
        }
    }

    static class LineInfo {
        private final TextLayout layout;
        private final int line;
        final String contents;
        final int x;
        final int y;
        final int lineNumber;
        final boolean hasCursor;

        public LineInfo(TextLayout layout, int line, int x, int y, boolean hasCursor) {
            this.layout = layout;
            this.line = line;
            this.contents = layout.contents[line];
            this.x = x;
            this.y = y;
            this.lineNumber = layout.lineNumbers[line];
            this.hasCursor = hasCursor;
        }

        public Component getComponent() {
            return this.layout.getComponent(this.line);
        }
    }

    static class Pos2i {
        public final int x;
        public final int y;

        Pos2i(int p_98249_, int p_98250_) {
            this.x = p_98249_;
            this.y = p_98250_;
        }
    }

    public static interface IMarkupProvider {
        /**
         * @param text The full text.
         * @return Styled segments that together form the full text.
         */
        public List<Pair<Style, String>> markup(String text);
    }

    public static interface ShortcutListener {
        public boolean keyPressed(int keyCode, int scanCode, int modifiers);
    }

}
