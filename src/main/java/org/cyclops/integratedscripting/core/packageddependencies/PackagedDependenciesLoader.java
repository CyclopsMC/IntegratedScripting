package org.cyclops.integratedscripting.core.packageddependencies;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.commons.io.function.IOStream;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.graalvm.polyglot.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads dependencies packaged within this mod jar into a fallback class loader.
 * This ensures that if the dependencies are already on the class path, that they will not be loaded again.
 * This avoid duplicate class loading errors.
 * NeoForge's JarJar doesn't do this deduplication (yet): https://github.com/neoforged/JarJar/issues/5
 * @author rubensworks
 */
public class PackagedDependenciesLoader {
    public static void load() throws IOException {
        // Forcefully make Graal run on unsupported OSes (such as OpenBSD). It will fall back to a slower version.
        System.setProperty("polyglot.engine.allowUnsupportedPlatform", "true");
        System.setProperty("polyglot.engine.userResourceCache", System.getProperty("java.io.tmpdir"));

        // Load libraries
        JsonObject jo;
        Path tmp = FMLLoader.getCurrent().getGameDir().resolve("libraries-integratedscripting");
        Files.createDirectories(tmp);
        Set<Path> outFiles = new HashSet<>();
        try (InputStream is = IntegratedScripting.class.getResourceAsStream("/META-INF/packageddependencies.json")) {
            assert is != null;
            jo = GsonHelper.parse(new InputStreamReader(is));
            for (JsonElement path : jo.get("paths").getAsJsonArray()) {
                // extract to temp dir
                String pathAsString = "/" + path.getAsString();
                Path outFile = tmp.resolve(pathAsString.substring(pathAsString.lastIndexOf('/') + 1));
                outFiles.add(outFile);
                try (InputStream jarStream = IntegratedScripting.class.getResourceAsStream(pathAsString)) {
                    assert jarStream != null;
                    Files.copy(jarStream, outFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        try (Stream<Path> files = Files.list(tmp)) {
            IOStream.adapt(files).filter(file -> !outFiles.contains(file)).forEach(Files::deleteIfExists);
        }

        for (Path outFile : outFiles) {
            UnsafeHelper.addToFallbackClassloader(outFile);
        }

        IntegratedScripting.clog(outFiles.size() + " Packaged dependencies loaded");

        // Pre-load GraalJS fully in the fallback classloader.
        ClassLoader f = UnsafeHelper.makeFallbackClassloader();
        ClassLoader p = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(f);
        try {
            Context.Builder build = Context.newBuilder("js");
            Context con = build.build();
            con.eval("js", "console.log('graaljs has been pre-loaded.')");
            con.close();
        } finally {
            Thread.currentThread().setContextClassLoader(p);
        }
    }
}
