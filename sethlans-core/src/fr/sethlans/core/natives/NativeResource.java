package fr.sethlans.core.natives;

public interface NativeResource<T> {

    T getNativeObject();

    Runnable createDestroyAction();
    
    NativeReference getNativeReference();

    static NativeResourceManager get() {
        return NativeResourceCleaner.getInstance();
    }
}
