package fr.sethlans.core.render.buffer;

import fr.sethlans.core.natives.NativeResource;

public interface NativeBuffer extends NativeResource<Long> {

    BufferMapping map(long offset, long size);

    void push(long offset, long size);

    default BufferMapping map(long offset) {
        return map(offset, size().getBytes() - offset);
    }

    default BufferMapping map() {
        return map(0, size().getBytes());
    }

    default void push(long offset) {
        push(offset, size().getBytes() - offset);
    }

    default void push() {
        push(0, size().getBytes());
    }

    void unmap();

    MemorySize size();
}
