package pl.edu.mimuw.kd209238.indekser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class WatchDir {

    private static Logger logger = LoggerFactory.getLogger(WatchDir.class);

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private boolean trace = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }


    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                logger.info("register: {}", dir);
            } else {
                if (!dir.equals(prev)) {
                    logger.info("update: {} -> {}", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault()
                .newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = true;

        registerAll(dir);

        // enable trace after initial registration
        this.trace = true;
    }

    WatchDir(String[] paths) throws IOException {
        this.watcher = FileSystems.getDefault()
                .newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = true;
        for (int i = 0; i < paths.length; i++) {
            registerAll(Paths.get(paths[i]));
        }


        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                logger.warn("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                String indexPath = System.getProperty("user.home") + "\\.index";
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                //todo WAŻNE
                logger.info("{}: {}", event.kind().name(), child);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                try {
                    if (kind == ENTRY_CREATE) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                            IndexFiles.indexDocs(child, indexPath);
                        }
                        else {
                            IndexFiles.indexDoc(child, indexPath);
                        }
                    }
                    else if (kind == ENTRY_DELETE) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            IndexFiles.removeDocs(child, indexPath);
                            //todo co jak usuwany jest katalog zapisany?
                            //todo w ogóle co robić jak usuwasz folder
                        }
                        else {
                            IndexFiles.removeDoc(child, indexPath);
                        }
                    }
                    else if (kind == ENTRY_MODIFY) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS))
                            IndexFiles.indexDoc(child, indexPath);
                        //todo albo modyfikujesz
                        else
                            IndexFiles.indexDocs(child, indexPath);
                    }
                }
                catch (IOException ignore) {

                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public static void glowna() throws IOException {

        Runtime.getRuntime()
                .addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        logger.info("Exiting...");
                    }
                });
        String[] dirs = IndexFiles.getPaths();
        WatchDir watchDir = new WatchDir(dirs);
        watchDir.processEvents();
    }

}
