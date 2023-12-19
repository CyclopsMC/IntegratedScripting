package org.cyclops.integratedscripting.core.network;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integratedscripting.api.network.IScriptingData;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ScriptingData implements IScriptingData {

    public static final LevelResource LEVEL_RESOURCE = new LevelResource("integratedscripting");

    private final Int2ObjectMap<Map<Path, String>> diskScripts = new Int2ObjectAVLTreeMap<>();
    private final Path rootPath;
    private final Set<Pair<Integer, Path>> dirtyPaths = Sets.newHashSet();
    private final Int2ObjectMap<List<IDiskScriptsChangeListener>> scriptChangeListeners = new Int2ObjectAVLTreeMap<>();
    private final Map<Path, WatchKey> pathWatchers = Maps.newHashMap();
    private final Map<WatchKey, Path> pathWatchersReverse = Maps.newHashMap();

    private boolean initialized = false;
    private WatchService watchService;

    public ScriptingData(Path rootDirectory) {
        this.rootPath = rootDirectory;
    }

    public void tick() {
        // Initialize once
        if (!this.initialized) {
            this.initialized = true;
            this.initialize();
        }

        // Flush changes to disk if needed
        if (!dirtyPaths.isEmpty()) {
            for (Pair<Integer, Path> entry : dirtyPaths) {
                this.flushScript(entry.getLeft(), entry.getRight());
            }
            dirtyPaths.clear();
        }
    }

    public void close() {
        try {
            this.watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Path getDisksPath() {
        return rootPath.resolve("scripting-disks");
    }

    protected Path getDiskPath(int id) {
        return getDisksPath().resolve(String.valueOf(id));
    }

    public void initialize() {
        Path disksPath = getDisksPath();
        disksPath.toFile().mkdirs();

        // Initialize watcher
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Collect all disk ids
        List<Integer> diskIds = Lists.newArrayList();
        try {
            Files.walk(disksPath, 1)
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        try {
                            diskIds.add(Integer.parseInt(name));
                        } catch (NumberFormatException e) {
                            // Ignore invalid disk directories
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read scripts for all disk id's
        for (Integer diskId : diskIds) {
            this.initializeDiskScripts(diskId);
        }

        // Listen to changes
        Thread watchThread = new Thread(this::watchFiles);
        watchThread.start();
    }

    protected void initializeDiskScripts(int diskId) {
        Map<Path, String> scripts = Maps.newHashMap();
        Path diskDir = getDiskPath(diskId);
        Iterator<File> filesIt = FileUtils.iterateFilesAndDirs(diskDir.toFile(), FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        while (filesIt.hasNext()) {
            File file = filesIt.next();
            if (file.isFile()) {
                Path filePathRelative = diskDir.relativize(file.toPath());
                try {
                    scripts.put(filePathRelative, FileUtils.readFileToString(file, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.setScripts(diskId, scripts, ChangeLocation.DISK);
    }

    protected void watchFiles() {
        // Listen to changes
        while (true) {
            try {
                boolean poll = true;
                while (poll) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {

                        // Check if the file is part of a disk directory
                        Path changedPathRelative = (Path) event.context();
                        Path pathDirectory = pathWatchersReverse.get(key);
                        if (pathDirectory != null) {
                            Path changedPathAbsolute = pathDirectory.resolve(changedPathRelative);
                            Path disksPath = getDisksPath();
                            if (changedPathAbsolute.startsWith(disksPath)) {
                                try {
                                    int diskId = Integer.parseInt(changedPathAbsolute.getName(disksPath.getNameCount()).toString());
                                    // Re-initialize the full directory
                                    this.initializeDiskScripts(diskId);
                                } catch (NumberFormatException e) {
                                    // Ignore non-disk directories
                                }
                            }
                        }
                    }
                    poll = key.reset();
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ClosedWatchServiceException e) {
                // Exit the infinite loop
                break;
            }
        }
    }

    @Override
    public Collection<Integer> getDisks() {
        return diskScripts.keySet();
    }

    @Override
    public Map<Path, String> getScripts(int disk) {
        return diskScripts.getOrDefault(disk, Maps.newHashMap());
    }

    @Override
    public void setScripts(int disk, Map<Path, String> scripts, ChangeLocation changeLocation) {
        // Update script data
        Map<Path, String> oldScripts = diskScripts.put(disk, scripts);

        // Propagate changes
        if (changeLocation == ChangeLocation.MEMORY) {
            // Mark dirty
            for (Path path : scripts.keySet()) {
                this.markDirty(disk, path);
            }
        }
        if (changeLocation == ChangeLocation.DISK) {
            // Invoke listeners
            List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
            if (listeners != null) {
                for (IDiskScriptsChangeListener listener : Lists.newArrayList(listeners.listIterator())) {
                    for (Path scriptPathRelative : scripts.keySet()) {
                        listener.onChange(scriptPathRelative);
                    }
                }
            }

            // Unregister old watchers
            if (oldScripts != null) {
                for (Path path : oldScripts.keySet()) {
                    this.unregisterPathWatcher(disk, path.getParent());
                }
            }

            // Register watchers for all directories
            for (Path path : scripts.keySet()) {
                this.registerPathWatcher(disk, path.getParent());
            }
        }
    }

    @Override
    public void setScript(int disk, Path scriptPathRelative, @Nullable String script, ChangeLocation changeLocation) {
        // Update script data
        Map<Path, String> scripts = diskScripts.get(disk);
        if (scripts == null) {
            scripts = Maps.newHashMap();
            diskScripts.put(disk, scripts);
        }
        if (script == null) {
            scripts.remove(scriptPathRelative);
        } else {
            scripts.put(scriptPathRelative, script);
        }

        // Propagate changes
        if (changeLocation == ChangeLocation.MEMORY) {
            this.markDirty(disk, scriptPathRelative);
        }
        if (changeLocation == ChangeLocation.DISK) {
            // Invoke listeners
            List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
            if (listeners != null) {
                for (IDiskScriptsChangeListener listener : listeners) {
                    listener.onChange(scriptPathRelative);
                }
            }

            // Register watcher
            this.registerPathWatcher(disk, scriptPathRelative.getParent());
        }
    }

    protected void registerPathWatcher(int diskId, @Nullable Path pathRelative) {
        Path diskPath = getDiskPath(diskId);
        Path pathAbsolute = pathRelative == null ? diskPath : diskPath.resolve(pathRelative);
        if (!pathWatchers.containsKey(pathAbsolute)) {
            try {
                WatchKey watchKey = pathAbsolute.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                pathWatchers.put(pathAbsolute, watchKey);
                pathWatchersReverse.put(watchKey, pathAbsolute);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void unregisterPathWatcher(int diskId, @Nullable Path pathRelative) {
        Path diskPath = getDiskPath(diskId);
        Path pathAbsolute = pathRelative == null ? diskPath : diskPath.resolve(pathRelative);
        WatchKey watchKey = pathWatchers.get(pathAbsolute);
        if (watchKey != null) {
            pathWatchers.remove(pathAbsolute);
            pathWatchersReverse.remove(watchKey);
            watchKey.cancel();
        }
    }

    @Override
    public void markDirty(int disk, Path scriptPathRelative) {
        dirtyPaths.add(Pair.of(disk, scriptPathRelative));
    }

    @Override
    public void addListener(int disk, IDiskScriptsChangeListener listener) {
        List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
        if (listeners == null) {
            listeners = Lists.newArrayList();
            scriptChangeListeners.put(disk, listeners);
        }
        listeners.add(listener);
    }

    @Override
    public void removeListener(int disk, IDiskScriptsChangeListener listener) {
        List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                scriptChangeListeners.remove(disk);
            }
        }
    }

    protected void flushScript(int disk, Path scriptPathRelative) {
        Map<Path, String> scripts = getScripts(disk);
        String script = scripts.get(scriptPathRelative);
        Path scriptPathAbsolute = getDiskPath(disk).resolve(scriptPathRelative);
        try {
            if (script == null) {
                FileUtils.delete(scriptPathAbsolute.toFile());
            } else {
                scriptPathAbsolute.getParent().toFile().mkdirs();
                FileUtils.write(scriptPathAbsolute.toFile(), script, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
