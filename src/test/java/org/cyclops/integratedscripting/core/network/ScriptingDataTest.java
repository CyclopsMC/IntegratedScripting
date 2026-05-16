package org.cyclops.integratedscripting.core.network;

import org.apache.commons.io.FileUtils;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ScriptingDataTest {

    private static final int AWAIT_TIMEOUT_MS = 20000;
    private static final int POLLING_INTERVAL_MS = 25;

    @Test
    public void testExternalUpdatesOnRuntimeCreatedDiskFolderSynced() throws IOException, InterruptedException {
        Path rootPath = Files.createTempDirectory("integratedscripting-test");
        ScriptingData scriptingData = new ScriptingData(rootPath);
        try {
            scriptingData.initialize();
            Path disksPath = rootPath.resolve("scripting-disks");
            awaitCondition(() -> isWatcherRegistered(scriptingData, disksPath));

            Path diskPath = disksPath.resolve("123");
            Files.createDirectories(diskPath);
            awaitCondition(() -> scriptingData.getDisks().contains(123));
            awaitCondition(() -> isWatcherRegistered(scriptingData, diskPath));

            Files.writeString(diskPath.resolve("main.js"), "export const value = 1;");
            awaitCondition(() -> "export const value = 1;".equals(scriptingData.getScripts(123).get(Path.of("main.js"))));

            assertThat(scriptingData.getDisks(), hasItem(123));
            assertThat(scriptingData.getScripts(123).get(Path.of("main.js")), equalTo("export const value = 1;"));
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

            scriptingData.setScript(456, Path.of("main.js"), "export const value = 1;", IScriptingData.ChangeLocation.MEMORY);
            scriptingData.tick();
            assertTrue(Files.exists(diskPath.resolve("main.js")));
            awaitCondition(() -> isWatcherRegistered(scriptingData, diskPath));

            Files.writeString(diskPath.resolve("main.js"), "export const value = 2;");
            awaitCondition(() -> "export const value = 2;".equals(scriptingData.getScripts(456).get(Path.of("main.js"))));

            assertThat(scriptingData.getScripts(456).get(Path.of("main.js")), equalTo("export const value = 2;"));
        } finally {
            scriptingData.close();
            FileUtils.deleteDirectory(rootPath.toFile());
        }
    }

    private static void awaitCondition(Condition condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(POLLING_INTERVAL_MS);
        }
        assertTrue("Timed out waiting for condition", condition.matches());
    }

    private static boolean isWatcherRegistered(ScriptingData scriptingData, Path path) {
        try {
            Field field = ScriptingData.class.getDeclaredField("pathWatchers");
            field.setAccessible(true);
            Map<Path, ?> pathWatchers = (Map<Path, ?>) field.get(scriptingData);
            return pathWatchers.containsKey(path);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches();
    }
}
