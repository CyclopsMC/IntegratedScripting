package org.cyclops.integratedscripting.client.gui.container;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
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
import org.cyclops.integratedscripting.client.gui.component.button.ButtonIcon;
import org.cyclops.integratedscripting.client.gui.component.input.TextEditing;
import org.cyclops.integratedscripting.client.gui.component.input.TextHistory;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetDialog;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetSearchBar;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetTextArea;
import org.cyclops.integratedscripting.client.gui.image.ScriptImages;
import org.cyclops.integratedscripting.client.key.Keys;
import org.cyclops.integratedscripting.core.language.LanguageHandlers;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingDeleteScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingModifiedScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingRenameScriptPacket;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.file.InvalidPathException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public static int DISK_X = 30;
    public static int SCRIPT_X = 80;
    public static int SCRIPT_X_INNER = 94;
    public static int SCRIPT_Y = 18;
    public static int SCRIPT_WIDTH = 168;
    public static int SCRIPT_HEIGHT = 115;
    public static int FOCUS_MARGIN = 20;

    private final Player player;

    private WidgetArrowedListField<Integer> fieldDisk;
    private WidgetScrollBar scrollBar;
    private int firstRow;
    private WidgetTextArea textArea;
    private ButtonText buttonCreateFile;
    @Nullable
    private ButtonIcon buttonOpenFolder;
    private int buttonOpenFolderDisk = Integer.MIN_VALUE;
    // The disk a script is being created on, so the new script can be opened once the server sends it
    private int createdScriptDisk = -1;
    private final List<ButtonIcon> toolbarEditButtons = Lists.newArrayList();
    private ButtonIcon buttonUndo;
    private ButtonIcon buttonRedo;
    private int toolbarLeft;
    private ButtonIcon buttonFocus;
    private boolean focusMode;
    private WidgetDialog pendingDialog;
    @Nullable
    private WidgetSearchBar searchBar;
    private int lastClientSyncTick;
    private final DisplayErrorsComponent displayErrors = new DisplayErrorsComponent();

    private final Map<Path, Integer> lastScriptCursorPos = Maps.newHashMap();
    private final Map<Path, Integer> lastScriptSelectionPos = Maps.newHashMap();
    private final Int2ObjectMap<Path> lastDiskScript = new Int2ObjectAVLTreeMap<>();
    private final Map<Pair<Integer, Path>, TextHistory> scriptHistories = Maps.newHashMap();

    public ContainerScreenTerminalScripting(ContainerTerminalScripting container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.player = inventory.player;

        this.imageHeight = 240;
        this.titleLabelX = 88;
        this.titleLabelY = 6;
        this.firstRow = 0;
        this.lastClientSyncTick = 0;
    }

    @Override
    public void init() {
        // This is called again when the screen is resized, which recreates all widgets
        if (textArea != null) {
            this.saveCursorPos();
        }
        pendingDialog = null;
        searchBar = null;

        super.init();

        fieldDisk = new WidgetArrowedListField<>(Minecraft.getInstance().font, leftPos + DISK_X,
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

        if (focusMode) {
            // Let the editor take up most of the screen
            int height = (this.height - 2 * FOCUS_MARGIN) / WidgetTextArea.ROW_HEIGHT * WidgetTextArea.ROW_HEIGHT;
            textArea = new WidgetTextArea(Minecraft.getInstance().font, FOCUS_MARGIN, FOCUS_MARGIN, this.width - 2 * FOCUS_MARGIN, height, Component.translatable("gui.integratedscripting.script"), true, true);
        } else {
            textArea = new WidgetTextArea(Minecraft.getInstance().font, this.leftPos + SCRIPT_X + 1, this.topPos + SCRIPT_Y + 1, SCRIPT_WIDTH, SCRIPT_HEIGHT, Component.translatable("gui.integratedscripting.script"), true, true);
        }
        textArea.setIndent(" ".repeat(GeneralConfig.terminalScriptingIndentSize));
        textArea.setListener(this::onActiveScriptModified);
        textArea.setShortcutListener((key, scanCode, modifiers) -> {
            boolean find = Keys.matches(Keys.FIND, key, scanCode, modifiers);
            if (find || Keys.matches(Keys.GOTO_LINE, key, scanCode, modifiers)) {
                openNavigate(!find);
                return true;
            }
            if (Keys.matches(Keys.FOCUS_MODE, key, scanCode, modifiers)) {
                this.toggleFocusMode();
                // Keep typing in the recreated editor
                this.setFocused(this.textArea);
                this.textArea.setFocused(true);
                return true;
            }
            return false;
        });
        getMenu().setRemoteScriptChangeListener(this::onRemoteScriptChanged);
        getMenu().setSyntaxErrorListener((disk, path) -> {
            if (disk == getMenu().getActiveDisk() && path.equals(getMenu().getActiveScriptPath())) {
                this.textArea.setSyntaxError(getMenu().getSyntaxError(disk, path));
            }
        });
        textArea.setListenerSelection(this::onSelectionModified);
        textArea.setListenerCursor(this::onSelectionModified);
        textArea.setMarkupProvider(text -> {
            Path path = getMenu().getActiveScriptPath();
            ILanguageHandler languageHandler = path != null ? LanguageHandlers.REGISTRY.getProvider(path) : null;
            return languageHandler != null ? languageHandler.markupScript(text) : Stream.of(Pair.of(Style.EMPTY, text)).collect(Collectors.toList());
        });
        addRenderableWidget(textArea);

        // The scripts folder can only be opened if this client is running the server, such as in singleplayer
        boolean canOpenFolder = Minecraft.getInstance().hasSingleplayerServer();
        buttonCreateFile = new ButtonText(this.leftPos + PATHS_X, this.topPos + 222, canOpenFolder ? PATHS_WIDTH - 12 : PATHS_WIDTH, 10, Component.translatable("gui.integratedscripting.create_file"), Component.literal("+"),
                (button) -> this.createNewFile(), true);
        buttonOpenFolder = null;
        buttonOpenFolderDisk = Integer.MIN_VALUE;
        if (!focusMode) {
            addRenderableWidget(buttonCreateFile);
            if (canOpenFolder) {
                buttonOpenFolder = new ButtonIcon(this.leftPos + PATHS_X + PATHS_WIDTH - 10, this.topPos + 222, 10, 10,
                        Component.translatable("gui.integratedscripting.open_folder"), (button) -> {
                            this.openDiskFolder();
                            // Run after the click is handled, as that focuses the button
                            Minecraft.getInstance().tell(() -> this.setFocused(null));
                        }, ScriptImages.ACTION_OPEN_FOLDER);
                addRenderableWidget(buttonOpenFolder);
                this.updateOpenFolderButton();
            }
        }

        if (focusMode) {
            this.addToolbar(this.width - FOCUS_MARGIN, FOCUS_MARGIN - 12);
        } else {
            this.addToolbar(this.leftPos + SCRIPT_X + SCRIPT_WIDTH, this.topPos + 5);
        }

        // Restore the open script in the new editor
        if (getMenu().getActiveScriptPath() != null) {
            this.onActiveScriptSelected();
            this.restoreCursorPos();
            textArea.ensureCursorVisible();
        }
    }

    /**
     * Add buttons for editor actions, which also show their keyboard shortcut in their tooltip.
     * @param right The right side of the toolbar.
     * @param y The top of the toolbar.
     */
    private void addToolbar(int right, int y) {
        Component focusLabel = Component.translatable(focusMode ? "gui.integratedscripting.focus_mode.exit" : "gui.integratedscripting.focus_mode.enter");
        buttonFocus = new ButtonIcon(right - 10, y, 10, 10, focusLabel, (button) -> this.toggleFocusMode(),
                focusMode ? ScriptImages.ACTION_FOCUS_EXIT : ScriptImages.ACTION_FOCUS_ENTER);
        // Escape also leaves the enlarged editor
        MutableComponent focusKeys = focusMode ? Component.translatable("key.keyboard.escape") : null;
        if (!Keys.FOCUS_MODE.isUnbound()) {
            focusKeys = focusKeys == null ? Keys.FOCUS_MODE.getTranslatedKeyMessage().copy()
                    : focusKeys.append(", ").append(Keys.FOCUS_MODE.getTranslatedKeyMessage());
        }
        MutableComponent focusTooltip = focusLabel.copy();
        if (focusKeys != null) {
            focusTooltip.append(Component.literal(" (").append(focusKeys).append(")").withStyle(ChatFormatting.GRAY));
        }
        buttonFocus.setTooltip(Tooltip.create(focusTooltip));
        addRenderableWidget(buttonFocus);
        right -= 10;

        this.toolbarLeft = right - 50;
        this.toolbarEditButtons.clear();
        this.buttonUndo = this.addToolbarButton(right - 50, y, ScriptImages.ACTION_UNDO, Keys.UNDO, () -> this.textArea.undo());
        this.buttonRedo = this.addToolbarButton(right - 40, y, ScriptImages.ACTION_REDO, Keys.REDO, () -> this.textArea.redo());
        this.addToolbarButton(right - 30, y, ScriptImages.ACTION_FIND, Keys.FIND, () -> this.openNavigate(false));
        this.addToolbarButton(right - 20, y, ScriptImages.ACTION_GOTO_LINE, Keys.GOTO_LINE, () -> this.openNavigate(true));
        this.addToolbarButton(right - 10, y, ScriptImages.ACTION_COMMENT, Keys.COMMENT, () -> this.textArea.toggleComment());
        this.updateToolbar();
    }

    private ButtonIcon addToolbarButton(int x, int y, IImage image, KeyMapping keyMapping, Runnable action) {
        Component tooltip = Keys.getTooltip(keyMapping);
        ButtonIcon button = new ButtonIcon(x, y, 10, 10, tooltip, (b) -> {
            // Run after the click is handled, as that focuses the button
            Minecraft.getInstance().tell(() -> {
                // Give focus back to the editor, unless the action focuses something else
                this.setFocused(this.textArea);
                this.textArea.setFocused(true);
                action.run();
            });
        }, image);
        button.setTooltip(Tooltip.create(tooltip));
        this.toolbarEditButtons.add(button);
        addRenderableWidget(button);
        return button;
    }

    protected Path getDiskFolder(int disk) {
        return IntegratedScripting._instance.scriptingData.getDiskPath(disk).toAbsolutePath().normalize();
    }

    private void updateOpenFolderButton() {
        int disk = getMenu().getActiveDisk();
        if (this.buttonOpenFolder != null && disk != this.buttonOpenFolderDisk) {
            this.buttonOpenFolderDisk = disk;
            this.buttonOpenFolder.active = disk >= 0;
            Component tooltip = Component.translatable("gui.integratedscripting.open_folder");
            if (disk >= 0) {
                tooltip = tooltip.copy().append("\n").append(Component.literal(this.getDiskFolder(disk).toString()).withStyle(ChatFormatting.GRAY));
            }
            this.buttonOpenFolder.setTooltip(Tooltip.create(tooltip));
        }
    }

    private void openDiskFolder() {
        int disk = getMenu().getActiveDisk();
        if (disk >= 0) {
            Path path = this.getDiskFolder(disk);
            try {
                // The folder of a disk without scripts may not exist yet
                Files.createDirectories(path);
            } catch (IOException e) {
                IntegratedScripting.clog(Level.ERROR, "Could not create the scripts folder " + path + ": " + e.getMessage());
            }
            Util.getPlatform().openPath(path);
        }
    }

    private void updateToolbar() {
        boolean scriptOpen = getMenu().getActiveScript() != null;
        for (ButtonIcon button : this.toolbarEditButtons) {
            button.active = scriptOpen;
        }
        this.buttonUndo.active = scriptOpen && this.textArea.canUndo();
        this.buttonRedo.active = scriptOpen && this.textArea.canRedo();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        textArea.tick();
        this.updateOpenFolderButton();
        this.updateToolbar();
        // The matches change when the script is edited while searching
        this.updateSearchStatus();

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

    private void toggleFocusMode() {
        this.focusMode = !this.focusMode;
        // Recreates the widgets, which keeps the open script
        this.rebuildWidgets();
    }

    @Override
    protected void drawCurrentScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (focusMode) {
            // Only show the editor, without the container contents
            guiGraphics.fill(textArea.getX() - 2, textArea.getY() - 2, textArea.getX() + textArea.getWidth() + 2, textArea.getY() + textArea.getHeight() + 2, Helpers.RGBAToInt(85, 85, 85, 255));
            guiGraphics.fill(textArea.getX() - 1, textArea.getY() - 1, textArea.getX() + textArea.getWidth() + 1, textArea.getY() + textArea.getHeight() + 1, Helpers.RGBAToInt(224, 224, 224, 255));
            Path path = getMenu().getActiveScriptPath();
            if (path != null) {
                guiGraphics.drawString(font, path.toString(), FOCUS_MARGIN, FOCUS_MARGIN - 11, 16777215);
            }
            for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
        } else {
            super.drawCurrentScreen(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        // Slots are hidden in focus mode
        if (!focusMode) {
            super.renderTooltip(guiGraphics, x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Only the editor is shown in focus mode
        if (focusMode) {
            return;
        }
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

            // If hovering, show the full path, as it may be truncated, and render removal button
            if (hovering) {
                // Don't draw over an open dialog
                if (pendingDialog == null) {
                    this.setTooltipForNextRenderPass(Lists.newArrayList(
                            Component.literal(path.toString()).getVisualOrderText(),
                            Component.translatable("gui.integratedscripting.rename_hint").withStyle(ChatFormatting.GRAY).getVisualOrderText()));
                }
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
        // Shorten the title if it would overlap with the toolbar, as translations can be long
        Component title = this.title;
        int maxTitleWidth = this.toolbarLeft - this.leftPos - this.titleLabelX - 1;
        if (this.font.width(title) > maxTitleWidth) {
            title = Component.literal(this.font.plainSubstrByWidth(title.getString(), maxTitleWidth - this.font.width("\u2026")) + "\u2026");
        }
        // The inventory label is left out, so its row can show the card binding
        guiGraphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, 4210752, false);

        // Draw disk label, scaled to the space before the disk field, which fits the English label at normal size
        String diskLabel = L10NHelpers.localize("gui.integratedscripting.disk") + ":";
        float diskLabelScale = (float) (DISK_X - 8 - 1) / font.width(diskLabel);
        int diskLabelY = Math.round(6 + font.lineHeight * (1 - diskLabelScale) / 2);
        RenderHelpers.drawScaledString(guiGraphics, font, diskLabel, 8, diskLabelY, diskLabelScale, 16777215, true, Font.DisplayMode.NORMAL);

        // Draw the member the card is bound to, or will be bound to, right-aligned next to the card slot
        Component bindingLabel = getBindingLabel();
        if (bindingLabel != null) {
            int right = getErrorX() - 2;
            guiGraphics.drawString(font, bindingLabel, right - font.width(bindingLabel), getErrorY() + 2, 4210752, false);
        }

        displayErrors.drawForeground(guiGraphics.pose(), getMenu().getReadErrors(), getErrorX(), getErrorY(), mouseX, mouseY, this, this.leftPos, this.topPos);
    }

    @Nullable
    protected Component getBindingLabel() {
        if (getMenu().getActiveScriptPath() == null) {
            return null;
        }
        String cardMember = getMenu().getCardMember();
        if (cardMember != null) {
            return Component.translatable("gui.integratedscripting.binding.card", formatBindingMember(cardMember, "gui.integratedscripting.binding.card"));
        }
        if (getMenu().isMemberSelected()) {
            // Not bound yet, so shown less prominently
            return Component.translatable("gui.integratedscripting.binding.next", formatBindingMember(getMenu().getSelectedMember(), "gui.integratedscripting.binding.next"))
                    .withStyle(style -> style.withItalic(true).withColor(0x707070));
        }
        return null;
    }

    protected Component formatBindingMember(String member, String translationKey) {
        // Truncate the member so the label fits between the editor's left border and the status icon
        int maxWidth = getErrorX() - 2 - (SCRIPT_X_INNER - 6) - font.width(L10NHelpers.localize(translationKey, ""));
        if (font.width(member) > maxWidth) {
            member = font.plainSubstrByWidth(member, maxWidth - font.width("\u2026")) + "\u2026";
        }
        return Component.literal(member).withStyle(style -> style.withColor(0x1D4F91));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // Make active dialog consume all input
        if (pendingDialog != null) {
            return pendingDialog.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // Only the editor widgets are visible in focus mode, so don't let clicks reach the container
        if (focusMode) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseClicked(mouseX, mouseY, mouseButton)) {
                    this.setFocused(child);
                    return true;
                }
            }
            return true;
        }

        // Handle script path clicks
        Path hoveredScriptPath = getHoveredScriptPath(mouseX, mouseY);
        if (hoveredScriptPath != null && mouseButton == 1) {
            this.fieldDisk.playDownSound(Minecraft.getInstance().getSoundManager());
            this.openRenameDialog(hoveredScriptPath);
            return true;
        }
        if (hoveredScriptPath != null) {
            // Handle script removal clicks
            if (mouseX >= this.leftPos + PATHS_X + PATHS_WIDTH - (int) (Images.ERROR.getWidth() * 0.4F) - 1) {
                this.fieldDisk.playDownSound(Minecraft.getInstance().getSoundManager());

                // Show confirmation dialog
                pendingDialog = new WidgetDialog(font, leftPos + getBaseXSize() / 2 - WidgetDialog.WIDTH / 2, topPos + 50, this,
                        Component.translatable("gui.integratedscripting.removal_dialog.title"),
                        Component.translatable("gui.integratedscripting.removal_dialog.message", hoveredScriptPath.toString()),
                        Component.translatable("gui.integratedscripting.removal_dialog.delete"),
                        Component.translatable("gui.integratedscripting.removal_dialog.keep"),
                        (b) -> {
                            removeWidget(pendingDialog);
                            pendingDialog = null;
                            this.removeScript(hoveredScriptPath);
                        },
                        (b) -> {
                            removeWidget(pendingDialog);
                            pendingDialog = null;
                        });
                addRenderableWidget(pendingDialog);

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
        Path path = getMenu().getActiveScriptPath();
        this.textArea.setSyntaxError(path == null ? null : getMenu().getSyntaxError(getMenu().getActiveDisk(), path));
        this.textArea.setHistory(path == null ? new TextHistory() : scriptHistories.computeIfAbsent(Pair.of(getMenu().getActiveDisk(), path), k -> new TextHistory()));
    }

    private void createNewFile() {
        this.createdScriptDisk = getMenu().getActiveDisk();
        getMenu().createNewFile();
    }

    private void onRemoteScriptChanged(int disk, Path path, boolean created) {
        // Open a newly created script
        if (created && disk == this.createdScriptDisk && disk == getMenu().getActiveDisk()) {
            this.createdScriptDisk = -1;
            this.saveCursorPos();
            getMenu().setActiveScriptPath(path);
            this.onActiveScriptSelected();
            this.setFocused(this.textArea);
            return;
        }

        // Show changes from outside this editor, such as external file edits, instead of overwriting them on the next edit
        if (disk == getMenu().getActiveDisk() && path.equals(getMenu().getActiveScriptPath())) {
            String script = getMenu().getActiveScript();
            if (script != null && !script.equals(this.textArea.getValue())) {
                this.textArea.replaceValue(script);
            }
        }
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

                // Only bind to declarations, so moving through function bodies doesn't rebind the card
                Path path = getMenu().getActiveScriptPath();
                ILanguageHandler languageHandler = path != null ? LanguageHandlers.REGISTRY.getProvider(path) : null;
                Set<String> members = languageHandler != null ? languageHandler.getTopLevelMembers(value) : null;
                if (members != null && !members.contains(selected)) {
                    String previous = getMenu().getSelectedMember();
                    selected = members.contains(previous) ? previous : "";
                }
            }
        }

        getMenu().setSelection(selected);
    }

    private void openRenameDialog(Path path) {
        pendingDialog = new WidgetDialog(font, leftPos + getBaseXSize() / 2 - WidgetDialog.WIDTH / 2, topPos + 50, this,
                Component.translatable("gui.integratedscripting.rename_dialog.title"),
                Component.translatable("gui.integratedscripting.rename_dialog.message"),
                Component.translatable("gui.integratedscripting.rename_dialog.rename"),
                Component.translatable("gui.integratedscripting.rename_dialog.cancel"),
                (b) -> {
                    String name = pendingDialog.getInput().getValue();
                    removeWidget(pendingDialog);
                    pendingDialog = null;
                    this.renameScript(path, name);
                },
                (b) -> {
                    removeWidget(pendingDialog);
                    pendingDialog = null;
                }, path.toString());
        pendingDialog.setWarning(Component.translatable("gui.integratedscripting.rename_dialog.warning"));
        addRenderableWidget(pendingDialog);
    }

    private void renameScript(Path from, String name) {
        int disk = getMenu().getActiveDisk();
        Map<Path, String> scripts = getActiveScripts();
        Path to;
        try {
            to = Path.of(name.trim());
        } catch (InvalidPathException e) {
            return;
        }
        if (scripts == null || to.toString().isEmpty() || scripts.containsKey(to) || !scripts.containsKey(from)) {
            return;
        }

        // Apply the rename locally, so an open script stays open with its editor state
        String script = scripts.get(from);
        boolean active = from.equals(getMenu().getActiveScriptPath());
        getMenu().setLastScript(disk, to, script);
        getMenu().setLastScript(disk, from, null);
        TextHistory history = scriptHistories.remove(Pair.of(disk, from));
        if (history != null) {
            scriptHistories.put(Pair.of(disk, to), history);
        }
        if (active) {
            getMenu().setActiveScriptPath(to);
        }
        IntegratedScripting._instance.getPacketHandler().sendToServer(new TerminalScriptingRenameScriptPacket(disk, from, to));
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
        if (pendingDialog != null) {
            return false;
        }

        if (textArea.mouseDragged(mouseX, mouseY, mouseButton, offsetX, offsetY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        // Always let the editor know, as a drag selection can be released anywhere
        textArea.mouseReleased(mouseX, mouseY, mouseButton);
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {
        // Make active dialog consume all input
        if (pendingDialog != null && pendingDialog.getInput() != null) {
            if (typedChar == GLFW.GLFW_KEY_ESCAPE) {
                pendingDialog.cancel();
            } else if (typedChar == GLFW.GLFW_KEY_ENTER || typedChar == GLFW.GLFW_KEY_KP_ENTER) {
                pendingDialog.confirm();
            } else {
                pendingDialog.getInput().keyPressed(typedChar, keyCode, modifiers);
            }
            return true;
        }
        if (typedChar != GLFW.GLFW_KEY_ESCAPE && pendingDialog != null) {
            return false;
        }

        if (this.searchBar != null && this.searchBar.isFocused()) {
            return this.searchBar.keyPressed(typedChar, keyCode, modifiers);
        }
        // Close the search bar before closing the screen
        if (this.searchBar != null && typedChar == GLFW.GLFW_KEY_ESCAPE) {
            closeNavigate();
            return true;
        }

        // Leave focus mode instead of closing the screen
        if (focusMode && typedChar == GLFW.GLFW_KEY_ESCAPE && pendingDialog == null) {
            this.toggleFocusMode();
            return true;
        }

        if (textArea.isFocused()) {
            boolean ret = textArea.keyPressed(typedChar, keyCode, modifiers);
            if (typedChar != GLFW.GLFW_KEY_ESCAPE) {
                return ret;
            }
        }
        return super.keyPressed(typedChar, keyCode, modifiers);
    }

    /**
     * Show a bar below the editor to find text, or to go to a line number.
     */
    private void openNavigate(boolean toLine) {
        String selected = this.textArea.getSelected();
        String initialValue = !toLine && !selected.isEmpty() && selected.indexOf('\n') < 0 ? selected : "";
        if (this.searchBar != null && this.searchBar.isLineMode() == toLine) {
            this.searchBar.focusAndSelect(initialValue);
        } else {
            closeNavigate();
            // The editor gives up the space for the bar
            this.textArea.setHeight(this.textArea.getHeight() - WidgetSearchBar.HEIGHT);
            this.searchBar = new WidgetSearchBar(font, this.textArea.getX(), this.textArea.getY() + this.textArea.getHeight(), this.textArea.getWidth(),
                    toLine, initialValue, this::onSearchChanged, this::navigate, this::closeNavigate);
            addRenderableWidget(this.searchBar);
        }
        this.textArea.setFocused(false);
        this.setFocused(this.searchBar);
        this.onSearchChanged(this.searchBar.getValue());
    }

    private void closeNavigate() {
        if (this.searchBar != null) {
            removeWidget(this.searchBar);
            this.searchBar = null;
            this.textArea.setHeight(this.textArea.getHeight() + WidgetSearchBar.HEIGHT);
            this.textArea.setSearchQuery(null);
            this.setFocused(this.textArea);
            this.textArea.setFocused(true);
        }
    }

    private void onSearchChanged(String query) {
        if (this.searchBar.isLineMode()) {
            this.searchBar.setStatus("", !query.isBlank() && parseLine(query) < 0);
            return;
        }
        this.textArea.setSearchQuery(query);
        if (!query.isEmpty()) {
            // Stay on the current match while typing, or move to the next one
            int pos = TextEditing.findNext(this.textArea.getValue(), query, Math.min(this.textArea.getCursorPos(), this.textArea.getSelectionPos()));
            if (pos >= 0) {
                this.textArea.selectRange(pos, pos + query.length());
            }
        }
        this.updateSearchStatus();
    }

    private void updateSearchStatus() {
        if (this.searchBar == null || this.searchBar.isLineMode()) {
            return;
        }
        String query = this.searchBar.getValue();
        if (query.isEmpty()) {
            this.searchBar.setStatus("", false);
            return;
        }
        int[] matches = this.textArea.getSearchMatches();
        int start = Math.min(this.textArea.getCursorPos(), this.textArea.getSelectionPos());
        int end = Math.max(this.textArea.getCursorPos(), this.textArea.getSelectionPos());
        int current = 0;
        for (int i = 0; i < matches.length; i++) {
            if (matches[i] == start && end - start == query.length()) {
                current = i + 1;
            }
        }
        this.searchBar.setStatus(current + "/" + matches.length, matches.length == 0);
    }

    private static int parseLine(String value) {
        try {
            int line = Integer.parseInt(value.trim());
            return line > 0 ? line : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @param direction 1 for the next match, -1 for the previous match.
     */
    private void navigate(int direction) {
        String query = this.searchBar.getValue();
        String value = this.textArea.getValue();
        if (this.searchBar.isLineMode()) {
            int line = parseLine(query);
            if (line < 0) {
                this.searchBar.setStatus("", true);
                return;
            }
            int pos = TextEditing.lineOffset(value, line);
            closeNavigate();
            this.textArea.selectRange(pos, pos);
            return;
        }
        int pos = direction > 0
                ? TextEditing.findNext(value, query, Math.max(this.textArea.getCursorPos(), this.textArea.getSelectionPos()))
                : TextEditing.findPrevious(value, query, Math.min(this.textArea.getCursorPos(), this.textArea.getSelectionPos()));
        if (pos >= 0) {
            this.textArea.selectRange(pos, pos + query.length());
        }
        this.updateSearchStatus();
    }

    @Override
    public boolean charTyped(char p_94683_, int p_94684_) {
        // Make active dialog consume all input
        if (pendingDialog != null) {
            return pendingDialog.getInput() != null && pendingDialog.getInput().charTyped(p_94683_, p_94684_);
        }
        if (this.searchBar != null && this.searchBar.isFocused()) {
            return this.searchBar.charTyped(p_94683_, p_94684_);
        }
        return super.charTyped(p_94683_, p_94684_);
    }
}
