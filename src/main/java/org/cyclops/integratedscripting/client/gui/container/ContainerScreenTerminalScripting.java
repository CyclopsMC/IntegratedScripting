package org.cyclops.integratedscripting.client.gui.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetArrowedListField;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetDialog;
import org.cyclops.integratedscripting.client.gui.component.input.WidgetTextArea;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingDeleteScriptPacket;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerScreenTerminalScripting extends ContainerScreenExtended<ContainerTerminalScripting> {

    public static int PATHS_X = 19;
    public static int PATHS_Y = 18;
    public static int PATHS_WIDTH = 56;
    public static int PATHS_HEIGHT = 230;
    public static int PATHS_ROW_HEIGHT = 5;
    public static int PATHS_MAX_ROWS = PATHS_HEIGHT / PATHS_ROW_HEIGHT;
    public static int SCRIPT_X = 88;
    public static int SCRIPT_Y = 18;
    public static int SCRIPT_WIDTH = 160;
    public static int SCRIPT_HEIGHT = 131;

    private final Player player;

    private WidgetArrowedListField<Integer> fieldDisk;
    private WidgetScrollBar scrollBar;
    private int firstRow;
    private WidgetTextArea textArea;
    private ButtonText buttonCreateFile;
    private WidgetDialog pendingScriptRemovalDialog;

    public ContainerScreenTerminalScripting(ContainerTerminalScripting container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.player = inventory.player;

        this.imageHeight = 256;
        this.titleLabelX = 88;
        this.titleLabelY = 6;
        this.inventoryLabelX = 88;
        this.inventoryLabelY = this.imageHeight - 94;
        this.firstRow = 0;
    }

    @Override
    public void init() {
        super.init();

        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

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

        scrollBar = new WidgetScrollBar(leftPos + 5, topPos + 18, 178,
                Component.translatable("gui.cyclopscore.scrollbar"), this::setFirstRow, PATHS_MAX_ROWS) {
            @Override
            public int getTotalRows() {
                Map<Path, String> scripts = getActiveScripts();
                return scripts == null ? 0 : scripts.keySet().size();
            }
        };

        textArea = new WidgetTextArea(Minecraft.getInstance().font, this.leftPos + SCRIPT_X + 1, this.topPos + SCRIPT_Y + 1, SCRIPT_WIDTH, SCRIPT_HEIGHT, Component.translatable("gui.integratedscripting.script"));
        textArea.setListener(this::onActiveScriptModified);
        addRenderableWidget(textArea);

        buttonCreateFile = new ButtonText(this.leftPos + 19, this.topPos + 238, 56, 10, Component.translatable("gui.integratedscripting.create_file"), Component.literal("+"),
                (button) -> getMenu().createNewFile(), true);
        addRenderableWidget(buttonCreateFile);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        textArea.tick();
    }

    public void removed() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return new ResourceLocation(Reference.MOD_ID, "textures/gui/scripting_terminal.png");
    }

    @Override
    public int getBaseXSize() {
        return 256;
    }

    @Override
    public int getBaseYSize() {
        return 256;
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        RenderHelpers.bindTexture(this.texture);
        fieldDisk.render(matrixStack, mouseX, mouseY, partialTicks);
        scrollBar.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);

        if (!this.getMenu().getAvailableDisks().isEmpty()) {
            this.renderScriptPaths(matrixStack, partialTicks, mouseX, mouseY);
        } else {
            // Gray-out editor and file list
            RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 0.3F);
            fill(matrixStack, leftPos + PATHS_X, topPos + PATHS_Y, leftPos + PATHS_X + PATHS_WIDTH, topPos + PATHS_Y + PATHS_HEIGHT, Helpers.RGBAToInt(50, 50, 50, 100));
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        if (this.getMenu().getActiveScript() == null) {
            // Gray-out editor and file list
            RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 0.3F);
            fill(matrixStack, leftPos + SCRIPT_X, topPos + SCRIPT_Y, leftPos + SCRIPT_X + SCRIPT_WIDTH, topPos + SCRIPT_Y + SCRIPT_HEIGHT, Helpers.RGBAToInt(50, 50, 50, 100));
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
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

    protected void renderScriptPaths(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
        List<Path> paths = getVisibleScriptPaths();
        int i = 0;
        for (Path path : paths) {
            boolean hovering = isHovering(PATHS_X, PATHS_Y + i * PATHS_ROW_HEIGHT, PATHS_WIDTH, PATHS_ROW_HEIGHT, mouseX, mouseY);
            boolean active = path.equals(getMenu().getActiveScriptPath());

            // Draw highlighted background behind script path text
            if (active) {
                fill(poseStack, this.leftPos + PATHS_X, this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT, this.leftPos + PATHS_X + PATHS_WIDTH, this.topPos + PATHS_Y + (i + 1) * PATHS_ROW_HEIGHT, Helpers.RGBAToInt(110, 130, 240, 255));
            }

            RenderHelpers.drawScaledString(
                    poseStack,
                    font,
                    StringUtil.truncateStringIfNecessary(path.toString(), 50, true),
                    this.leftPos + PATHS_X + 1,
                    this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT + 1,
                    0.5f,
                    hovering && !active ? Helpers.RGBToInt(50, 50, 250) : Helpers.RGBToInt(0, 0, 0)
            );

            // If hovering, render removal button
            if (hovering) {
                poseStack.pushPose();
                float scale = 0.4F;
                int size = (int) (Images.ERROR.getWidth() * scale);
                poseStack.translate(this.leftPos + PATHS_X + PATHS_WIDTH - size, this.topPos + PATHS_Y + i * PATHS_ROW_HEIGHT, 0);
                poseStack.scale(scale, scale, 4F);
                if (isHovering(PATHS_X + PATHS_WIDTH - size, PATHS_Y + i * PATHS_ROW_HEIGHT, PATHS_X + PATHS_WIDTH - size + size, PATHS_Y + i * PATHS_ROW_HEIGHT + size, mouseX, mouseY)) {
                    Images.ERROR.draw(this, poseStack, 0, 0);
                } else {
                    Images.ERROR.drawWithColor(this, poseStack, 0, 0, 0.7F, 0.7F, 0.7F, 1F);
                }
                poseStack.popPose();
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
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        super.renderLabels(poseStack, mouseX, mouseY);

        // Draw disk label
        drawString(poseStack, font, L10NHelpers.localize("gui.integratedscripting.disk") + ":", 8, 6, 16777215);
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
            if (mouseX >= this.leftPos + PATHS_X + PATHS_WIDTH - (int) (Images.ERROR.getWidth() * 0.4F)) {
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
            } else{
                getMenu().setActiveScriptPath(hoveredScriptPath);
                this.onActiveScriptSelected();
                this.fieldDisk.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        // Update channel when changing channel field
        if (this.fieldDisk.mouseClicked(mouseX, mouseY, mouseButton)) {
            int disk;
            try {
                disk = this.fieldDisk.getActiveElement();
            } catch (NumberFormatException e) {
                disk = -1;
            }
            getMenu().setActiveDisk(disk);
            scrollBar.scrollTo(0); // Reset scrollbar

            playButtonClickSound();

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void playButtonClickSound() {
        this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void setFirstRow(int firstRow) {
        this.firstRow = firstRow;
    }

    private void onActiveScriptSelected() {
        this.textArea.setValue(getMenu().getActiveScript());
    }

    private void onActiveScriptModified() {
        getMenu().setActiveScript(this.textArea.getValue());
    }

    private void removeScript(Path path) {
        IntegratedScripting._instance.getPacketHandler()
                .sendToServer(new TerminalScriptingDeleteScriptPacket(getMenu().getActiveDisk(), path));
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
