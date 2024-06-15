package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.client.gui.container.ContainerScreenTerminalScripting;

/**
 * Config for {@link ContainerTerminalScripting}.
 * @author rubensworks
 */
public class ContainerTerminalScriptingConfig extends GuiConfig<ContainerTerminalScripting> {

    public ContainerTerminalScriptingConfig() {
        super(IntegratedScripting._instance,
                "part_terminal_scripting",
                eConfig -> new ContainerTypeData<>(ContainerTerminalScripting::new, FeatureFlags.VANILLA_SET));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerTerminalScripting>> MenuScreens.ScreenConstructor<ContainerTerminalScripting, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenTerminalScripting::new);
    }

}
