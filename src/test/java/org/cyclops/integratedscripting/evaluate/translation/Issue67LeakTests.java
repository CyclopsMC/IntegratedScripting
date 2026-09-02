package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.network.chat.Component;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingData;
import org.cyclops.integratedscripting.evaluate.EvaluationExceptionResolutionHelpers;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates that every script error registers a script change listener that is only removed
 * once the exception is garbage collected AND a new exception factory is created, see issue #67.
 *
 * @author rubensworks
 */
public class Issue67LeakTests {

    private static IntegratedScripting instanceOriginal;
    private static ScriptingData scriptingData;

    @BeforeAll
    public static void beforeAll() throws Exception {
        instanceOriginal = IntegratedScripting._instance;
        IntegratedScripting mod = Mockito.mock(IntegratedScripting.class);
        scriptingData = new ScriptingData(Path.of("/tmp/integratedscripting-test"));
        mod.scriptingData = scriptingData;
        IntegratedScripting._instance = mod;
    }

    @AfterAll
    public static void afterAll() {
        IntegratedScripting._instance = instanceOriginal;
    }

    @SuppressWarnings("unchecked")
    private static int countListeners() throws Exception {
        Field field = ScriptingData.class.getDeclaredField("scriptChangeListeners");
        field.setAccessible(true);
        Map<Integer, List<IScriptingData.IDiskScriptsChangeListener>> listeners =
                (Map<Integer, List<IScriptingData.IDiskScriptsChangeListener>>) field.get(scriptingData);
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    @Test
    public void testErrorsRegisterListeners() throws Exception {
        System.out.println("listeners before: " + countListeners());

        // A script variable creates its exception factory once, and reuses it for every evaluation.
        IEvaluationExceptionFactory factory = ScriptHelpers.getEvaluationExceptionFactory(0, Path.of("script.js"), "member");

        // Every failing evaluation goes through the factory.
        for (int i = 0; i < 10000; i++) {
            factory.createError(Component.literal("boom"));
        }
        System.out.println("listeners after 10000 errors: " + countListeners());

        // Even after the exceptions became unreachable and were collected,
        // the listeners stay until an expunge happens, which only happens when a new factory is created.
        System.gc();
        Thread.sleep(200);
        System.out.println("listeners after gc: " + countListeners());

        long start = System.nanoTime();
        EvaluationExceptionResolutionHelpers.expungeStaleEvaluationExceptions();
        long duration = System.nanoTime() - start;
        System.out.println("listeners after expunge: " + countListeners()
                + " (expunge took " + (duration / 1_000_000D) + "ms)");
    }
}
