package org.cyclops.integratedscripting.core.evaluate;

import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.expression.VariableAdapter;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptMember;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * A variable for a given script.
 * @author rubensworks
 */
public class ScriptVariable extends VariableAdapter<IValue> {

    private final int disk;
    private final Path path;
    private final String member;
    private final IScriptingNetwork scriptingNetwork;

    @Nullable
    protected IScript script;
    private EvaluationException lastEvaluationException;

    public ScriptVariable(int disk, Path path, String member, IScriptingNetwork scriptingNetwork) {
        this.disk = disk;
        this.path = path;
        this.member = member;
        this.scriptingNetwork = scriptingNetwork;
    }

    @Override
    public IValueType<IValue> getType() {
        return ValueTypes.CATEGORY_ANY;
    }

    @Override
    public IValue getValue() throws EvaluationException {
        if (this.lastEvaluationException != null) {
            throw this.lastEvaluationException;
        }

        // Fetch script from network lazily
        if (this.script == null) {
            try {
                this.script = this.scriptingNetwork.getScript(disk, path);

                // Listen to script changes to invalidate this variable.
                if (this.script != null) {
                    this.script.addInvalidationListener(this::invalidate);
                }
            } catch (EvaluationException e) {
                // Store error and re-throw later if the value is re-fetched.
                this.lastEvaluationException = e;
                throw this.lastEvaluationException;
            }
        }

        // If the script is still null, then we have a non-available script.
        if (this.script == null) {
            throw new EvaluationException(Component.translatable("script.integratedscripting.error.path_not_in_network", this.disk, this.path.toString()));
        }

        // Fetch the script member.
        IScriptMember scriptMember = this.script.getMember(member);
        if (scriptMember == null) {
            throw new EvaluationException(Component.translatable("script.integratedscripting.error.member_not_in_network", member, path.toString()));
        }
        return scriptMember.getValue();
    }

    @Override
    public void invalidate() {
        // Remove listeners on scripts
        if (this.script != null) {
            this.script.removeInvalidationListeners();
        }

        // Reset state
        this.script = null;
        this.lastEvaluationException = null;

        super.invalidate();
    }
}
