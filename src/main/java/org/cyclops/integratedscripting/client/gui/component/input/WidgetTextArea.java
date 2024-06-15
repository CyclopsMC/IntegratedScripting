package org.cyclops.integratedscripting.client.gui.component.input;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.component.input.IInputListener;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A widget to edit multi-line text.
 * Inspired by {@link BookEditScreen}.
 *
 * The using screen must add this as a child and call the following method from its respective method:
 * * {@link #tick()}
 * * {@link #mouseClicked(double, double, int)}
 * * {@link #mouseDragged(double, double, int, double, double)}
 * * {@link #mouseScrolled(double, double, double, double)}
 * * {@link #keyPressed(int, int, int)}
 * * {@link #charTyped(char, int)}
 *
 * @author rubensworks
 */
@OnlyIn(Dist.CLIENT)
public class WidgetTextArea extends AbstractWidget implements GuiEventListener {

    public static final int ROW_HEIGHT = 9;

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
    private WidgetTextArea.DisplayCache emptyDisplayCache = createEmptyDisplayCache();
    @Nullable
    private WidgetTextArea.DisplayCache displayCache = emptyDisplayCache;
    private long lastClickTime;
    private int lastIndex = -1;
    @Nullable
    private WidgetScrollBar scrollBar;
    private int firstRow;

    public WidgetTextArea(Font font, int x, int y, int width, int height, Component narrationMessage, boolean scrollBar, boolean showLineNumbers) {
        super(x, y, width, height, narrationMessage);
        this.font = font;
        this.showLineNumbers = showLineNumbers;
        this.textFieldHelper = new TextFieldHelperExtended(this::getValue, this::setValuePassive, this::getClipboard, this::setClipboard, s -> true, this::setSelected, this::onCursorPosChanged);
        if (scrollBar) {
            this.scrollBar = new WidgetScrollBar(x + width - 14, y, height,
                    Component.translatable("gui.cyclopscore.scrollbar"), firstRow -> setFirstRow(firstRow, false), height / ROW_HEIGHT) {
                @Override
                public int getTotalRows() {
                    return getDisplayCache().linesTotal;
                }

                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                    // Only show scrollbar if needed
                    if (this.needsScrollBars()) {
                        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
                    }
                }
            };
        }
    }

    private DisplayCache createEmptyDisplayCache() {
        Pos2i pos = convertLocalToScreen(new Pos2i(0, 0));
        return new DisplayCache("", pos, true, new int[]{0}, new LineInfo[]{new LineInfo(Collections.emptyList(), pos.x, pos.y, 0, false)}, new LineInfo[]{new LineInfo(Collections.emptyList(), pos.x, pos.y, 0, false)}, new Rect2i[0], 0, 0);
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

    public void setMarkupProvider(@Nullable IMarkupProvider markupProvider) {
        this.markupProvider = markupProvider;
    }

    public void setValue(String value) {
        this.setValuePassive(value);

        textFieldHelper.setCursorToStart();
        textFieldHelper.setSelectionPos(textFieldHelper.getCursorPos());
        if (this.scrollBar != null) {
            this.firstRow = 0;
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
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 1 && mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            // Select everything
            this.setFocused(true);
            textFieldHelper.selectAll();
            return true;
        } else {
            if (mouseButton == 0 && mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                this.setFocused(true);
                long i = Util.getMillis();
                DisplayCache displayCache = this.getDisplayCache();
                int j = displayCache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i((int)mouseX, (int)mouseY)));
                if (j >= 0) {
                    if (j == this.lastIndex && i - this.lastClickTime < 250L) {
                        if (!this.textFieldHelper.isSelecting()) {
                            this.selectWord(j);
                        } else {
                            this.textFieldHelper.selectAll();
                        }
                    } else {
                        this.textFieldHelper.setCursorPos(j, Screen.hasShiftDown());
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
        if (this.scrollBar != null && mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height
                && this.scrollBar.mouseScrolled(mouseX, mouseY, mouseZ, scroll)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double offsetX, double offsetY) {
        if (this.scrollBar != null && mouseX >= this.getX() + this.width - 12 && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            return this.scrollBar.mouseDragged(mouseX, mouseY, mouseButton, offsetX, offsetY);
        }

        if (mouseButton == 0 && mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height) {
            DisplayCache bookeditscreen$displaycache = this.getDisplayCache();
            int i = bookeditscreen$displaycache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i((int)mouseX, (int)mouseY)));
            this.textFieldHelper.setCursorPos(i, true);
            this.clearDisplayCache();
            this.setFocused(true);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {
        boolean flag = this.textFieldKeyPressed(typedChar, keyCode, modifiers);
        if (flag) {
            this.clearDisplayCache();
            this.setFocused(true);
            return true;
        } else {
            this.setFocused(false);
            return false;
        }
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        if (super.charTyped(typedChar, keyCode)) {
            this.setFocused(true);
            return true;
        } else if (SharedConstants.isAllowedChatCharacter(typedChar)) {
            this.textFieldHelper.insertText(Character.toString(typedChar));
            this.clearDisplayCache();
            this.setFocused(true);
            return true;
        } else {
            this.setFocused(false);
            return false;
        }
    }

    private boolean textFieldKeyPressed(int p_98153_, int p_98154_, int p_98155_) {
        if (Screen.isSelectAll(p_98153_)) {
            this.textFieldHelper.selectAll();
            return true;
        } else if (Screen.isCopy(p_98153_)) {
            this.textFieldHelper.copy();
            return true;
        } else if (Screen.isPaste(p_98153_)) {
            this.textFieldHelper.paste();
            return true;
        } else if (Screen.isCut(p_98153_)) {
            this.textFieldHelper.cut();
            return true;
        } else {
            TextFieldHelper.CursorStep cursorStep = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
            switch (p_98153_) {
                case 257:
                case 335:
                    this.textFieldHelper.insertText("\n");
                    return true;
                case 259:
                    this.textFieldHelper.removeFromCursor(-1, cursorStep);
                    return true;
                case 261:
                    this.textFieldHelper.removeFromCursor(1, cursorStep);
                    return true;
                case 262:
                    this.textFieldHelper.moveBy(1, Screen.hasShiftDown(), cursorStep);
                    return true;
                case 263:
                    this.textFieldHelper.moveBy(-1, Screen.hasShiftDown(), cursorStep);
                    return true;
                case 264:
                    this.keyDown();
                    return true;
                case 265:
                    this.keyUp();
                    return true;
                case 266:
//                    this.backButton.onPress();
                    return true;
                case 267:
//                    this.forwardButton.onPress();
                    return true;
                case 268:
                    this.keyHome();
                    return true;
                case 269:
                    this.keyEnd();
                    return true;
                default:
                    return false;
            }
        }
    }

    private void keyUp() {
        this.changeLine(-1);
    }

    private void keyDown() {
        this.changeLine(1);
    }

    private void changeLine(int p_98098_) {
        int cursorPos = this.textFieldHelper.getCursorPos();
        DisplayCache displayCache = this.getDisplayCache();
        int j = displayCache.changeLine(cursorPos, p_98098_);
        this.textFieldHelper.setCursorPos(j, Screen.hasShiftDown());

        // Modify scroll position when cursor goes out of screen
        if (this.scrollBar != null) {
            int cursorLine = findLineFromPos(Arrays.stream(displayCache.lineStarts).toArray(), cursorPos);
            if (cursorLine < this.firstRow + 2) {
                // Cursor is above viewport
                this.setFirstRow(cursorLine - 2, true);
            } else if (cursorLine > this.firstRow + this.scrollBar.getVisibleRows() - 3) {
                // Cursor is below viewport
                this.setFirstRow(cursorLine - (this.scrollBar.getVisibleRows() - 3), true);
            }
        }
    }

    private void keyHome() {
        if (Screen.hasControlDown()) {
            this.textFieldHelper.setCursorToStart(Screen.hasShiftDown());
        } else {
            int i = this.textFieldHelper.getCursorPos();
            int j = this.getDisplayCache().findLineStart(i);
            this.textFieldHelper.setCursorPos(j, Screen.hasShiftDown());
        }

    }

    private void keyEnd() {
        if (Screen.hasControlDown()) {
            this.textFieldHelper.setCursorToEnd(Screen.hasShiftDown());
        } else {
            DisplayCache displayCache = this.getDisplayCache();
            int i = this.textFieldHelper.getCursorPos();
            int j = displayCache.findLineEnd(i);
            this.textFieldHelper.setCursorPos(j, Screen.hasShiftDown());
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Determine lines to show
        DisplayCache displayCache = this.getDisplayCache();
        List<LineInfo> lines = Arrays.asList(displayCache.lines);
        int offsetY = 0;

        // Draw lines
        int lastLineNumber = -1;
        for(LineInfo line : lines) {
            guiGraphics.drawString(this.font, line.asComponent, line.x, line.y - offsetY, -16777216, false);
            // Draw line number
            if (this.showLineNumbers && lastLineNumber != line.lineNumber) {
                RenderHelpers.drawScaledString(
                        guiGraphics,
                        font,
                        String.valueOf(line.lineNumber),
                        this.getX(),
                        line.y - offsetY + 2,
                        0.5f,
                        line.hasCursor ? 0 : Helpers.RGBToInt(120, 120, 120),
                        false,
                        Font.DisplayMode.NORMAL
                );
            }
            lastLineNumber = line.lineNumber;
        }

        // Show highlighting and cursor
        this.renderHighlight(displayCache.selection);
        if (displayCache.cursor != null) {
            this.renderCursor(guiGraphics, displayCache.cursor, displayCache.cursorAtEnd);
        }

        // Render scrollbar
        if (this.scrollBar != null) {
            this.scrollBar.render(guiGraphics, getX(), getY(), partialTicks);
        }
    }

    private void renderCursor(GuiGraphics guiGraphics, Pos2i pos, boolean cursorAtEnd) {
        if (this.isFocused() && this.frameTick / 6 % 2 == 0) {
            pos = this.convertLocalToScreen(pos);
            if (!cursorAtEnd) {
                guiGraphics.fill(pos.x, pos.y - 1, pos.x + 1, pos.y + 9, -16777216);
            } else {
                guiGraphics.drawString(this.font, "_", pos.x, pos.y, 0);
            }
        }

    }

    private void renderHighlight(Rect2i[] p_98139_) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, this.isFocused() ? 255.0F : 100.0F /* changed */, 255.0F);
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for(Rect2i rect2i : p_98139_) {
            int i = rect2i.getX();
            int j = rect2i.getY();
            int k = i + rect2i.getWidth();
            int l = j + rect2i.getHeight();
            bufferbuilder.vertex((double)i, (double)l, 0.0D).endVertex();
            bufferbuilder.vertex((double)k, (double)l, 0.0D).endVertex();
            bufferbuilder.vertex((double)k, (double)j, 0.0D).endVertex();
            bufferbuilder.vertex((double)i, (double)j, 0.0D).endVertex();
        }

        tesselator.end();
        RenderSystem.disableColorLogicOp();

        RenderSystem.setShaderColor(1, 1, 1, 1);
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
            IntList linePositionsOld = new IntArrayList();
            List<LineInfo> linesAll = Lists.newArrayList();
            MutableInt mutableint = new MutableInt();
            MutableBoolean mutableboolean = new MutableBoolean();
            StringSplitter stringsplitter = this.font.getSplitter();
            MutableInt lineNumber = new MutableInt();
            stringsplitter.splitLines(s, this.getWidth() - getLinesXOffset() /* changed */, Style.EMPTY, true, (style, startPos, endPos) -> {
                int k3 = mutableint.getAndIncrement();
                String stringPart = s.substring(startPos, endPos);
                boolean hasNewLine = stringPart.endsWith("\n");
                mutableboolean.setValue(hasNewLine);
                String s3 = StringUtils.stripEnd(stringPart, " \n");
                int l3 = (k3 - this.firstRow) * 9; // Offset firstRow!
                Pos2i bookeditscreen$pos2i1 = this.convertLocalToScreen(new Pos2i(0, l3));
                linePositionsOld.add(startPos);
                linesAll.add(new LineInfo(this.markupLine(style, s3), bookeditscreen$pos2i1.x, bookeditscreen$pos2i1.y, lineNumber.getValue(), cursorPos >= startPos && cursorPos < (hasNewLine ? endPos : s.indexOf("\n", endPos))));
                if (hasNewLine) {
                    lineNumber.increment();
                }
            });

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
            Pos2i cursor;
            if (flag && mutableboolean.isTrue()) {
                cursor = new Pos2i(0, lines.size() * 9);
            } else {
                int cursorLine = findLineFromPos(lineStartsArr, cursorPos);
                int cursorX = this.font.width(s.substring(lineStartsArr[cursorLine], cursorPos));
                cursor = new Pos2i(cursorX, (cursorLine - this.firstRow) * 9); // We have to offset the firstRow here!
            }
            // Hide cursor if out of view
            if (cursor.y < 0 || cursor.y > getHeight()) {
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
            // Hide out-of-view selections and offset them
            List<Rect2i> selectionNew = Lists.newArrayList();
            for (Rect2i rect2i : selection) {
                Rect2i rect2iNew = new Rect2i(rect2i.getX(), rect2i.getY() - this.firstRow * ROW_HEIGHT, rect2i.getWidth(), rect2i.getHeight());
                if (rect2iNew.getY() - this.getY() >= 0 && rect2iNew.getY() - this.getY() < getHeight()) {
                    selectionNew.add(rect2iNew);
                }
            }

            return new DisplayCache(s, cursor, flag, lineStartsArr, lines.toArray(new LineInfo[0]), linesAll.toArray(new LineInfo[0]), selectionNew.toArray(new Rect2i[0]), linesTotal, this.firstRow);
        }
    }

    private List<Pair<Style, String>> markupLine(Style style, String line) {
        if (this.markupProvider != null) {
            return this.markupProvider.markupLine(style, line);
        }
        return Stream.of(Pair.of(style, line)).collect(Collectors.toList());
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

    @OnlyIn(Dist.CLIENT)
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
            int j = i + p_98212_;
            int k;
            if (0 <= j && j < this.lineStarts.length) {
                int l = p_98211_ - this.lineStarts[i];
                int i1 = this.linesAll[j].contents.length();
                k = this.lineStarts[j] + Math.min(l, i1);
            } else {
                k = p_98211_;
            }

            return k;
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

    @OnlyIn(Dist.CLIENT)
    static class LineInfo {
        final List<Pair<Style, String>> contentsStyled;
        final String contents;
        final Component asComponent;
        final int x;
        final int y;
        final int lineNumber;
        final boolean hasCursor;

        public LineInfo(List<Pair<Style, String>> contentsStyled, int x, int y, int lineNumber, boolean hasCursor) {
            this.contentsStyled = contentsStyled;
            this.contents = contentsStyled.stream().map(Pair::getRight).collect(Collectors.joining());
            this.x = x;
            this.y = y;
            this.lineNumber = lineNumber;
            this.hasCursor = hasCursor;

            MutableComponent component = Component.literal("");
            for (Pair<Style, String> value : contentsStyled) {
                component = component.append(Component.literal(value.getRight()).setStyle(value.getLeft()));
            }
            this.asComponent = component;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Pos2i {
        public final int x;
        public final int y;

        Pos2i(int p_98249_, int p_98250_) {
            this.x = p_98249_;
            this.y = p_98250_;
        }
    }

    public static interface IMarkupProvider {
        public List<Pair<Style, String>> markupLine(Style style, String line);
    }
}
