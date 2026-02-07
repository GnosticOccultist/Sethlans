package fr.sethlans.core.natives;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;

public class NativeResourceCleaner implements NativeResourceManager {

    /**
     * The logger for the native resources.
     */
    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.natives");

    private static NativeResourceManager instance = new NativeResourceCleaner();

    public static NativeResourceManager getInstance() {
        return instance;
    }

    public static void setInstance(NativeResourceManager instance) {
        NativeResourceCleaner.instance = instance;
    }

    /**
     * The cleaner used to clean objects when references are reclaimed.
     */
    private final Cleaner cleaner = Cleaner.create();

    private final Map<Long, CleanableNativeRef> refMap = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0L);

    @Override
    public NativeReference register(NativeResource<?> resource) {
        logger.debug("Registered " + resource + ".");

        var cleanable = cleaner.register(resource, resource.createDestroyAction());
        var ref = new CleanableNativeRef(idGenerator.getAndIncrement(), cleanable);
        refMap.put(ref.id, ref);

        return ref;
    }

    @Override
    public void clear() {
        for (var ref : refMap.values()) {
            ref.destroy();
        }

        refMap.clear();
    }

    public class CleanableNativeRef implements NativeReference {

        private final long id;
        private final Cleanable cleanable;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final Collection<NativeReference> dependents = new ArrayList<>();

        CleanableNativeRef(long id, Cleanable cleanable) {
            this.id = id;
            this.cleanable = cleanable;
        }

        @Override
        public void addDependent(NativeReference reference) {
            if (isDestroyed()) {
                throw new IllegalStateException(
                        "Can't add a dependent to a destroyed resource (ref= " + reference + ").");
            }

            this.dependents.add(reference);
        }

        @Override
        public boolean isDestroyed() {
            return !active.get();
        }

        @Override
        public void destroy() {
            if (active.getAndSet(false)) {
                for (NativeReference ref : dependents) {
                    ref.destroy();
                }

                dependents.clear();
                cleanable.clean();
            }
        }

        @Override
        public String toString() {
            return "CleanableNativeRef [id=" + id + ", cleanable=" + cleanable + ", active=" + active + ", dependents="
                    + dependents + "]";
        }
    }
}
