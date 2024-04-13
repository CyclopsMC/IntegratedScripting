package org.cyclops.integratedscripting.evaluate;

import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Path;

/**
 * @author rubensworks
 */
public class EvaluationExceptionResolutionHelpers {

    // Holds weak references of created EvaluationExceptions
    private static final ReferenceQueue<? super EvaluationException> EVALUATION_EXCEPTION_REFERENCE_QUEUE = new ReferenceQueue<>();

    /**
     * Indicate that the given EvaluationException must be resolved when the given script is changed.
     * @param evaluationException An evaluation exception.
     * @param disk A script disk.
     * @param path A script path.
     * @return The given exception.
     */
    public static EvaluationException resolveOnScriptChange(EvaluationException evaluationException, int disk, Path path) {
        Wrapper<IScriptingData.IDiskScriptsChangeListener> listener = new Wrapper<>();
        listener.set(createListener(new EvaluationExceptionReference(evaluationException, EVALUATION_EXCEPTION_REFERENCE_QUEUE, disk, listener), disk, path));
        ScriptingNetworkHelpers.getScriptingData().addListener(disk, listener.get());
        return evaluationException;
    }

    /**
     * Call this periodically to flush stale entries in
     * {@link EvaluationExceptionResolutionHelpers#EVALUATION_EXCEPTION_REFERENCE_QUEUE}.
     */
    public static void expungeStaleEvaluationExceptions() {
        for (Object x; (x = EVALUATION_EXCEPTION_REFERENCE_QUEUE.poll()) != null; ) {
            ((EvaluationExceptionReference) x).removeListener();
        }
    }

    protected static IScriptingData.IDiskScriptsChangeListener createListener(EvaluationExceptionReference evaluationExceptionReference, int disk, Path path) {
        return scriptPathRelative -> {
            if (scriptPathRelative.equals(path)) {
                EvaluationException exception = evaluationExceptionReference.get();
                if (exception != null) {
                    exception.resolve();
                }
                ScriptingNetworkHelpers.getScriptingData().removeListener(disk, evaluationExceptionReference.listener.get());
            }
        };
    }

    public static class EvaluationExceptionReference extends WeakReference<EvaluationException> {

        private final int disk;
        private final Wrapper<IScriptingData.IDiskScriptsChangeListener> listener;

        public EvaluationExceptionReference(
                EvaluationException referent,
                ReferenceQueue<? super EvaluationException> queue,
                int disk,
                Wrapper<IScriptingData.IDiskScriptsChangeListener> listener
        ) {
            super(referent, queue);
            this.disk = disk;
            this.listener = listener;
        }

        public void removeListener() {
            ScriptingNetworkHelpers.getScriptingData().removeListener(disk, listener.get());
        }
    }

}
