package org.cyclops.integratedscripting.client.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.inventory.container.ContainerScriptingDrive;

/**
 * Gui for the scripting drive
 * @author rubensworks
 */
public class ContainerScreenScriptingDrive extends ContainerScreenExtended<ContainerScriptingDrive> {

    public ContainerScreenScriptingDrive(ContainerScriptingDrive container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scripting_drive.png");
    }

    @Override
    protected int getBaseYSize() {
        return 128;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int x, int y) {
        // super.drawGuiContainerForegroundLayer(matrixStack, x, y);
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
}
