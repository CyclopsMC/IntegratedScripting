package org.cyclops.integratedscripting.core.network;

import org.apache.commons.io.FileUtils;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testFlushDoesNotExposePartialContents() throws IOException, InterruptedException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            Path scriptPath = Path.of("main.js");
            String body = "x".repeat(1_000_000);

            // The file watcher reloads the disk after each flush, which must never see a partially written file
            for (int i = 0; i < 20; i++) {
                scriptingData.setScript(789, scriptPath, body + i + "//end", IScriptingData.ChangeLocation.MEMORY);
                scriptingData.tick();
                long deadline = System.currentTimeMillis() + 100;
                while (System.currentTimeMillis() < deadline) {
                    String script = scriptingData.getScripts(789).get(scriptPath);
                    assertTrue("Observed partial script contents", script != null && script.endsWith("//end"));
                }
            }

            try (var files = Files.list(rootPath)) {
                assertTrue("Temporary files were left behind", files.noneMatch(f -> f.toString().endsWith(".tmp")));
            }
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testFlushKeepsFilePermissions() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            org.junit.Assume.assumeTrue(Files.getFileStore(rootPath).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class));
            Path scriptPath = Path.of("main.js");
            Path scriptPathAbsolute = rootPath.resolve("scripting-disks").resolve("321").resolve(scriptPath);

            // New files are readable by others
            scriptingData.setScript(321, scriptPath, scriptValue(1), IScriptingData.ChangeLocation.MEMORY);
            scriptingData.tick();
            assertThat(java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(scriptPathAbsolute)), equalTo("rw-r--r--"));

            // Existing permissions are kept
            Files.setPosixFilePermissions(scriptPathAbsolute, java.nio.file.attribute.PosixFilePermissions.fromString("rw-rw-r--"));
            scriptingData.setScript(321, scriptPath, scriptValue(2), IScriptingData.ChangeLocation.MEMORY);
            scriptingData.tick();
            assertThat(java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(scriptPathAbsolute)), equalTo("rw-rw-r--"));
            assertThat(Files.readString(scriptPathAbsolute), equalTo(scriptValue(2)));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testDiskReloadKeepsUnflushedChanges() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            Path diskPath = rootPath.resolve("scripting-disks").resolve("654");
            Files.createDirectories(diskPath);
            Files.writeString(diskPath.resolve("old.js"), "old");
            Files.writeString(diskPath.resolve("other.js"), "other");
            scriptingData.initialize();

            // Rename old.js to new.js in memory, without flushing it to disk yet
            scriptingData.setScript(654, Path.of("new.js"), "old", IScriptingData.ChangeLocation.MEMORY);
            scriptingData.setScript(654, Path.of("old.js"), null, IScriptingData.ChangeLocation.MEMORY);

            // A file watcher reload, for example caused by another file, must not undo these changes
            scriptingData.initializeDiskScripts(654);
            assertThat(scriptingData.getScripts(654).get(Path.of("new.js")), equalTo("old"));
            assertThat(scriptingData.getScripts(654).get(Path.of("old.js")), nullValue());
            assertThat(scriptingData.getScripts(654).get(Path.of("other.js")), equalTo("other"));

            // After flushing, the disk contents are used again
            scriptingData.tick();
            scriptingData.initializeDiskScripts(654);
            assertThat(scriptingData.getScripts(654).get(Path.of("new.js")), equalTo("old"));
            assertFalse(Files.exists(diskPath.resolve("old.js")));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testDiskReloadDuringFirstFlushKeepsScript() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            scriptingData.setScript(777, Path.of("main.js"), "value", IScriptingData.ChangeLocation.MEMORY);

            // The file watcher reloads the disk once its directory is created, possibly before the script is written
            Files.createDirectories(rootPath.resolve("scripting-disks").resolve("777"));
            scriptingData.initializeDiskScripts(777);

            assertThat(scriptingData.getScripts(777).get(Path.of("main.js")), equalTo("value"));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    @Test
    public void testDiskReloadAppliesExternalChanges() throws IOException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            Path diskPath = rootPath.resolve("scripting-disks").resolve("778");
            Files.createDirectories(diskPath);
            Files.writeString(diskPath.resolve("a.js"), "a1");
            Files.writeString(diskPath.resolve("b.js"), "b1");
            scriptingData.initialize();
            assertThat(scriptingData.getScripts(778).get(Path.of("a.js")), equalTo("a1"));

            // Externally modify one file and delete another
            Files.writeString(diskPath.resolve("a.js"), "a2");
            Files.delete(diskPath.resolve("b.js"));
            scriptingData.initializeDiskScripts(778);

            assertThat(scriptingData.getScripts(778).get(Path.of("a.js")), equalTo("a2"));
            assertThat(scriptingData.getScripts(778).get(Path.of("b.js")), nullValue());
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    private static void awaitCondition(Condition condition) throws InterruptedException {
        assertTrue("Timed out waiting for condition", awaitCondition(condition, AWAIT_TIMEOUT_MS));
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
