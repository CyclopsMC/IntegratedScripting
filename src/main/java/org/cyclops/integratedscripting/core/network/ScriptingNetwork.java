package org.cyclops.integratedscripting.core.network;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.evaluate.ScriptVariable;
import org.cyclops.integratedscripting.core.language.LanguageHandlers;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ScriptingNetwork implements IScriptingNetwork {

    private final Set<Integer> disks = Sets.newHashSet();
    private final Map<Triple<Integer, Path, String>, ScriptVariable> variableCache = Maps.newHashMap();

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
        ILanguageHandler languageHandler = LanguageHandlers.REGISTRY.getProvider(path);
        if (languageHandler == null) {
            throw new EvaluationException(Component.translatable("script.integratedscripting.error.unsupported_language", path.toString()));
        }
        IScriptFactory scriptFactory = languageHandler.getScriptFactory();
        return scriptFactory.getScript(disk, path);
    }

    @Override
    public <V extends IValue> IVariable<V> getVariable(int disk, Path path, String member) {
        Triple<Integer, Path, String> cacheKey = Triple.of(disk, path, member);
        ScriptVariable variable = variableCache.get(cacheKey);
        if (variable == null) {
            variable = new ScriptVariable(disk, path, member, this);
            variable.addInvalidationListener(() -> variableCache.remove(cacheKey));
            variableCache.put(cacheKey, variable);
        }
        return (IVariable) variable;
    }

}
