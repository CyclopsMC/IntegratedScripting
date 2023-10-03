package org.cyclops.integratedscripting.client.gui.container;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.cyclops.cyclopscore.client.gui.component.WidgetScrollBar;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

/**
 * The crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerScreenTerminalScripting extends ContainerScreenExtended<ContainerTerminalScripting> {

    private final Player player;

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
        scrollBar.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
        RenderHelpers.bindTexture(this.texture);
    }

    public void setFirstRow(int firstRow) {
        // TODO: handle scrolling
    }
}
