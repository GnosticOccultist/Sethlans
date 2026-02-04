package fr.sethlans.core.render.buffer;

public final class DirtyRegion {

    private int offset, end;

    public DirtyRegion(int offset, int size) {
        set(offset, size);
    }

    public DirtyRegion(DirtyRegion region) {
        set(region);
    }

    public DirtyRegion set(DirtyRegion region) {
        this.offset = region.offset;
        this.end = region.end;
        return this;
    }

    public DirtyRegion set(int offset, int size) {
        this.offset = offset;
        this.end = offset + size;
        return this;
    }

    public DirtyRegion merge(DirtyRegion region) {
        this.offset = Math.min(offset, region.offset);
        this.end = Math.max(end, region.end);
        return this;
    }

    public void clear() {
        set(0, 0);
    }
}
