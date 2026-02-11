package fr.sethlans.core.render.vk.util;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public interface VkFlag<T extends VkFlag<T>> extends Comparable<T>, Iterable<Integer> {

    static <T extends VkFlag<T>> VkFlag<T> of(int bits) {
        return new VkFlagImpl<>(bits);
    }
    
    static <V extends Enum<V>, T extends VkFlag<T>> VkFlag<T> of(EnumSet<V> enums, Function<V, T> mapper) {
        int bits = 0;
        for (var e : enums) {
            bits |= mapper.apply(e).bits();
        }
        
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
    
    default VkFlag<T> addIf(boolean b, VkFlag<T> flag) {
        return b ? add(flag) : this;
    }

    default boolean contains(VkFlag<T> flag) {
        return contains(flag.bits());
    }

    default boolean contains(int bits) {
        return (bits() & bits) == bits;
    }
    
    default boolean containedIn(int bits) {
        return (bits & bits()) == bits();
    }
    
    default boolean isEmpty() {
        return bits() == 0;
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

        @Override
        public int hashCode() {
            return Objects.hash(bits);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            
            var other = (VkFlagImpl<?>) obj;
            return bits == other.bits;
        }

        @Override
        public String toString() {
            return "VkFlagImpl [bits=" + bits + "]";
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
