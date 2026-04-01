package fr.sethlans.core.render.struct;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;

public interface GpuStructLayout {

    <T> T get(String name, NativeBuffer buffer);

    <T> void set(NativeBuffer buffer, String name, T value);
    
    <T> void set(NativeBuffer buffer, String name, int frameIndex, T value);
    
    LayoutType type();

    MemorySize size();

    public enum LayoutType {

        STD140,

        STD430;
    }
}
