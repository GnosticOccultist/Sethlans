package fr.sethlans.core.natives;

public interface NativeResourceManager {

    NativeReference register(NativeResource<?> resource);
    
    void clear();
}
