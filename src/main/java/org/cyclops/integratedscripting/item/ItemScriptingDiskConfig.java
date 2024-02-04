package org.cyclops.integratedscripting.item;

import net.minecraft.world.item.Item;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for a scripting disk item.
 * @author rubensworks
 */
public class ItemScriptingDiskConfig extends ItemConfig {

    public ItemScriptingDiskConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_disk",
                eConfig -> new ItemScriptingDisk(new Item.Properties())
        );
    }
}
