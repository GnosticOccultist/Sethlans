package fr.sethlans.core.render.buffer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class DirtyRegions implements Iterable<DirtyRegions.Region> {

    private Region head = null;
    private int regionCount;
    private int dirtySize;

    public void add(long offset, long size) {
        if (offset < 0 || size < 0) {
            throw new IllegalArgumentException("Dirty region parameters must be non-negative!");
        }
        if (size == 0) {
            return;
        }

        var newStart = offset;
        var newEnd = offset + size;

        Region prev = null;
        Region curr = head;

        // Find first region that might overlap.
        while (curr != null && curr.end < newStart) {
            prev = curr;
            curr = curr.next;
        }

        // Merge overlapping regions.
        while (curr != null && curr.start <= newEnd) {
            newStart = Math.min(newStart, curr.start);
            newEnd = Math.max(newEnd, curr.end);

            // Remove current.
            curr = curr.next;
            this.regionCount--;
        }

        /// Create merged region.
        var merged = new Region(newStart, newEnd);
        merged.next = curr;

        // Link into list.
        if (prev == null) {
            head = merged;
        } else {
            prev.next = merged;
        }
        this.regionCount++;

        dirtySize = 0;
        for (var r : this) {
            dirtySize += r.size;
        }
    }

    public int regionCount() {
        return regionCount;
    }

    public int dirtySize() {
        return dirtySize;
    }

    public void clear() {
        this.head = null;
        this.regionCount = 0;
    }

    public boolean isEmpty() {
        return head == null || head.isEmpty();
    }

    @Override
    public Iterator<Region> iterator() {
        return new RegionIterator(head);
    }

    public final class Region {

        private long start, size, end;
        private Region next;

        Region(long start, long end) {
            this.start = start;
            this.size = end - start;
            this.end = end;
        }

        boolean isEmpty() {
            return size <= 0;
        }
        
        public long end() {
            return end;
        }

        public long start() {
            return start;
        }

        public long size() {
            return size;
        }

        boolean isTail() {
            return next == null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, next);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            var other = (Region) obj;
            return start == other.start && end == other.end && Objects.equals(next, other.next);
        }

        @Override
        public String toString() {
            return "Region [start=" + start + ", end=" + end + ", next=" + next + "]";
        }
    }

    private static class RegionIterator implements Iterator<Region> {

        private Region current, prev;

        private RegionIterator(Region head) {
            this.current = head;
        }

        @Override
        public boolean hasNext() {
            return current != null && !current.isEmpty();
        }

        @Override
        public Region next() {
            if (current == null) {
                throw new NoSuchElementException();
            }

            prev = current;
            current = current.next;
            return prev;
        }

        @Override
        public void remove() {
            if (prev != null && current != null) {
                prev.next = current.next;
            }
        }
    }
}
