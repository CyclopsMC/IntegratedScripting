package org.cyclops.integratedscripting.item;

import org.cyclops.cyclopscore.config.extendedconfig.ItemConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for a scripting disk item.
 * @author rubensworks
 */
public class ItemScriptingDiskConfig extends ItemConfigCommon<IModBase> {

    public ItemScriptingDiskConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_disk",
                (eConfig, properties) -> new ItemScriptingDisk(properties)
        );
    }
}
