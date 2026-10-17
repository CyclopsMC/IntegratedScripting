package org.cyclops.integratedscripting.network.packet;

import org.junit.Test;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TerminalScriptingModifiedScriptPacketTest {

    private static String roundTrip(String script) {
        return new TerminalScriptingModifiedScriptPacket(0, Path.of("script0.js"), script).getScript();
    }

    @Test
    public void testSingleLine() {
        assertThat(roundTrip("const a = 1;"), equalTo("const a = 1;"));
    }

    @Test
    public void testEmpty() {
        assertThat(roundTrip(""), equalTo(""));
    }

    @Test
    public void testTrailingNewlines() {
        assertThat(roundTrip("const a = 1;\n\n"), equalTo("const a = 1;\n\n"));
    }

    @Test
    public void testOnlyNewlines() {
        assertThat(roundTrip("\n\n\n"), equalTo("\n\n\n"));
    }

    @Test
    public void testLeadingAndInnerNewlines() {
        assertThat(roundTrip("\na\n\nb"), equalTo("\na\n\nb"));
    }

}
