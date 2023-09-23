package org.cyclops.integratedscripting.evaluate;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.util.Map;

/**
 * @author rubensworks
 */
public class ScriptHelpers {

    public static Context createBaseContext() {
        Engine engine = Engine
                .newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        return Context
                .newBuilder()
                .engine(engine)
                .allowAllAccess(true)
                .build();
    }

    public static Context createPopulatedContext() throws EvaluationException {
        Context context = createBaseContext();

        // Create idContext field with ops
        Value idContext = context.eval("js", "new Object()");
        Value ops = context.eval("js", "new Object()");
        for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getGlobalInteractOperators().entrySet()) {
            ops.putMember(entry.getKey(), ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeOperator.ValueOperator.of(entry.getValue())));
        }
        idContext.putMember("ops", ops);
        context.getBindings("js").putMember("idContext", idContext);

        return context;
    }

}
