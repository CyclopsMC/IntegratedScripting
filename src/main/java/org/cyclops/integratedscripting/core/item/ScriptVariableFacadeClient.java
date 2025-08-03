package org.cyclops.integratedscripting.core.item;

import net.minecraft.client.renderer.item.ItemModel;
import org.cyclops.integrateddynamics.api.client.model.IVariableModelBaked;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeClient;
import org.cyclops.integratedscripting.core.client.model.ScriptingVariableModelProviders;
import org.jetbrains.annotations.Nullable;

/**
 * @author rubensworks
 */
public class ScriptVariableFacadeClient implements IVariableFacadeClient {

    private final ScriptVariableFacade variableFacade;

    public ScriptVariableFacadeClient(ScriptVariableFacade variableFacade) {
        this.variableFacade = variableFacade;
    }

    @Override
    public @Nullable ItemModel getItemModelOverlay(IVariableModelBaked variableModelBaked) {
        if (this.variableFacade.isValid()) {
            return variableModelBaked.getSubModels(ScriptingVariableModelProviders.SCRIPT).getBakedModel();
        }
        return null;
    }
}
