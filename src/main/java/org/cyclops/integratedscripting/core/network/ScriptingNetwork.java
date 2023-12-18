package org.cyclops.integratedscripting.core.network;

import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.expression.VariableAdapter;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.api.network.IScriptMember;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.language.LanguageHandlers;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ScriptingNetwork implements IScriptingNetwork {

    private final Set<Integer> disks = Sets.newHashSet();

    @Override
    public void addDisk(int disk) {
        disks.add(disk);
    }

    @Override
    public void removeDisk(int disk) {
        disks.remove(disk);
    }

    @Override
    public Set<Integer> getDisks() {
        return Collections.unmodifiableSet(this.disks);
    }

    @Nullable
    @Override
    public IScript getScript(int disk, Path path) throws EvaluationException {
        // TODO: cache the following

        ILanguageHandler languageHandler = LanguageHandlers.REGISTRY.getProvider(path);
        if (languageHandler == null) {
            throw new EvaluationException(Component.translatable("script.integratedscripting.error.unsupported_language", path.toString()));
        }
        IScriptFactory scriptFactory = languageHandler.getScriptFactory();
        return scriptFactory.getScript(disk, path);
    }

    @Override
    public <V extends IValue> IVariable<V> getOrCreateVariable(int disk, Path path, String member) {
        // TODO: variable caching and invalidation (listen to file changes via IScript to ScriptingData)

        Wrapper<IScript> script = new Wrapper<>();
        VariableAdapter<IValue> variable = new VariableAdapter<IValue>() {

            @Override
            public IValueType<IValue> getType() {
                return ValueTypes.CATEGORY_ANY;
            }

            @Override
            public IValue getValue() throws EvaluationException {
                if (script.get() == null) {
                    // TODO: cache error?
                    script.set(getScript(disk, path));
                }
                IScriptMember scriptMember = script.get().getMember(member);
                if (scriptMember == null) {
                    throw new EvaluationException(Component.translatable("script.integratedscripting.error.member_not_in_network", member, path.toString()));
                }
                return scriptMember.getValue();
            }
        };

        return (IVariable) variable;
    }

}
