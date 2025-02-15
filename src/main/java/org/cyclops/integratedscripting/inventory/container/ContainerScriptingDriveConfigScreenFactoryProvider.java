package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedscripting.client.gui.container.ContainerScreenScriptingDrive;

/**
 * @author rubensworks
 */
public class ContainerScriptingDriveConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerScriptingDrive> {
    @Override
    public <U extends Screen & MenuAccess<ContainerScriptingDrive>> MenuScreens.ScreenConstructor<ContainerScriptingDrive, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenScriptingDrive::new);
    }
}
