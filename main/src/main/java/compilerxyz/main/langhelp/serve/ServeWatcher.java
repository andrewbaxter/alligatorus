package compilerxyz.main.langhelp.serve;

import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public final class ServeWatcher implements AutoCloseable {
    private static final long DEBOUNCE_MS = 100;

    public sealed interface Change {
        record Changed(Path path) implements Change {}

        record Deleted(Path path) implements Change {}
    }

    private final Path root;
    private final WatchService service;
    private final Object pendingLock = new Object();
    private final LinkedHashMap<Path, Long> pending = new LinkedHashMap<>();

    private final Thread thread;

    public ServeWatcher(Path root) {
        this.root = root;
        try {
            service = root.getFileSystem().newWatchService();
            registerTree(root);
        } catch (IOException e) {
            throw new RuntimeException("ServeWatcher: cannot watch " + root, e);
        }
        thread = new Thread(
                () -> {
                    while (true) {
                        WatchKey key;
                        try {
                            key = service.take();
                        } catch (InterruptedException | ClosedWatchServiceException e) {
                            return;
                        }
                        var dir = (Path) key.watchable();
                        for (var event : key.pollEvents()) {
                            if (event.kind().name().equals(StandardWatchEventKinds.OVERFLOW.name())) {
                                for (var module : watcherModules()) touch(module);
                                continue;
                            }
                            var path = dir.resolve((Path) event.context());
                            if (event.kind().name().equals(StandardWatchEventKinds.ENTRY_CREATE.name())
                                    && Files.isDirectory(path)) {
                                try {
                                    registerTree(path);
                                } catch (IOException e) {
                                    throw new RuntimeException("ServeWatcher: cannot watch " + path, e);
                                }
                                for (var module : watcherModules()) {
                                    if (module.startsWith(path)) touch(module);
                                }
                                continue;
                            }
                            if (CacheReal.isModulePath(path.toString())) touch(path);
                        }
                        key.reset();
                    }
                },
                "serve-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void registerTree(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (var d : (Iterable<Path>) walk.filter(Files::isDirectory)::iterator) {
                d.register(
                        service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
            }
        }
    }

    public TList<Path> watcherModules() {
        var out = new TList<Path>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (var file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                if (CacheReal.isModulePath(file.toString())) out.add(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("ServeWatcher: cannot list " + root, e);
        }
        return out;
    }

    private void touch(Path path) {
        synchronized (pendingLock) {
            pending.put(path, System.currentTimeMillis());
            pendingLock.notifyAll();
        }
    }

    public TList<Change> watcherDrain(boolean wait) {
        synchronized (pendingLock) {
            while (true) {
                var now = System.currentTimeMillis();
                var out = new TList<Change>();
                long soonest = Long.MAX_VALUE;
                var it = pending.entrySet().iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    var due = e.getValue() + DEBOUNCE_MS;
                    if (due <= now) {
                        out.add(
                                Files.isRegularFile(e.getKey())
                                        ? new Change.Changed(e.getKey())
                                        : new Change.Deleted(e.getKey()));
                        it.remove();
                    } else {
                        soonest = Math.min(soonest, due);
                    }
                }
                if (!out.isEmpty() || !wait) return out;
                try {
                    if (soonest == Long.MAX_VALUE) {
                        pendingLock.wait();
                    } else {
                        pendingLock.wait(Math.max(1, soonest - now));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return out;
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            service.close();
        } catch (IOException e) {
            throw new RuntimeException("ServeWatcher.close", e);
        }
        thread.interrupt();
    }
}
