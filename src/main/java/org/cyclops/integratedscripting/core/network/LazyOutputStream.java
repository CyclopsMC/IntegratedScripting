package org.cyclops.integratedscripting.core.network;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that delays inner output stream creation until the first byte was written.
 * @author rubensworks
 */
public class LazyOutputStream extends OutputStream {

    private final OutputStreamSupplier outputStreamSupplier;
    @Nullable
    private OutputStream outputStream;

    public LazyOutputStream(OutputStreamSupplier outputStreamSupplier) {
        this.outputStreamSupplier = outputStreamSupplier;
    }

    @Override
    public void write(int b) throws IOException {
        if (outputStream == null) {
            outputStream = outputStreamSupplier.get();
        }
        outputStream.write(b);
    }

    public static interface OutputStreamSupplier {
        public OutputStream get() throws FileNotFoundException;
    }
}
