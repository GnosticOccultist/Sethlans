package fr.sethlans.core.render.buffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class ArenaBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private Arena arena = Arena.ofConfined();

    private final PointerBuffer pAddress = MemoryUtil.memCallocPointer(1);

    private MemorySegment segment;

    private long baseAddress = MemoryUtil.NULL;

    private int lastMappedOffset = -1;

    public ArenaBuffer(MemorySize size) {
        this.size = size;
        this.arena = Arena.ofConfined();
        this.segment = arena.allocate(size.getBytes());
        this.baseAddress = segment.address();

        this.object = baseAddress;
        this.ref = NativeResource.get().register(this);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (offset < 0) {
            throw new IllegalArgumentException("The offset can't be negative!");
        }

        if (offset + size > size().getBytes()) {
            throw new IndexOutOfBoundsException(
                    "Mapping out of bounds [" + offset + ", " + size + "], " + size().getBytes() + "!");
        }

        if (offset != lastMappedOffset) {
            if (offset == 0) {
                pAddress.put(0, baseAddress);

            } else {
                var slice = segment.asSlice(offset, size);
                var address = slice.address();
                pAddress.put(0, MemoryUtil.memPointerBuffer(address, size));
            }
            lastMappedOffset = offset;
        }

        return pAddress;
    }

    @Override
    public void unmap() {

    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            arena.close();
            MemoryUtil.memFree(pAddress);
        };
    }
}
