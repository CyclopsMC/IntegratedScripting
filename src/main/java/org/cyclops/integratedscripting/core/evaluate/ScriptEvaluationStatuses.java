package org.cyclops.integratedscripting.core.evaluate;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.core.network.ScriptingData;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last evaluation results of script members, so they can be shown while editing.
 * @author rubensworks
 */
public class ScriptEvaluationStatuses {

    private static final int MAX_VALUE_LENGTH = 100;
    private static final Map<Triple<Integer, Path, String>, Status> STATUSES = new ConcurrentHashMap<>();

    public static void recordValue(int disk, Path path, String member, IValue value) {
        Status status = getOrCreate(disk, path, member);
        status.value = value;
        status.readError = null;
    }

    public static void recordReadError(int disk, Path path, String member, MutableComponent error) {
        Status status = getOrCreate(disk, path, member);
        status.value = null;
        status.readError = error;
        // Read errors are also created by the exception factory, so don't show them twice
        status.callError = null;
    }

    public static void recordCallError(int disk, Path path, String member, MutableComponent error) {
        getOrCreate(disk, path, member).callError = error;
    }

    private static Status getOrCreate(int disk, Path path, String member) {
        String script = getScript(disk, path);
        Status status = STATUSES.get(Triple.of(disk, path, member));
        if (status == null || !Objects.equals(status.script, script)) {
            status = new Status(script);
            STATUSES.put(Triple.of(disk, path, member), status);
        }
        return status;
    }

    @Nullable
    private static String getScript(int disk, Path path) {
        // Scripting data is not available outside of a running server, such as in unit tests
        ScriptingData scriptingData = IntegratedScripting._instance == null ? null : IntegratedScripting._instance.scriptingData;
        return scriptingData == null ? null : scriptingData.getScripts(disk).get(path);
    }

    /**
     * @return Lines describing the last evaluation of the member since its script last changed.
     */
    public static List<MutableComponent> describe(int disk, Path path, String member) {
        Status status = STATUSES.get(Triple.of(disk, path, member));
        List<MutableComponent> lines = Lists.newArrayList();
        if (status == null || !Objects.equals(status.script, getScript(disk, path)) || (status.value == null && status.readError == null)) {
            lines.add(Component.translatable("gui.integratedscripting.evaluation.none").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (status.readError != null) {
            lines.add(Component.translatable("gui.integratedscripting.evaluation.error", status.readError.copy()).withStyle(ChatFormatting.RED));
        } else {
            String value = status.value.getType().toCompactString(status.value).getString();
            if (value.length() > MAX_VALUE_LENGTH) {
                value = value.substring(0, MAX_VALUE_LENGTH) + "…";
            }
            lines.add(Component.translatable("gui.integratedscripting.evaluation.value", Component.literal(value).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }
        if (status.callError != null) {
            lines.add(Component.translatable("gui.integratedscripting.evaluation.call_error", status.callError.copy()).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    public static void reset() {
        STATUSES.clear();
    }

    private static class Status {
        @Nullable
        private final String script;
        @Nullable
        private IValue value;
        @Nullable
        private MutableComponent readError;
        @Nullable
        private MutableComponent callError;

        private Status(@Nullable String script) {
            this.script = script;
        }
    }

}
