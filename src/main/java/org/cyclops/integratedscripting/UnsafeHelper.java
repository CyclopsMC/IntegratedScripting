package org.cyclops.integratedscripting;

import net.neoforged.fml.classloading.ModuleClassLoader;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Wagyourtail
 */
public class UnsafeHelper {
    private static final Unsafe UNSAFE = getUnsafe();
    private static final MethodHandles.Lookup IMPL_LOOKUP = UnsafeHelper.getImplLookup();

    private static final MethodHandle getFallbackClassLoader;
    private static final MethodHandle setFallbackClassLoader;

    static {
        try {
            getFallbackClassLoader = IMPL_LOOKUP.findGetter(ModuleClassLoader.class, "fallbackClassLoader", ClassLoader.class);
            setFallbackClassLoader = IMPL_LOOKUP.findSetter(ModuleClassLoader.class, "fallbackClassLoader", ClassLoader.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new UnsupportedOperationException("Unable to get Unsafe instance", e);
        }
    }

    private static MethodHandles.Lookup getImplLookup() {
        try {
            // ensure lookup is initialized
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            // get the impl_lookup field
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Unsafe unsafe = getUnsafe();
            MethodHandles.Lookup IMPL_LOOKUP;
            IMPL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(implLookupField));
            if (IMPL_LOOKUP != null) return IMPL_LOOKUP;
            throw new NullPointerException();
        } catch (Throwable e) {
            try {
                // try to create a new lookup
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                return constructor.newInstance(Object.class, -1);
            } catch (Throwable e2) {
                e.addSuppressed(e2);
            }
            throw new UnsupportedOperationException("Unable to get MethodHandles.Lookup.IMPL_LOOKUP", e);
        }
    }

    public static void addToFallbackClassloader(Path path) {
        try {
            UnsafeFallbackClassLoader u = makeFallbackClassloader();
            u.addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static UnsafeFallbackClassLoader makeFallbackClassloader() {
        try {
            List<ClassLoader> loaders = new ArrayList<>();
            ClassLoader l = UnsafeHelper.class.getClassLoader();
            if (!(l instanceof ModuleClassLoader)) {
                return new UnsafeFallbackClassLoader(l);
            }
            while (l instanceof ModuleClassLoader) {
                loaders.add(l);
                l = (ClassLoader) getFallbackClassLoader.invokeExact((ModuleClassLoader) l);
            }
            if (l instanceof UnsafeFallbackClassLoader) {
                return (UnsafeFallbackClassLoader) l;
            }
            UnsafeFallbackClassLoader u = new UnsafeFallbackClassLoader(l);
            setFallbackClassLoader.invokeExact((ModuleClassLoader) loaders.getLast(), (ClassLoader) u);
            return u;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static class UnsafeFallbackClassLoader extends URLClassLoader {
        public UnsafeFallbackClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

    }

}
