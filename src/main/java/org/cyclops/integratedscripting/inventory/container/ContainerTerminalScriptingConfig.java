package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for {@link ContainerTerminalScripting}.
 * @author rubensworks
 */
public class ContainerTerminalScriptingConfig extends GuiConfigCommon<ContainerTerminalScripting, IModBase> {

    public ContainerTerminalScriptingConfig() {
        super(IntegratedScripting._instance,
                "part_terminal_scripting",
                eConfig -> new ContainerTypeData<>(ContainerTerminalScripting::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerTerminalScripting> getScreenFactoryProvider() {
        return new ContainerTerminalScriptingConfigScreenFactoryProvider();
    }
}
