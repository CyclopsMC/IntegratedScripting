package org.cyclops.integratedscripting.core.network;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptMember;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * A script referencing a Graal value.
 * @author rubensworks
 */
public class GraalScript implements IScript, IScriptMember {

    private final Context graalContext;
    private final Value graalValue;
    private final Consumer<IInvalidateListener> addInvalidationListener;
    private final Runnable removeInvalidationListener;
    private final int disk;
    private final Path path;
    @Nullable
    private final String member;
    private final ValueDeseralizationContext valueDeseralizationContext;

    public GraalScript(Context graalContext, Value graalValue,
                       Consumer<IInvalidateListener> addInvalidationListener,
                       Runnable removeInvalidationListener,
                       int disk, Path path, @Nullable String member, ValueDeseralizationContext valueDeseralizationContext) {
        this.graalContext = graalContext;
        this.graalValue = graalValue;
        this.addInvalidationListener = addInvalidationListener;
        this.removeInvalidationListener = removeInvalidationListener;
        this.disk = disk;
        this.path = path;
        this.member = member;
        this.valueDeseralizationContext = valueDeseralizationContext;
    }

    @Nullable
    @Override
    public IScriptMember getMember(String memberName) {
        Value member = this.graalValue.getMember(memberName);
        return member == null ? null : new GraalScript(this.graalContext, member, this.addInvalidationListener, this.removeInvalidationListener, disk, path, memberName, valueDeseralizationContext);
    }

    @Override
    public void addInvalidationListener(IInvalidateListener listener) {
        this.addInvalidationListener.accept(listener);
    }

    @Override
    public void removeInvalidationListeners() {
        this.removeInvalidationListener.run();
    }

    @Override
    public IValue getValue() throws EvaluationException {
        IEvaluationExceptionFactory exceptionFactory = ScriptHelpers.getEvaluationExceptionFactory(disk, path, member);
        try {
            this.graalContext.resetLimits();
            return ValueTranslators.REGISTRY.translateFromGraal(this.graalContext, graalValue, exceptionFactory, valueDeseralizationContext);
        } catch (PolyglotException e) {
            throw exceptionFactory.createError(e.getMessage());
        }
    }
}
