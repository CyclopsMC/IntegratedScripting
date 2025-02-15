package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedscripting.client.gui.container.ContainerScreenTerminalScripting;

/**
 * @author rubensworks
 */
public class ContainerTerminalScriptingConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerTerminalScripting> {
    @Override
    public <U extends Screen & MenuAccess<ContainerTerminalScripting>> MenuScreens.ScreenConstructor<ContainerTerminalScripting, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenTerminalScripting::new);
    }
}
