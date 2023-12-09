package org.cyclops.integratedscripting.client.gui.component.input;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.cyclops.cyclopscore.client.gui.component.input.IInputListener;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A widget to edit multi-line text.
 * Inspired by {@link WidgetTextArea}.
 *
 * The using screen must add this as a child and call the following method from its respective method:
 * * {@link #tick()}
 * * {@link #mouseClicked(double, double, int)}
 * * {@link #mouseDragged(double, double, int, double, double)}
 * * {@link #keyPressed(int, int, int)}
 * * {@link #charTyped(char, int)}
 *
 * @author rubensworks
 */
@OnlyIn(Dist.CLIENT)
public class WidgetTextArea extends AbstractWidget implements Widget, GuiEventListener {

    private final TextFieldHelper textFieldHelper;
    private final Font font;

    private int frameTick;
    private String value = "";
    @Nullable
    private IInputListener listener;
    @Nullable
    private WidgetTextArea.DisplayCache displayCache = WidgetTextArea.DisplayCache.EMPTY;
    private long lastClickTime;
    private int lastIndex = -1;

    public WidgetTextArea(Font font, int x, int y, int width, int height, Component narrationMessage) {
        super(x, y, width, height, narrationMessage);
        this.font = font;
        this.textFieldHelper = new TextFieldHelper(this::getValue, this::setValuePassive, this::getClipboard, this::setClipboard, s -> true);
    }

    public void setListener(IInputListener listener) {
        this.listener = listener;
    }

    public void setValue(String value) {
        this.setValuePassive(value);

        textFieldHelper.setCursorToStart();
        textFieldHelper.setSelectionPos(textFieldHelper.getCursorPos());
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

    public void updateNarration(NarrationElementOutput narrationElementOutput) {
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
        if (mouseButton == 1 && mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height) {
            // Select everything
            this.setFocused(true);
            this.setFocused(true);
            textFieldHelper.selectAll();
            return true;
        } else {
            if (mouseButton == 0 && mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double offsetX, double offsetY) {
        if (mouseButton == 0) {
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
        int i = this.textFieldHelper.getCursorPos();
        int j = this.getDisplayCache().changeLine(i, p_98098_);
        this.textFieldHelper.setCursorPos(j, Screen.hasShiftDown());
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
    public void render(PoseStack poseStack, int x, int y, float partialTicks) {
        this.renderBg(poseStack, Minecraft.getInstance(), x, y);

        DisplayCache displayCache = this.getDisplayCache();
        for(LineInfo line : displayCache.lines) {
            this.font.draw(poseStack, line.asComponent, (float)line.x, (float)line.y, -16777216);
        }

        this.renderHighlight(displayCache.selection);
        this.renderCursor(poseStack, displayCache.cursor, displayCache.cursorAtEnd);
    }

    private void renderCursor(PoseStack poseStack, Pos2i pos, boolean cursorAtEnd) {
        if (this.isFocused() && this.frameTick / 6 % 2 == 0) {
            pos = this.convertLocalToScreen(pos);
            if (!cursorAtEnd) {
                GuiComponent.fill(poseStack, pos.x, pos.y - 1, pos.x + 1, pos.y + 9, -16777216);
            } else {
                this.font.draw(poseStack, "_", (float)pos.x, (float)pos.y, 0);
            }
        }

    }

    private void renderHighlight(Rect2i[] p_98139_) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, this.isFocused() ? 255.0F : 100.0F /* changed */, 255.0F);
        RenderSystem.disableTexture();
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
        RenderSystem.enableTexture();

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private Pos2i convertScreenToLocal(Pos2i posScreen) {
        return new Pos2i(posScreen.x - this.x, posScreen.y - this.y);
    }

    private Pos2i convertLocalToScreen(Pos2i posLocal) {
        return new Pos2i(this.x + posLocal.x, this.y + posLocal.y);
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
            return DisplayCache.EMPTY;
        } else {
            int i = this.textFieldHelper.getCursorPos();
            int j = this.textFieldHelper.getSelectionPos();
            IntList intlist = new IntArrayList();
            List<LineInfo> list = Lists.newArrayList();
            MutableInt mutableint = new MutableInt();
            MutableBoolean mutableboolean = new MutableBoolean();
            StringSplitter stringsplitter = this.font.getSplitter();
            stringsplitter.splitLines(s, this.getWidth() /* changed: constant width */, Style.EMPTY, true, (p_98132_, p_98133_, p_98134_) -> {
                int k3 = mutableint.getAndIncrement();
                String s2 = s.substring(p_98133_, p_98134_);
                mutableboolean.setValue(s2.endsWith("\n"));
                String s3 = StringUtils.stripEnd(s2, " \n");
                int l3 = k3 * 9;
                Pos2i bookeditscreen$pos2i1 = this.convertLocalToScreen(new Pos2i(0, l3));
                intlist.add(p_98133_);
                list.add(new LineInfo(p_98132_, s3, bookeditscreen$pos2i1.x, bookeditscreen$pos2i1.y));
            });
            int[] aint = intlist.toIntArray();
            boolean flag = i == s.length();
            Pos2i bookeditscreen$pos2i;
            if (flag && mutableboolean.isTrue()) {
                bookeditscreen$pos2i = new Pos2i(0, list.size() * 9);
            } else {
                int k = findLineFromPos(aint, i);
                int l = this.font.width(s.substring(aint[k], i));
                bookeditscreen$pos2i = new Pos2i(l, k * 9);
            }

            List<Rect2i> list1 = Lists.newArrayList();
            if (i != j) {
                int l2 = Math.min(i, j);
                int i1 = Math.max(i, j);
                int j1 = findLineFromPos(aint, l2);
                int k1 = findLineFromPos(aint, i1);
                if (j1 == k1) {
                    int l1 = j1 * 9;
                    int i2 = aint[j1];
                    list1.add(this.createPartialLineSelection(s, stringsplitter, l2, i1, l1, i2));
                } else {
                    int i3 = j1 + 1 > aint.length ? s.length() : aint[j1 + 1];
                    list1.add(this.createPartialLineSelection(s, stringsplitter, l2, i3, j1 * 9, aint[j1]));

                    for(int j3 = j1 + 1; j3 < k1; ++j3) {
                        int j2 = j3 * 9;
                        String s1 = s.substring(aint[j3], aint[j3 + 1]);
                        int k2 = (int)stringsplitter.stringWidth(s1);
                        list1.add(this.createSelection(new Pos2i(0, j2), new Pos2i(k2, j2 + 9)));
                    }

                    list1.add(this.createPartialLineSelection(s, stringsplitter, aint[k1], i1, k1 * 9, aint[k1]));
                }
            }

            return new DisplayCache(s, bookeditscreen$pos2i, flag, aint, list.toArray(new LineInfo[0]), list1.toArray(new Rect2i[0]));
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

    @OnlyIn(Dist.CLIENT)
    static class DisplayCache {
        static final DisplayCache EMPTY = new DisplayCache("", new Pos2i(0, 0), true, new int[]{0}, new LineInfo[]{new LineInfo(Style.EMPTY, "", 0, 0)}, new Rect2i[0]);
        private final String fullText;
        final Pos2i cursor;
        final boolean cursorAtEnd;
        private final int[] lineStarts;
        final LineInfo[] lines;
        final Rect2i[] selection;

        public DisplayCache(String p_98201_, Pos2i p_98202_, boolean p_98203_, int[] p_98204_, LineInfo[] p_98205_, Rect2i[] p_98206_) {
            this.fullText = p_98201_;
            this.cursor = p_98202_;
            this.cursorAtEnd = p_98203_;
            this.lineStarts = p_98204_;
            this.lines = p_98205_;
            this.selection = p_98206_;
        }

        public int getIndexAtPosition(Font p_98214_, Pos2i p_98215_) {
            int i = p_98215_.y / 9;
            if (i < 0) {
                return 0;
            } else if (i >= this.lines.length) {
                return this.fullText.length();
            } else {
                LineInfo bookeditscreen$lineinfo = this.lines[i];
                return this.lineStarts[i] + p_98214_.getSplitter().plainIndexAtWidth(bookeditscreen$lineinfo.contents, p_98215_.x, bookeditscreen$lineinfo.style);
            }
        }

        public int changeLine(int p_98211_, int p_98212_) {
            int i = WidgetTextArea.findLineFromPos(this.lineStarts, p_98211_);
            int j = i + p_98212_;
            int k;
            if (0 <= j && j < this.lineStarts.length) {
                int l = p_98211_ - this.lineStarts[i];
                int i1 = this.lines[j].contents.length();
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
            return this.lineStarts[i] + this.lines[i].contents.length();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class LineInfo {
        final Style style;
        final String contents;
        final Component asComponent;
        final int x;
        final int y;

        public LineInfo(Style p_98232_, String p_98233_, int p_98234_, int p_98235_) {
            this.style = p_98232_;
            this.contents = p_98233_;
            this.x = p_98234_;
            this.y = p_98235_;
            this.asComponent = Component.literal(p_98233_).setStyle(p_98232_);
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
}
