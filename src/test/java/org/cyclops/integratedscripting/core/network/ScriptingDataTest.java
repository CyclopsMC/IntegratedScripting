package org.cyclops.integratedscripting.core.network;

import org.apache.commons.io.FileUtils;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScriptingDataTest {

    @Test
    public void testIsScriptPathSafeAcceptsSimpleName() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertTrue(scriptingData.isScriptPathSafe(1, Path.of("main.js")));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testIsScriptPathSafeAcceptsSubdirectory() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertTrue(scriptingData.isScriptPathSafe(1, Path.of("subdir/main.js")));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testIsScriptPathSafeRejectsNull() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertFalse(scriptingData.isScriptPathSafe(1, null));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testIsScriptPathSafeRejectsAbsolute() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertFalse(scriptingData.isScriptPathSafe(1, Path.of("/etc/passwd")));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testIsScriptPathSafeRejectsParentTraversal() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertFalse(scriptingData.isScriptPathSafe(1, Path.of("../escape.txt")));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testIsScriptPathSafeRejectsNestedTraversal() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            assertFalse(scriptingData.isScriptPathSafe(1, Path.of("subdir/../../escape.txt")));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testSetScriptIgnoresTraversalPath() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.setScript(1, Path.of("../escape.txt"), "bad content", IScriptingData.ChangeLocation.MEMORY);
            assertThat(scriptingData.getScripts(1).get(Path.of("../escape.txt")), nullValue());
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testSetScriptIgnoresAbsolutePath() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            Path outsidePath = rootPath.resolve("outside.txt").toAbsolutePath();
            scriptingData.setScript(1, outsidePath, "bad content", IScriptingData.ChangeLocation.MEMORY);
            assertFalse(Files.exists(outsidePath));
        } finally {
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testSetScriptAcceptsValidPath() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            Path scriptPath = Path.of("main.js");
            scriptingData.setScript(1, scriptPath, "export const x = 1;", IScriptingData.ChangeLocation.MEMORY);
            assertThat(scriptingData.getScripts(1).get(scriptPath), equalTo("export const x = 1;"));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    private static final int AWAIT_TIMEOUT_MS = 20000;
    private static final int ATTEMPT_TIMEOUT_MS = 750;
    private static final int POLLING_INTERVAL_MS = 25;

    @Test
    public void testExternalUpdatesOnRuntimeCreatedDiskFolderSynced() throws IOException, InterruptedException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            Path diskPath = rootPath.resolve("scripting-disks").resolve("123");
            Files.createDirectories(diskPath);
            awaitCondition(() -> scriptingData.getDisks().contains(123));

            Path scriptPath = Path.of("main.js");
            int observedValue = writeScriptValueUntilObserved(
                    scriptingData, 123, diskPath.resolve(scriptPath), scriptPath, 1);
            assertThat(scriptingData.getDisks(), hasItem(123));
            assertThat(scriptingData.getScripts(123).get(scriptPath), equalTo(scriptValue(observedValue)));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testExternalUpdatesOnRuntimeFirstUsedDiskSynced() throws IOException, InterruptedException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            Path diskPath = rootPath.resolve("scripting-disks").resolve("456");
            Path scriptPath = Path.of("main.js");

            scriptingData.setScript(456, scriptPath, scriptValue(1), IScriptingData.ChangeLocation.MEMORY);
            scriptingData.tick();
            assertTrue(Files.exists(diskPath.resolve(scriptPath)));

            int observedValue = writeScriptValueUntilObserved(
                    scriptingData, 456, diskPath.resolve(scriptPath), scriptPath, 2);

            assertThat(scriptingData.getScripts(456).get(scriptPath), equalTo(scriptValue(observedValue)));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    private static void awaitCondition(Condition condition) throws InterruptedException {
        assertTrue(awaitCondition(condition, AWAIT_TIMEOUT_MS), "Timed out waiting for condition");
    }

    private static boolean awaitCondition(Condition condition, int timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.matches()) {
                return true;
            }
            Thread.sleep(POLLING_INTERVAL_MS);
        }
        return condition.matches();
    }

    private static int writeScriptValueUntilObserved(ScriptingData scriptingData, int disk, Path scriptPathAbsolute,
                                                     Path scriptPathRelative, int startValue) throws IOException, InterruptedException {
        int value = startValue;
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String expectedScriptValue = scriptValue(value);
            Files.writeString(scriptPathAbsolute, expectedScriptValue);
            if (awaitCondition(() -> expectedScriptValue.equals(scriptingData.getScripts(disk).get(scriptPathRelative)),
                    ATTEMPT_TIMEOUT_MS)) {
                return value;
            }
            value++;
        }
        throw new AssertionError("Timed out waiting for script update after repeated writes");
    }

    private static String scriptValue(int value) {
        return "export const value = " + value + ";";
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches();
    }
}
