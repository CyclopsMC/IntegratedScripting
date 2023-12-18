package org.cyclops.integratedscripting.core.network;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptMember;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

/**
 * A script referencing a Graal value.
 * @author rubensworks
 */
public class GraalScript implements IScript, IScriptMember {

    private final Context graalContext;
    private final Value graalValue;

    public GraalScript(Context graalContext, Value graalValue) {
        this.graalContext = graalContext;
        this.graalValue = graalValue;
    }

    @Nullable
    @Override
    public IScriptMember getMember(String memberName) {
        Value member = this.graalValue.getMember(memberName);
        return member == null ? null : new GraalScript(this.graalContext, member);
    }

    @Override
    public IValue getValue() throws EvaluationException {
        return ValueTranslators.REGISTRY.translateFromGraal(this.graalContext, graalValue);
    }
}
