package fr.sethlans.core.render.buffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class ArenaBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private Arena arena;

    private MemorySegment segment;

    public ArenaBuffer(MemorySize size) {
        this.size = size;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (offset < 0) {
            throw new IllegalArgumentException("The offset can't be negative!");
        }

        if (offset + size > size().getBytes()) {
            throw new IndexOutOfBoundsException(
                    "Mapping out of bounds [" + offset + ", " + size + "], " + size().getBytes() + "!");
        }

        if (segment == null || arena == null) {
            if (ref != null) {
                ref.destroy();
            }

            arena = Arena.ofConfined();
            
            segment = arena.allocate(size().getBytes());
            ref = NativeResource.get().register(this);
        }

        var mapping = offset == 0 ? new DirectBufferMapping(segment)
                : new DirectBufferMapping(segment.asSlice(size().getOffset() + offset, size));
        return mapping;
    }

    @Override
    public void push(long offset, long size) {

    }

    public MemorySegment getSegment() {
        return segment;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            arena.close();
            arena = null;

            segment = null;
        };
    }
}
