package org.cyclops.integratedscripting.core.evaluate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.item.IVariableFacade;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandler;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.item.IScriptVariableFacade;
import org.cyclops.integratedscripting.core.item.ScriptVariableFacade;

import java.nio.file.Path;

/**
 * Handler for script variable facades.
 * @author rubensworks
 */
public class ScriptVariableFacadeHandler implements IVariableFacadeHandler<IScriptVariableFacade> {

    private static final IScriptVariableFacade INVALID_FACADE = new ScriptVariableFacade(false, -1, Path.of(""), "");
    private static ScriptVariableFacadeHandler _instance;

    private ScriptVariableFacadeHandler() {

    }

    public static ScriptVariableFacadeHandler getInstance() {
        if(_instance == null) _instance = new ScriptVariableFacadeHandler();
        return _instance;
    }

    @Override
    public Identifier getUniqueName() {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "script");
    }

    @Override
    public IScriptVariableFacade getVariableFacade(ValueDeseralizationContext valueDeseralizationContext, int id, CompoundTag tag) {
        if ((!tag.contains("disk") && !tag.contains("disk")) || !tag.contains("path") || !tag.contains("member")) {
            return INVALID_FACADE;
        }
        return new ScriptVariableFacade(id, tag.getInt("disk").orElseThrow(), Path.of(tag.getString("path").orElseThrow()), tag.getString("member").orElseThrow());
    }

    @Override
    public void setVariableFacade(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag, IScriptVariableFacade variableFacade) {
        tag.putInt("disk", variableFacade.getDisk());
        tag.putString("path", variableFacade.getPath().toString());
        tag.putString("member", variableFacade.getMember());
    }

    @Override
    public boolean isInstance(IVariableFacade variableFacade) {
        return variableFacade instanceof IScriptVariableFacade;
    }

    @Override
    public boolean isInstance(IVariable<?> variable) {
        return variable instanceof ScriptVariable;
    }
}
