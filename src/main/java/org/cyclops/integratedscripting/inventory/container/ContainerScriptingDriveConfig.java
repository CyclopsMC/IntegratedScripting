package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.client.gui.container.ContainerScreenScriptingDrive;

/**
 * Config for {@link ContainerScriptingDrive}.
 * @author rubensworks
 */
public class ContainerScriptingDriveConfig extends GuiConfig<ContainerScriptingDrive> {

    public ContainerScriptingDriveConfig() {
        super(IntegratedScripting._instance,
                "scripting_drive",
                eConfig -> new MenuType<>(ContainerScriptingDrive::new));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerScriptingDrive>> MenuScreens.ScreenConstructor<ContainerScriptingDrive, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenScriptingDrive::new);
    }

}
