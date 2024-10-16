package org.cyclops.integratedscripting.client.gui.container;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetArrowedListField;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integrateddynamics.core.client.gui.container.DisplayErrorsComponent;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetDialog;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetTextArea;
import org.cyclops.integratedscripting.client.gui.image.ScriptImages;
import org.cyclops.integratedscripting.core.language.LanguageHandlers;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingDeleteScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingModifiedScriptPacket;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerScreenTerminalScripting extends ContainerScreenExtended<ContainerTerminalScripting> {

    public static int PATHS_X = 19;
    public static int PATHS_Y = 18;
    public static int PATHS_WIDTH = 56;
    public static int PATHS_HEIGHT = 214;
    public static int PATHS_ROW_HEIGHT = 8;
    public static int PATHS_MAX_ROWS = PATHS_HEIGHT / PATHS_ROW_HEIGHT;
    public static int SCRIPT_X = 80;
    public static int SCRIPT_X_INNER = 94;
    public static int SCRIPT_Y = 18;
    public static int SCRIPT_WIDTH = 168;
    public static int SCRIPT_HEIGHT = 115;

    private final Player player;

    private WidgetArrowedListField<Integer> fieldDisk;
    private WidgetScrollBar scrollBar;
    private int firstRow;
    private WidgetTextArea textArea;
    private ButtonText buttonCreateFile;
    private WidgetDialog pendingScriptRemovalDialog;
    private int lastClientSyncTick;
    private final DisplayErrorsComponent displayErrors = new DisplayErrorsComponent();

    private final Map<Path, Integer> lastScriptCursorPos = Maps.newHashMap();
    private final Map<Path, Integer> lastScriptSelectionPos = Maps.newHashMap();
    private final Int2ObjectMap<Path> lastDiskScript = new Int2ObjectAVLTreeMap<>();

    public ContainerScreenTerminalScripting(ContainerTerminalScripting container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.player = inventory.player;

        this.imageHeight = 240;
        this.titleLabelX = 88;
        this.titleLabelY = 6;
        this.inventoryLabelX = 88;
        this.inventoryLabelY = this.imageHeight - 94;
        this.firstRow = 0;
        this.lastClientSyncTick = 0;
    }

    @Override
    public void init() {
        super.init();

        fieldDisk = new WidgetArrowedListField<>(Minecraft.getInstance().font, leftPos + 30,
                topPos + 4, 42, 15, true,
                Component.translatable("gui.integratedscripting.disk"), true,
                getMenu().getAvailableDisks());
        fieldDisk.setMaxLength(5);
        fieldDisk.setVisible(true);
        fieldDisk.setTextColor(16777215);
        fieldDisk.setCanLoseFocus(true);
        fieldDisk.setEditable(true);
        fieldDisk.setValue(String.valueOf(getMenu().getActiveDisk()));

        scrollBar = new WidgetScrollBar(leftPos + 5, topPos + 18, 162,
                Component.translatable("gui.cyclopscore.scrollbar"), this::setFirstRow, PATHS_MAX_ROWS) {
            @Override
            public int getTotalRows() {
                Map<Path, String> scripts = getActiveScripts();
                return scripts == null ? 0 : scripts.keySet().size();
            }
        };

        textArea = new WidgetTextArea(Minecraft.getInstance().font, this.leftPos + SCRIPT_X + 1, this.topPos + SCRIPT_Y + 1, SCRIPT_WIDTH, SCRIPT_HEIGHT, Component.translatable("gui.integratedscripting.script"), true, true);
        textArea.setListener(this::onActiveScriptModified);
        textArea.setListenerSelection(this::onSelectionModified);
        textArea.setListenerCursor(this::onSelectionModified);
        textArea.setMarkupProvider((style, line) -> {
            Path path = getMenu().getActiveScriptPath();
            ILanguageHandler languageHandler = path != null ? LanguageHandlers.REGISTRY.getProvider(path) : null;
            return languageHandler != null ? languageHandler.markupLine(line) : Stream.of(Pair.of(style, line)).collect(Collectors.toList());
        });
        addRenderableWidget(textArea);

        buttonCreateFile = new ButtonText(this.leftPos + 19, this.topPos + 222, 56, 10, Component.translatable("gui.integratedscripting.create_file"), Component.literal("+"),
                (button) -> getMenu().createNewFile(), true);
        addRenderableWidget(buttonCreateFile);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        textArea.tick();

        if (this.lastClientSyncTick == 0) {
            // Send modified scripts from client to server
            syncDirtyScripts();
        }
        this.lastClientSyncTick = (this.lastClientSyncTick + 1) % GeneralConfig.terminalScriptingClientSyncTickInterval;
    }

    protected void syncDirtyScripts() {
        for (Pair<Integer, Path> entry : getMenu().getClientScriptsDirty()) {
            Map<Path, String> diskScripts = getMenu().getLastScripts().get(entry.getLeft());
            if (diskScripts != null) {
                String script = diskScripts.get(entry.getRight());
                if (script != null) {
                    IntegratedScripting._instance.getPacketHandler()
                            .sendToServer(new TerminalScriptingModifiedScriptPacket(entry.getLeft(), entry.getRight(), script));
                }
            }
        }
        getMenu().getClientScriptsDirty().clear();
    }

    @Override
    public void onClose() {
        this.syncDirtyScripts();
        super.onClose();
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scripting_terminal.png");
    }

    @Override
    public int getBaseXSize() {
        return 256;
    }

    @Override
    public int getBaseYSize() {
        return 240;
    }

    protected int getErrorX() {
        return 212;
    }

    protected int getErrorY() {
        return 139;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        RenderHelpers.bindTexture(this.texture);
        fieldDisk.render(guiGraphics, mouseX, mouseY, partialTicks);
        scrollBar.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (!this.getMenu().getAvailableDisks().isEmpty()) {
            this.renderScriptPaths(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            // Gray-out editor and file list
            RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 0.3F);
            guiGraphics.fill(leftPos + PATHS_X, topPos + PATHS_Y, leftPos + PATHS_X + PATHS_WIDTH, topPos + PATHS_Y + PATHS_HEIGHT, Helpers.RGBAToInt(50, 50, 50, 100));
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        if (this.getMenu().getActiveScript() == null) {
            // Gray-out editor and file list
            RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 0.3F);
            guiGraphics.fill(leftPos + SCRIPT_X_INNER, topPos + SCRIPT_Y, leftPos + SCRIPT_X_INNER + SCRIPT_WIDTH, topPos + SCRIPT_Y + SCRIPT_HEIGHT, Helpers.RGBAToInt(50, 50, 50, 100));
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        displayErrors.drawBackground(guiGraphics, getMenu().getReadErrors(), getErrorX(), getErrorY(), getErrorX(), getErrorY(), this,
                this.leftPos, this.topPos, getMenu().canWriteScriptToVariable());
    }

    @Nullable
    protected Map<Path, String> getActiveScripts() {
        return this.container.getLastScripts().get(this.container.getActiveDisk());
    }

    protected List<Path> getVisibleScriptPaths() {
        Map<Path, String> scripts = getActiveScripts();
        if (scripts != null) {
            List<Path> paths = scripts.keySet().stream().sorted().collect(Collectors.toList());
            if (!paths.isEmpty()) {
                return paths.subList(
                        Math.max(0, this.firstRow),
                        Math.max(0, this.firstRow) + Math.min(paths.size(), scrollBar.getVisibleRows())
                );
            }
        }
        return Collections.emptyList();
    }

    protected void renderScriptPaths(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        List<Path> paths = getVisibleScriptPaths();
        int i = 0;
        for (Path path : paths) {
            boolean hovering = isHovering(PATHS_X, PATHS_Y + i * PATHS_ROW_HEIGHT, PATHS_WIDTH, PATHS_ROW_HEIGHT, mouseX, mouseY);
            boolean active = path.equals(getMenu().getActiveScriptPath());

            // Draw highlighted background behind script path text
            if (active) {
                guiGraphics.fill(this.leftPos + PATHS_X, this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT, this.leftPos + PATHS_X + PATHS_WIDTH, this.topPos + PATHS_Y + (i + 1) * PATHS_ROW_HEIGHT, Helpers.RGBAToInt(110, 130, 240, 255));
            }

            // Draw file type icon
            ILanguageHandler languageHandler = LanguageHandlers.REGISTRY.getProvider(path);
            IImage icon = ScriptImages.FILE_OTHER;
            if (languageHandler != null) {
                icon = languageHandler.getIcon();
            }
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(this.leftPos + PATHS_X + 1, this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT + 1, 0);
            guiGraphics.pose().scale(0.5F, 0.5F, 0.5F);
            icon.draw(guiGraphics, 0, 0);
            guiGraphics.pose().popPose();

            // Draw filename
            RenderHelpers.drawScaledString(
                    guiGraphics,
                    font,
                    StringUtil.truncateStringIfNecessary(path.toString(), 20, true),
                    this.leftPos + PATHS_X + 1 + 7,
                    this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT + 1 + 1,
                    0.5f,
                    hovering && !active ? Helpers.RGBToInt(50, 50, 250) : Helpers.RGBToInt(0, 0, 0),
                    false,
                    Font.DisplayMode.NORMAL
            );

            // If hovering, render removal button
            if (hovering) {
                guiGraphics.pose().pushPose();
                float scale = 0.4F;
                int size = (int) (Images.ERROR.getWidth() * scale);
                guiGraphics.pose().translate(this.leftPos + PATHS_X + PATHS_WIDTH - size - 1, this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT + 1, 0);
                guiGraphics.pose().scale(scale, scale, 4F);
                if (isHovering(PATHS_X + PATHS_WIDTH - size - 1, PATHS_Y + i * PATHS_ROW_HEIGHT, PATHS_X + PATHS_WIDTH - size + size - 1, PATHS_Y + i * PATHS_ROW_HEIGHT + size, mouseX, mouseY)) {
                    Images.ERROR.draw(guiGraphics, 0, 0);
                } else {
                    Images.ERROR.drawWithColor(guiGraphics, 0, 0, 0.7F, 0.7F, 0.7F, 1F);
                }
                guiGraphics.pose().popPose();
            }

            i++;
        }
    }

    @Nullable
    protected Path getHoveredScriptPath(double mouseX, double mouseY) {
        List<Path> paths = getVisibleScriptPaths();
        int i = 0;
        for (Path path : paths) {
            if (isHovering(PATHS_X, PATHS_Y + i * PATHS_ROW_HEIGHT, PATHS_WIDTH, PATHS_ROW_HEIGHT, mouseX, mouseY)) {
                return path;
            }
            i++;
        }
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Draw disk label
        guiGraphics.drawString(font, L10NHelpers.localize("gui.integratedscripting.disk") + ":", 8, 6, 16777215);

        displayErrors.drawForeground(guiGraphics.pose(), getMenu().getReadErrors(), getErrorX(), getErrorY(), mouseX, mouseY, this, this.leftPos, this.topPos);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // Make active dialog consume all input
        if (pendingScriptRemovalDialog != null) {
            return pendingScriptRemovalDialog.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // Handle script path clicks
        Path hoveredScriptPath = getHoveredScriptPath(mouseX, mouseY);
        if (hoveredScriptPath != null) {
            // Handle script removal clicks
            if (mouseX >= this.leftPos + PATHS_X + PATHS_WIDTH - (int) (Images.ERROR.getWidth() * 0.4F) - 1) {
                this.fieldDisk.playDownSound(Minecraft.getInstance().getSoundManager());

                // Show confirmation dialog
                pendingScriptRemovalDialog = new WidgetDialog(font, leftPos + getBaseXSize() / 2 - WidgetDialog.WIDTH / 2, topPos + 50, this,
                        Component.translatable("gui.integratedscripting.removal_dialog.title"),
                        Component.translatable("gui.integratedscripting.removal_dialog.message", hoveredScriptPath.toString()),
                        Component.translatable("gui.integratedscripting.removal_dialog.delete"),
                        Component.translatable("gui.integratedscripting.removal_dialog.keep"),
                        (b) -> {
                            removeWidget(pendingScriptRemovalDialog);
                            pendingScriptRemovalDialog = null;
                            this.removeScript(hoveredScriptPath);
                        },
                        (b) -> {
                            removeWidget(pendingScriptRemovalDialog);
                            pendingScriptRemovalDialog = null;
                        });
                addRenderableWidget(pendingScriptRemovalDialog);

                return true;
            } else {
                // Save script state
                this.saveCursorPos();

                // Select another script
                getMenu().setActiveScriptPath(hoveredScriptPath);
                this.onActiveScriptSelected();

                // Restore script state
                this.restoreCursorPos();

                this.fieldDisk.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        // Update disk when changing disk field
        int previousDisk = getActiveDisk();
        if (this.fieldDisk.mouseClicked(mouseX, mouseY, mouseButton)) {
            // Save script state
            this.saveCursorPos();
            this.saveDiskScript(previousDisk);

            // Set new disk
            getMenu().setActiveDisk(getActiveDisk());
            scrollBar.scrollTo(0); // Reset scrollbar

            // Restore script state
            this.restoreDiskScript();
            this.restoreCursorPos();

            playButtonClickSound();

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public int getActiveDisk() {
        try {
            Integer element = this.fieldDisk.getActiveElement();
            return element == null ? -1 : element;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void saveCursorPos() {
        Path currentScriptPath = getMenu().getActiveScriptPath();
        if (currentScriptPath != null) {
            lastScriptCursorPos.put(currentScriptPath, textArea.getCursorPos());
            lastScriptSelectionPos.put(currentScriptPath, textArea.getSelectionPos());
        }
    }

    private void restoreCursorPos() {
        Path currentScriptPath = getMenu().getActiveScriptPath();

        Integer scriptCursorPos = lastScriptCursorPos.get(currentScriptPath);
        if (scriptCursorPos != null) {
            textArea.setCursorPos(scriptCursorPos);
        } else {
            textArea.setCursorPos(0);
            if (textArea.isFocused()) {
                textArea.setFocused(false);
            }
        }

        Integer scriptSelectionPos = lastScriptSelectionPos.get(currentScriptPath);
        if (scriptSelectionPos != null) {
            textArea.setSelectionPos(scriptSelectionPos);
        } else {
            textArea.setSelectionPos(0);
        }
    }

    private void saveDiskScript(int disk) {
        Path currentScriptPath = getMenu().getActiveScriptPath();
        if (disk >= 0 && currentScriptPath != null) {
            lastDiskScript.put(disk, currentScriptPath);
        }
    }

    private void restoreDiskScript() {
        int disk = getActiveDisk();
        if (disk >= 0) {
            Path scriptPath = lastDiskScript.get(disk); // This may be null, but that's fine, as that will reset the selected active path
            getMenu().setActiveScriptPath(scriptPath);
            this.onActiveScriptSelected();
        }
    }

    protected void playButtonClickSound() {
        this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void setFirstRow(int firstRow) {
        this.firstRow = firstRow;
    }

    private void onActiveScriptSelected() {
        String script = getMenu().getActiveScript();
        this.textArea.setValue(script == null ? "" : script);
    }

    private void onActiveScriptModified() {
        getMenu().setActiveScript(this.textArea.getValue());
    }

    private void onSelectionModified() {
        String selected = this.textArea.getSelected();
        if (Strings.isNullOrEmpty(selected)) {
            // If nothing was selected, derive the member around the current cursor position
            int cursorPos = this.textArea.getCursorPos();
            if (cursorPos >= 0) {
                String value = this.textArea.getValue();
                Matcher matcherToEnd = ContainerTerminalScripting.INVALID_MEMBER_NAME.matcher(value);
                int endPos = value.length();
                if (matcherToEnd.find(cursorPos)) {
                    endPos = matcherToEnd.end() - 1;
                }
                Matcher matcherToStart = ContainerTerminalScripting.INVALID_MEMBER_NAME.matcher(new StringBuilder(value).reverse().toString());
                int startPos = 0;
                if (matcherToStart.find(value.length() - cursorPos)) {
                    startPos = value.length() - matcherToStart.end() + 1;
                }
                selected = value.substring(startPos, endPos);
            }
        }

        getMenu().setSelection(selected);
    }

    private void removeScript(Path path) {
        IntegratedScripting._instance.getPacketHandler()
                .sendToServer(new TerminalScriptingDeleteScriptPacket(getMenu().getActiveDisk(), path));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double mouseZ, double scroll) {
        if (textArea.mouseScrolled(mouseX, mouseY, mouseZ, scroll)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, mouseZ, scroll);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double offsetX, double offsetY) {
        // Make active dialog consume all input
        if (pendingScriptRemovalDialog != null) {
            return false;
        }

        if (textArea.mouseDragged(mouseX, mouseY, mouseButton, offsetX, offsetY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {
        // Make active dialog consume all input
        if (typedChar != GLFW.GLFW_KEY_ESCAPE && pendingScriptRemovalDialog != null) {
            return false;
        }

        if (textArea.isFocused()) {
            boolean ret = textArea.keyPressed(typedChar, keyCode, modifiers);
            if (typedChar != GLFW.GLFW_KEY_ESCAPE) {
                return ret;
            }
        }
        return super.keyPressed(typedChar, keyCode, modifiers);
    }

    @Override
    public boolean charTyped(char p_94683_, int p_94684_) {
        // Make active dialog consume all input
        if (pendingScriptRemovalDialog != null) {
            return false;
        }
        return super.charTyped(p_94683_, p_94684_);
    }
}
