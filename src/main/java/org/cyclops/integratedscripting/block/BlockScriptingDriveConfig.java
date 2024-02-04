package org.cyclops.integratedscripting.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for {@link BlockScriptingDrive}.
 * @author rubensworks
 */
public class BlockScriptingDriveConfig extends BlockConfig {

    public BlockScriptingDriveConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_drive",
                eConfig -> new BlockScriptingDrive(Block.Properties.of()
                        .strength(2.0F, 5.0F)
                        .sound(SoundType.METAL)),
                getDefaultItemConstructor(IntegratedScripting._instance)
        );
    }

}
