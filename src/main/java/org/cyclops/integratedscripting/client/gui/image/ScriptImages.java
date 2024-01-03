package org.cyclops.integratedscripting.client.gui.image;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.cyclopscore.client.gui.image.Image;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * @author rubensworks
 */
public class ScriptImages {

    public static final ResourceLocation ICONS = new ResourceLocation(IntegratedScripting._instance.getModId(),
            IntegratedScripting._instance.getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI) + "icons.png");

    public static final Image FILE_OTHER = new Image(ICONS, 0, 0, 12, 12);
    public static final Image FILE_JS = new Image(ICONS, 12, 0, 12, 12);

}
