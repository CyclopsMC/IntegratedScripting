package org.cyclops.integratedscripting.core.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.api.network.IScriptingData;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author rubensworks
 */
public class ScriptingData implements IScriptingData {

    public static final LevelResource LEVEL_RESOURCE = new LevelResource("integratedscripting");

    private final Int2ObjectMap<Map<Path, String>> diskScripts = new Int2ObjectAVLTreeMap<>();
    // The file contents as last written or read by this class, to detect changes made outside of it
    private final Map<Integer, Map<Path, String>> knownDiskContents = new ConcurrentHashMap<>();
    private final Path rootPath;
    private final Set<Pair<Integer, Path>> dirtyPaths = Sets.newHashSet();
    private final Int2ObjectMap<List<IDiskScriptsChangeListener>> scriptChangeListeners = new Int2ObjectAVLTreeMap<>();
    // Accessed by both the server thread and the file watcher thread
    private final Map<Path, WatchKey> pathWatchers = new ConcurrentHashMap<>();
    private final Map<WatchKey, Path> pathWatchersReverse = new ConcurrentHashMap<>();

    private boolean initialized = false;
    private WatchService watchService;

    public ScriptingData(Path rootDirectory) {
        this.rootPath = rootDirectory;
    }

    public Path getRootPath() {
        return rootPath;
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
            if (this.watchService != null) {
                this.watchService.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Path getDisksPath() {
        return rootPath.resolve("scripting-disks");
    }

    public Path getDiskPath(int id) {
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

        // Register watcher for all disk directories
        this.registerAbsolutePathWatcher(disksPath);

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

    // Synchronized, as this is also called from the file watcher thread
    protected synchronized void initializeDiskScripts(int diskId) {
        // Watch all directories before reading files, so files created during reading still trigger a reload
        this.registerDiskWatchers(diskId);

        Map<Path, String> scriptsDisk = Maps.newHashMap();
        Path diskDir = getDiskPath(diskId);
        Iterator<File> filesIt = FileUtils.iterateFilesAndDirs(diskDir.toFile(), FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        while (filesIt.hasNext()) {
            File file = filesIt.next();
            if (file.isFile()) {
                Path filePathRelative = diskDir.relativize(file.toPath());
                try {
                    scriptsDisk.put(filePathRelative, FileUtils.readFileToString(file, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Only apply changes made outside of this class.
        // The file watcher may also trigger while scripts are being written here,
        // in which case the disk contents are outdated compared to the scripts in memory.
        Map<Path, String> known = knownDiskContents.computeIfAbsent(diskId, k -> new ConcurrentHashMap<>());
        Map<Path, String> scriptsMemory = diskScripts.get(diskId);
        Map<Path, String> scripts = scriptsMemory == null ? Maps.newHashMap() : Maps.newHashMap(scriptsMemory);
        Set<Path> paths = Sets.newHashSet(scriptsDisk.keySet());
        paths.addAll(known.keySet());
        for (Path path : paths) {
            String scriptDisk = scriptsDisk.get(path);
            if (!Objects.equals(scriptDisk, known.get(path))) {
                if (scriptDisk == null) {
                    scripts.remove(path);
                    known.remove(path);
                } else {
                    scripts.put(path, scriptDisk);
                    known.put(path, scriptDisk);
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
                    if (!poll) {
                        // The directory was deleted
                        Path path = pathWatchersReverse.remove(key);
                        if (path != null) {
                            pathWatchers.remove(path);
                        }
                    }
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
    public synchronized void setScripts(int disk, Map<Path, String> scripts, ChangeLocation changeLocation) {
        // Update script data
        Map<Path, String> oldScripts = diskScripts.put(disk, scripts);

        // Determine scripts that actually changed
        List<Path> modifiedScripts = Lists.newLinkedList();
        Set<Path> keys = Sets.newHashSet();
        keys.addAll(scripts.keySet());
        if (oldScripts != null) {
            keys.addAll(oldScripts.keySet());
        }
        for (Path path : keys) {
            if (oldScripts == null || !Objects.equals(oldScripts.get(path), scripts.get(path))) {
                modifiedScripts.add(path);
            }
        }

        // Propagate changes
        if (changeLocation == ChangeLocation.MEMORY) {
            // Mark dirty
            for (Path path : modifiedScripts) {
                this.markDirty(disk, path);
            }
        }
        if (changeLocation == ChangeLocation.DISK) {
            // Limit length of log files
            if (GeneralConfig.maxLogLines != -1) {
                for (Path modifiedScript : modifiedScripts) {
                    String modifiedScriptStr = modifiedScript.toString();
                    if (modifiedScriptStr.endsWith(".stdout") || modifiedScriptStr.endsWith(".stderr")) {
                        String data = scripts.get(modifiedScript);
                        String[] lines = data.split("\n");
                        if (lines.length > GeneralConfig.maxLogLines + 100) {
                            String dataNew = Arrays.stream(lines).skip(lines.length - GeneralConfig.maxLogLines).collect(Collectors.joining("\n")) + "\n";
                            scripts.put(modifiedScript, dataNew);
                            this.flushScript(disk, modifiedScript);
                        }
                    }
                }
            }

            // Watchers are not re-registered, as events that happen in between would be lost
            this.registerDiskWatchers(disk);
        }

        // Invoke listeners
        List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
        if (listeners != null) {
            for (IDiskScriptsChangeListener listener : Lists.newArrayList(listeners.listIterator())) {
                for (Path scriptPathRelative : modifiedScripts) {
                    listener.onChange(scriptPathRelative);
                }
            }
        }
    }

    boolean isScriptPathSafe(int disk, Path supplied) {
        if (supplied == null || supplied.isAbsolute()) {
            return false;
        }

        Path relative = supplied.normalize();

        if (relative.getNameCount() == 0 || relative.startsWith("..")) {
            return false;
        }

        Path diskRoot = getDiskPath(disk).toAbsolutePath().normalize();
        Path resolved = diskRoot.resolve(relative).normalize();

        return resolved.startsWith(diskRoot);
    }

    @Override
    public synchronized void setScript(int disk, Path scriptPathRelative, @Nullable String script, ChangeLocation changeLocation) {
        // Validate that the path is safe and contained within the disk root
        if (!isScriptPathSafe(disk, scriptPathRelative)) {
            return;
        }

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
            // Register watcher
            this.registerDiskWatchers(disk);
        }

        // Invoke listeners
        List<IDiskScriptsChangeListener> listeners = scriptChangeListeners.get(disk);
        if (listeners != null) {
            for (IDiskScriptsChangeListener listener : new ArrayList<>(listeners)) {
                listener.onChange(scriptPathRelative);
            }
        }
    }

    /**
     * Watch all directories of a disk, including empty ones, so that new files in any of them are noticed.
     */
    protected void registerDiskWatchers(int diskId) {
        Path diskPath = getDiskPath(diskId);
        if (!Files.isDirectory(diskPath)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(diskPath)) {
            paths.filter(Files::isDirectory).forEach(this::registerAbsolutePathWatcher);
        } catch (IOException | UncheckedIOException e) {
            // A directory may have been deleted while walking, which will trigger another reload
        }
    }

    protected void registerPathWatcher(int diskId, @Nullable Path pathRelative) {
        Path diskPath = getDiskPath(diskId);
        Path pathAbsolute = pathRelative == null ? diskPath : diskPath.resolve(pathRelative);
        this.registerAbsolutePathWatcher(pathAbsolute);
    }

    protected void registerAbsolutePathWatcher(Path pathAbsolute) {
        if (!pathWatchers.containsKey(pathAbsolute)) {
            try {
                WatchKey watchKey = pathAbsolute.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                pathWatchers.put(pathAbsolute, watchKey);
                pathWatchersReverse.put(watchKey, pathAbsolute);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    @Override
    public Pair<OutputStream, OutputStream> getOutputStreams(int disk, Path scriptPathRelative) {
        Path scriptPathAbsolute = getDiskPath(disk).resolve(scriptPathRelative);
        return Pair.of(
                new LazyOutputStream(() -> new FileOutputStream(scriptPathAbsolute + ".stdout", true)),
                new LazyOutputStream(() -> new FileOutputStream(scriptPathAbsolute + ".stderr", true))
        );
    }

    /**
     * Write a file without exposing partially written contents to the file watcher.
     * The temporary file is created outside the watched disk directories.
     */
    protected void writeAtomically(Path path, String contents) throws IOException {
        Path tempFile = Files.createTempFile(rootPath, "flush", ".tmp");
        try {
            // Temporary files are only accessible by their owner, so use the permissions of a regular file instead
            if (Files.getFileStore(tempFile).supportsFileAttributeView(PosixFileAttributeView.class)) {
                Files.setPosixFilePermissions(tempFile, Files.exists(path) ? Files.getPosixFilePermissions(path) : PosixFilePermissions.fromString("rw-r--r--"));
            }
            Files.writeString(tempFile, contents, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    protected synchronized void flushScript(int disk, Path scriptPathRelative) {
        Map<Path, String> scripts = getScripts(disk);
        String script = scripts.get(scriptPathRelative);
        Path scriptPathAbsolute = getDiskPath(disk).resolve(scriptPathRelative);
        try {
            Map<Path, String> known = knownDiskContents.computeIfAbsent(disk, k -> new ConcurrentHashMap<>());
            if (script == null) {
                FileUtils.delete(scriptPathAbsolute.toFile());
                known.remove(scriptPathRelative);
            } else {
                scriptPathAbsolute.getParent().toFile().mkdirs();
                writeAtomically(scriptPathAbsolute, script);
                known.put(scriptPathRelative, script);
                // Also watch intermediate directories that were created
                this.registerDiskWatchers(disk);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
