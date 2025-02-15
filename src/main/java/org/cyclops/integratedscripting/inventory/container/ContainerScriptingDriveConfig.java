package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for {@link ContainerScriptingDrive}.
 * @author rubensworks
 */
public class ContainerScriptingDriveConfig extends GuiConfigCommon<ContainerScriptingDrive, IModBase> {

    public ContainerScriptingDriveConfig() {
        super(IntegratedScripting._instance,
                "scripting_drive",
                eConfig -> new MenuType<>(ContainerScriptingDrive::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerScriptingDrive> getScreenFactoryProvider() {
        return new ContainerScriptingDriveConfigScreenFactoryProvider();
    }
}
