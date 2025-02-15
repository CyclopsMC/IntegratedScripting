package org.cyclops.integratedscripting.blockentity;

import com.google.common.collect.Sets;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockEntityConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.RegistryEntries;

/**
 * Config for the {@link BlockEntityScriptingDrive}.
 * @author rubensworks
 *
 */
public class BlockEntityScriptingDriveConfig extends BlockEntityConfigCommon<BlockEntityScriptingDrive, IModBase> {

    public BlockEntityScriptingDriveConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_drive",
                (eConfig) -> new BlockEntityType<>(BlockEntityScriptingDrive::new,
                        Sets.newHashSet(RegistryEntries.BLOCK_SCRIPTING_DRIVE.get()))
        );
        IntegratedScripting._instance.getModEventBus().addListener(new BlockEntityScriptingDrive.CapabilityRegistrar(this::getInstance)::register);
    }

}
