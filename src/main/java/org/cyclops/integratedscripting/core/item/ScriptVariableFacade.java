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
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.L10NValues;
import org.cyclops.integrateddynamics.core.item.VariableFacadeBase;
import org.cyclops.integratedscripting.api.item.IScriptVariableFacade;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.client.model.ScriptingVariableModelProviders;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

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
    public <V extends IValue> IVariable<V> getVariable(INetwork network, IPartNetwork partNetwork) {
        if(isValid()) {
            return ScriptingNetworkHelpers.getScriptingNetwork(network)
                    .map(scriptingNetwork -> scriptingNetwork.<V>getVariable(this.disk, this.path, this.member))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return disk >= 0;
    }

    @Override
    public void validate(INetwork network, IPartNetwork partNetwork, IValidator validator, IValueType containingValueType) {
        IScriptingNetwork scriptingNetwork = ScriptingNetworkHelpers.getScriptingNetwork(network).orElse(null);
        try {
            if (!isValid()) {
                validator.addError(Component.translatable(L10NValues.VARIABLE_ERROR_INVALIDITEM));
            } else if (scriptingNetwork == null) {
                validator.addError(Component.translatable("script.integratedscripting.error.invalid_network"));
            } else if (!scriptingNetwork.getDisks().contains(this.disk)) {
                validator.addError(Component.translatable("script.integratedscripting.error.disk_not_in_network", this.disk));
            } else if (scriptingNetwork.getScript(this.disk, this.path) == null) {
                validator.addError(Component.translatable("script.integratedscripting.error.path_not_in_network", this.disk, this.path.toString()));
            } else if (scriptingNetwork.getScript(this.disk, this.path).getMember(this.member) == null) {
                validator.addError(Component.translatable("script.integratedscripting.error.member_not_in_network", this.disk, this.member, this.path.toString()));
            } else {
                // Check if the expected type corresponds to the actual value's type produced by this script.
                IValue value = scriptingNetwork.getScript(this.disk, this.path).getMember(this.member).getValue();
                if (!ValueHelpers.correspondsTo(containingValueType, value.getType())) {
                    validator.addError(Component.translatable("script.integratedscripting.error.invalid_type",
                            this.member, this.path.toString(), this.disk,
                            Component.translatable(containingValueType.getTranslationKey()),
                            Component.translatable(value.getType().getTranslationKey())));
                }
            }
        } catch (EvaluationException e) {
            validator.addError(e.getErrorMessage());
        }
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
