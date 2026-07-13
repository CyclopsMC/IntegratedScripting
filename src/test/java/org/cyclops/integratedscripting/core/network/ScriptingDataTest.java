package org.cyclops.integratedscripting.core.network;

import org.apache.commons.io.FileUtils;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
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
}
