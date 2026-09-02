package org.cyclops.integratedscripting.evaluate.translation;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Breaks down where the cost of a throwing script goes, see issue #67.
 *
 * @author rubensworks
 */
public class Issue67ErrorCostTests {

    private static ValueDeseralizationContext VDC = null;
    private static Context CTX = null;

    @BeforeAll
    public static void beforeAll() throws EvaluationException {
        VDC = ValueDeseralizationContextMocked.get();
        CTX = ScriptHelpers.createPopulatedContext(null, VDC);
    }

    @Test
    public void benchmarkErrorCost() {
        // Throwing and catching entirely within JS.
        Value insideJs = CTX.eval("js", """
                (function () { try { null.foo; } catch (e) { return 1; } })
                """);
        BenchmarkValueTranslators.benchmark("Throw-insideJs", () -> { CTX.resetLimits(); insideJs.execute(); }, 2000, 5000);

        // Throwing across the host boundary, without touching the exception.
        Value toHost = CTX.eval("js", "(function () { null.foo; })");
        BenchmarkValueTranslators.benchmark("Throw-toHost", () -> {
            try {
                CTX.resetLimits();
                toHost.execute();
            } catch (PolyglotException e) {
                // Ignored
            }
        }, 2000, 5000);

        // Throwing across the host boundary, and reading the message, as the mod does.
        BenchmarkValueTranslators.benchmark("Throw-toHost-getMessage", () -> {
            try {
                CTX.resetLimits();
                toHost.execute();
            } catch (PolyglotException e) {
                if (e.getMessage() == null) {
                    throw new IllegalStateException();
                }
            }
        }, 2000, 5000);

        // A successful call of the same shape, as a baseline.
        Value noThrow = CTX.eval("js", "(function () { return 1; })");
        BenchmarkValueTranslators.benchmark("Throw-none", () -> { CTX.resetLimits(); noThrow.execute(); }, 20000, 50000);
    }
}
