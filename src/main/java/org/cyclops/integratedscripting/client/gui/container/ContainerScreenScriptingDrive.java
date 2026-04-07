package org.cyclops.integratedscripting.client.gui.container;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
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
    protected Identifier constructGuiTexture() {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scripting_drive.png");
    }

    @Override
    protected int getBaseYSize() {
        return 128;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        // super.drawGuiContainerForegroundLayer(matrixStack, x, y);
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, ARGB.opaque(4210752), false);
    }
}
