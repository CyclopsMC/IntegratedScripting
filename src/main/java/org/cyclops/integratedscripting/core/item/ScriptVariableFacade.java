package org.cyclops.integratedscripting.core.item;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.cyclops.integrateddynamics.api.client.model.IVariableModelBaked;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.item.VariableFacadeBase;
import org.cyclops.integratedscripting.api.item.IScriptVariableFacade;
import org.cyclops.integratedscripting.core.client.model.ScriptingVariableModelProviders;

import java.nio.file.Path;
import java.util.List;

/**
 * Variable facade for variables determined by proxies.
 * @author rubensworks
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ScriptVariableFacade extends VariableFacadeBase implements IScriptVariableFacade {

    private final int disk;
    private final Path path;
    private final String member;

    public ScriptVariableFacade(boolean generateId, int disk, Path path, String member) {
        super(generateId);
        this.disk = disk;
        this.path = path;
        this.member = member;
    }

    public ScriptVariableFacade(int id, int disk, Path path, String member) {
        super(id);
        this.disk = disk;
        this.path = path;
        this.member = member;
    }

    @Override
    public <V extends IValue> IVariable<V> getVariable(IPartNetwork network) {
        if(isValid()) {
            // TODO: get variable of script from scripting network
//            IVariable<V> variable = getTargetVariable(network).orElse(null);
//            return variable;
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return disk >= 0;
    }

    @Override
    public void validate(IPartNetwork network, IValidator validator, IValueType containingValueType) {
        // TODO: validation
//        Optional<IVariable> targetVariable = getTargetVariable(network);
//        if (!isValid()) {
//            validator.addError(Component.translatable(L10NValues.VARIABLE_ERROR_INVALIDITEM));
//        } else if (network.getScript(proxyId) == null) {
//            validator.addError(getScriptNotInNetworkError());
//        } else if (!targetVariable.isPresent()) {
//            validator.addError(getScriptInvalidError());
//        } else if (!ValueHelpers.correspondsTo(containingValueType, targetVariable.get().getType())) {
//            validator.addError(getScriptInvalidTypeError(network, containingValueType,
//                    targetVariable.get().getType()));
//        }

        getVariable(network);
    }

    @Override
    public IValueType getOutputType() {
        return ValueTypes.CATEGORY_ANY;
    }

    protected List<Component> getScriptTooltip() {
        return List.of(
                Component.translatable("script.integratedscripting.tooltip.disk", getDisk()),
                Component.translatable("script.integratedscripting.tooltip.path", getPath()),
                Component.translatable("script.integratedscripting.tooltip.member", getMember())
        );
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(List<Component> list, Level world) {
        if(isValid()) {
            list.addAll(getScriptTooltip());
        }
        super.appendHoverText(list, world);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addModelOverlay(IVariableModelBaked variableModelBaked, List<BakedQuad> quads, RandomSource random, ModelData modelData) {
        if(isValid()) {
            quads.addAll(variableModelBaked.getSubModels(ScriptingVariableModelProviders.SCRIPT).getBakedModel().getQuads(null, null, random, modelData, null));
        }
    }
}
