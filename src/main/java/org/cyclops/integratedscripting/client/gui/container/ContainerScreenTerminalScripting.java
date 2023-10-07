package org.cyclops.integratedscripting.client.gui.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetArrowedListField;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

/**
 * The crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerScreenTerminalScripting extends ContainerScreenExtended<ContainerTerminalScripting> {

    private final Player player;

    private WidgetArrowedListField<Integer> fieldDisk;
    private WidgetScrollBar scrollBar;

    public ContainerScreenTerminalScripting(ContainerTerminalScripting container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.player = inventory.player;

        this.imageHeight = 256;
        this.titleLabelX = 88;
        this.titleLabelY = 6;
        this.inventoryLabelX = 88;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void init() {
        super.init();

        fieldDisk = new WidgetArrowedListField<>(Minecraft.getInstance().font, leftPos + 207,
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
                Component.translatable("gui.cyclopscore.scrollbar"), this::setFirstRow, 10);
        scrollBar.setTotalRows(10); // TODO: set to number of files - 1
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
            this.renderScripts(matrixStack, partialTicks, mouseX, mouseY);
        } else {
            // Gray-out editor and file list
            RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 0.3F);
            fill(matrixStack, leftPos + 88, topPos + 18, leftPos + 88 + 160, topPos + 18 + 131, Helpers.RGBAToInt(50, 50, 50, 100));
            fill(matrixStack, leftPos + 19, topPos + 18, leftPos + 19 + 56, topPos + 18 + 230, Helpers.RGBAToInt(50, 50, 50, 100));
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    protected void renderScripts(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        // TODO: render scripts
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        super.renderLabels(poseStack, mouseX, mouseY);

        // Draw disk label
        drawString(poseStack, font, L10NHelpers.localize("gui.integratedscripting.disk") + ":", 185, 6, 16777215);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
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
        // TODO: handle scrolling
    }
}
