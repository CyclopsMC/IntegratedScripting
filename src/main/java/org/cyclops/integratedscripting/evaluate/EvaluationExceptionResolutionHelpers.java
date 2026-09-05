package org.cyclops.integratedscripting.evaluate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author rubensworks
 */
public class EvaluationExceptionResolutionHelpers {

    // Holds weak references of created EvaluationExceptions
    private static final ReferenceQueue<? super EvaluationException> EVALUATION_EXCEPTION_REFERENCE_QUEUE = new ReferenceQueue<>();

    // Holds one script change listener per script, no matter how many exceptions must be resolved for it.
    private static final Map<Pair<Integer, Path>, ScriptExceptionsListener> LISTENERS = Maps.newHashMap();

    /**
     * Indicate that the given EvaluationException must be resolved when the given script is changed.
     * @param evaluationException An evaluation exception.
     * @param disk A script disk.
     * @param path A script path.
     * @return The given exception.
     */
    public static synchronized EvaluationException resolveOnScriptChange(EvaluationException evaluationException, int disk, Path path) {
        // Drop the exceptions that were collected since the last call,
        // so that scripts that keep failing don't accumulate them.
        expungeStaleEvaluationExceptions();

        Pair<Integer, Path> key = Pair.of(disk, path);
        ScriptExceptionsListener listener = LISTENERS.get(key);
        if (listener == null) {
            listener = new ScriptExceptionsListener(key);
            LISTENERS.put(key, listener);
            ScriptingNetworkHelpers.getScriptingData().addListener(disk, listener);
        }
        listener.addException(new EvaluationExceptionReference(evaluationException,
                EVALUATION_EXCEPTION_REFERENCE_QUEUE, listener));

        return evaluationException;
    }

    /**
     * Call this periodically to flush stale entries in
     * {@link EvaluationExceptionResolutionHelpers#EVALUATION_EXCEPTION_REFERENCE_QUEUE}.
     */
    public static synchronized void expungeStaleEvaluationExceptions() {
        for (Object x; (x = EVALUATION_EXCEPTION_REFERENCE_QUEUE.poll()) != null; ) {
            EvaluationExceptionReference reference = (EvaluationExceptionReference) x;
            reference.getListener().removeException(reference);
        }
    }

    /**
     * Forget all pending exceptions and listeners.
     * This must be called when the scripting data these listeners were registered on goes away.
     */
    public static synchronized void reset() {
        LISTENERS.clear();
        while (EVALUATION_EXCEPTION_REFERENCE_QUEUE.poll() != null) {
            // Drop all pending references
        }
    }

    protected static synchronized void removeListener(ScriptExceptionsListener listener) {
        if (LISTENERS.remove(listener.getKey()) != null) {
            ScriptingNetworkHelpers.getScriptingData().removeListener(listener.getKey().getLeft(), listener);
        }
    }

    /**
     * Resolves all exceptions that were created for one script once that script changes.
     */
    public static class ScriptExceptionsListener implements IScriptingData.IDiskScriptsChangeListener {

        private final Pair<Integer, Path> key;
        private final Set<EvaluationExceptionReference> exceptions = Sets.newHashSet();

        public ScriptExceptionsListener(Pair<Integer, Path> key) {
            this.key = key;
        }

        public Pair<Integer, Path> getKey() {
            return key;
        }

        public void addException(EvaluationExceptionReference reference) {
            this.exceptions.add(reference);
        }

        public void removeException(EvaluationExceptionReference reference) {
            if (this.exceptions.remove(reference) && this.exceptions.isEmpty()) {
                EvaluationExceptionResolutionHelpers.removeListener(this);
            }
        }

        @Override
        public void onChange(Path scriptPathRelative) {
            if (scriptPathRelative.equals(this.key.getRight())) {
                Collection<EvaluationExceptionReference> references = Lists.newArrayList(this.exceptions);
                this.exceptions.clear();
                EvaluationExceptionResolutionHelpers.removeListener(this);
                for (EvaluationExceptionReference reference : references) {
                    EvaluationException exception = reference.get();
                    if (exception != null) {
                        exception.resolve();
                    }
                }
            }
        }
    }

    public static class EvaluationExceptionReference extends WeakReference<EvaluationException> {

        private final ScriptExceptionsListener listener;

        public EvaluationExceptionReference(
                EvaluationException referent,
                ReferenceQueue<? super EvaluationException> queue,
                ScriptExceptionsListener listener
        ) {
            super(referent, queue);
            this.listener = listener;
        }

        public ScriptExceptionsListener getListener() {
            return listener;
        }
    }

}
