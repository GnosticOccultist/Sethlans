package fr.sethlans.core.render.vk.util;

import java.util.Iterator;

public interface VkFlag<T extends VkFlag<T>> extends Comparable<T>, Iterable<Integer> {

    static <T extends VkFlag<T>> VkFlag<T> of(int bits) {
        return new VkFlagImpl<>(bits);
    }

    static <T extends VkFlag<T>> VkFlag<T> empty() {
        return of(0);
    }

    @SafeVarargs
    static <T extends VkFlag<T>> VkFlag<T> of(VkFlag<T>... flags) {
        return new VkFlagImpl<>(flags);
    }

    @SafeVarargs
    static <T extends VkFlag<T>> int bitsOf(VkFlag<T>... flags) {
        var result = 0;
        for (var f : flags) {
            result |= f.bits();
        }
        return result;
    }

    int bits();

    default VkFlag<T> add(VkFlag<T> flag) {
        if (!contains(flag)) {
            return new VkFlagImpl<>(bits() | flag.bits());
        }

        return this;
    }

    default boolean contains(VkFlag<T> flag) {
        return contains(flag.bits());
    }

    default boolean contains(int bits) {
        return (bits() & bits) == bits;
    }

    @Override
    default Iterator<Integer> iterator() {
        return new IteratorImpl(bits());
    }

    @Override
    default int compareTo(T o) {
        return Integer.compare(bits(), o.bits());
    }

    class VkFlagImpl<T extends VkFlag<T>> implements VkFlag<T> {

        private final int bits;

        public VkFlagImpl(int bits) {
            this.bits = bits;
        }

        @SafeVarargs
        public VkFlagImpl(VkFlag<T>... flags) {
            this.bits = bitsOf(flags);
        }

        @Override
        public int bits() {
            return bits;
        }
    }

    class IteratorImpl implements Iterator<Integer> {

        private int bits;

        public IteratorImpl(int bits) {
            this.bits = bits;
        }

        @Override
        public boolean hasNext() {
            return bits != 0;
        }

        @Override
        public Integer next() {
            var bit = Integer.lowestOneBit(bits);
            bits &= ~bit;
            return bit;
        }
    }
}
