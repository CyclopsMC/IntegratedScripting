package org.cyclops.integratedscripting.modcompat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.client.gui.container.ContainerScreenTerminalScripting;

import java.util.Collections;
import java.util.List;

/**
 * Only loaded by JEI if it is installed.
 * @author rubensworks
 */
@JeiPlugin
public class JEIIntegratedScriptingPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "main");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ContainerScreenTerminalScripting.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ContainerScreenTerminalScripting screen) {
                // Claim the whole screen in focus mode, so JEI hides its overlays instead of drawing them over the editor
                return screen.isFocusMode() ? List.of(new Rect2i(0, 0, screen.width, screen.height)) : Collections.emptyList();
            }
        });
    }

}
