package fr.sethlans.core.natives.cache;

import org.lwjgl.system.MemoryStack;

public abstract class CacheableNativeBuilder<K, T, V extends T> {

    protected Cache<K, T> cache;

    public V build() {
        var obj = getBuildTarget();
        try (var stack = MemoryStack.stackPush()) {
            if (cache != null) {
                obj = cache.allocate(obj, () -> construct(stack));
            } else {
                construct(stack);
            }
        }
        return obj;
    }

    protected abstract void construct(MemoryStack stack);

    protected abstract V getBuildTarget();

    public Cache<K, T> getCache() {
        return cache;
    }

    public void setCache(Cache<K, T> cache) {
        this.cache = cache;
    }
}
