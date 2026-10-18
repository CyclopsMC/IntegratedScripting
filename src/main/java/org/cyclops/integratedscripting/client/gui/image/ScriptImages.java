package org.cyclops.integratedscripting.client.gui.image;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.cyclopscore.client.gui.image.Image;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * @author rubensworks
 */
public class ScriptImages {

    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(IntegratedScripting._instance.getModId(),
            IntegratedScripting._instance.getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI) + "icons.png");

    public static final Image FILE_OTHER = new Image(ICONS, 0, 0, 12, 12);
    public static final Image FILE_JS = new Image(ICONS, 12, 0, 12, 12);

    public static final Image ACTION_UNDO = new Image(ICONS, 0, 16, 8, 8);
    public static final Image ACTION_REDO = new Image(ICONS, 8, 16, 8, 8);
    public static final Image ACTION_FIND = new Image(ICONS, 16, 16, 8, 8);
    public static final Image ACTION_GOTO_LINE = new Image(ICONS, 24, 16, 8, 8);
    public static final Image ACTION_COMMENT = new Image(ICONS, 32, 16, 8, 8);
    public static final Image ACTION_HELP = new Image(ICONS, 40, 16, 8, 8);

}
