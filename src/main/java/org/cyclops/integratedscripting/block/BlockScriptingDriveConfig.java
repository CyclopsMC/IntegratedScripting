package org.cyclops.integratedscripting.block;

import net.minecraft.world.level.block.SoundType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for {@link BlockScriptingDrive}.
 * @author rubensworks
 */
public class BlockScriptingDriveConfig extends BlockConfigCommon<IModBase> {

    public BlockScriptingDriveConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_drive",
                (eConfig, properties) -> new BlockScriptingDrive(properties
                        .strength(2.0F, 5.0F)
                        .sound(SoundType.METAL)),
                getDefaultItemConstructor(IntegratedScripting._instance)
        );
    }

}
