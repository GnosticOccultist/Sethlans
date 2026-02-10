package fr.sethlans.core.natives.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Cache<K, V> {

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();

    private Function<V, K> keyMapper;

    private long idleTimeout = 2_000 * 1_000_000;

    public Cache(Function<V, K> keyMapper) {
        this.keyMapper = keyMapper;
    }

    @SuppressWarnings("unchecked")
    public <T extends V> T allocate(T element, Runnable build) {
        var key = keyMapper.apply(element);
        Entry<V> e = entries.get(key);
        if (e != null) {
            return (T) e.acquire();
        }

        this.entries.put(key, new Entry<>(element));
        build.run();
        return element;
    }

    public void flush() {
        var current = System.nanoTime();
        this.entries.values().removeIf(e -> e.flush(current, idleTimeout));
    }

    public void clear() {
        this.entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private static class Entry<E> {

        private final E element;
        private long lastUsed = System.nanoTime();

        private Entry(E element) {
            this.element = element;
        }

        public E acquire() {
            this.lastUsed = System.nanoTime();
            return element;
        }

        public boolean flush(long current, long timeout) {
            return current - lastUsed >= timeout;
        }
    }
}
