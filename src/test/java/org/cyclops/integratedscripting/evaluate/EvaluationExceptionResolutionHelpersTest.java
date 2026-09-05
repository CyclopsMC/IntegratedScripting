package org.cyclops.integratedscripting.evaluate;

import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author rubensworks
 */
public class EvaluationExceptionResolutionHelpersTest {

    private static final Path PATH = Path.of("script.js");
    private static final Path PATH_OTHER = Path.of("other.js");

    // Registered listeners outlive a test, so every test uses its own disks.
    private static final AtomicInteger NEXT_DISK = new AtomicInteger();

    private int disk;
    private IntegratedScripting instanceOriginal;
    private List<IScriptingData.IDiskScriptsChangeListener> listeners;

    @BeforeEach
    public void before() {
        this.disk = NEXT_DISK.getAndAdd(2);
        this.listeners = Lists.newArrayList();

        ScriptingData scriptingData = Mockito.mock(ScriptingData.class);
        Mockito.doAnswer(invocation -> this.listeners.add(invocation.getArgument(1)))
                .when(scriptingData).addListener(Mockito.anyInt(), Mockito.any());
        Mockito.doAnswer(invocation -> this.listeners.remove(invocation.getArgument(1)))
                .when(scriptingData).removeListener(Mockito.anyInt(), Mockito.any());

        IntegratedScripting mod = Mockito.mock(IntegratedScripting.class);
        mod.scriptingData = scriptingData;
        this.instanceOriginal = IntegratedScripting._instance;
        IntegratedScripting._instance = mod;
    }

    @AfterEach
    public void after() {
        IntegratedScripting._instance = this.instanceOriginal;
    }

    @Test
    public void testSingleListenerForManyExceptions() {
        IEvaluationExceptionFactory factory = ScriptHelpers.getEvaluationExceptionFactory(this.disk, PATH, "member");

        // Hold on to the exceptions, so that they can not be collected during this test.
        List<EvaluationException> exceptions = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            exceptions.add(factory.createError(Component.literal("error " + i)));
        }

        assertThat(this.listeners.size(), is(1));
    }

    @Test
    public void testListenerPerScript() {
        List<EvaluationException> exceptions = Lists.newArrayList();
        for (int i = 0; i < 2; i++) {
            exceptions.add(ScriptHelpers.getEvaluationExceptionFactory(this.disk, PATH, "member")
                    .createError(Component.literal("a")));
            exceptions.add(ScriptHelpers.getEvaluationExceptionFactory(this.disk, PATH_OTHER, "member")
                    .createError(Component.literal("b")));
            exceptions.add(ScriptHelpers.getEvaluationExceptionFactory(this.disk + 1, PATH, "member")
                    .createError(Component.literal("c")));
        }

        assertThat(this.listeners.size(), is(3));
    }

    @Test
    public void testExceptionsAreResolvedOnScriptChange() {
        IEvaluationExceptionFactory factory = ScriptHelpers.getEvaluationExceptionFactory(this.disk, PATH, "member");
        List<EvaluationException> exceptions = Lists.newArrayList();
        List<Boolean> resolved = Lists.newArrayList();
        for (int i = 0; i < 3; i++) {
            EvaluationException exception = factory.createError(Component.literal("error " + i));
            int index = i;
            resolved.add(false);
            exception.addResolutionListeners(() -> resolved.set(index, true));
            exceptions.add(exception);
        }

        // Changes to other scripts don't resolve anything.
        List<IScriptingData.IDiskScriptsChangeListener> listenersBefore = Lists.newArrayList(this.listeners);
        listenersBefore.forEach(listener -> listener.onChange(PATH_OTHER));
        assertThat(resolved, is(Lists.newArrayList(false, false, false)));

        listenersBefore.forEach(listener -> listener.onChange(PATH));
        assertThat(resolved, is(Lists.newArrayList(true, true, true)));

        // The listeners are not needed anymore once the script changed.
        assertThat(this.listeners.isEmpty(), is(true));
    }

    @Test
    public void testListenersAreRemovedAfterExceptionsAreCollected() throws InterruptedException {
        IEvaluationExceptionFactory factory = ScriptHelpers.getEvaluationExceptionFactory(this.disk, PATH, "member");
        for (int i = 0; i < 100; i++) {
            factory.createError(Component.literal("error " + i));
        }
        assertThat(this.listeners.size(), is(1));

        for (int i = 0; i < 20 && !this.listeners.isEmpty(); i++) {
            System.gc();
            Thread.sleep(50);
            EvaluationExceptionResolutionHelpers.expungeStaleEvaluationExceptions();
        }
        assertThat(this.listeners.isEmpty(), is(true));
    }
}
