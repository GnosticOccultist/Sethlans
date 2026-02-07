package fr.sethlans.core.natives;

public abstract class AbstractNativeResource<T> implements NativeResource<T> {

    protected T object;
    protected NativeReference ref;

    @Override
    public T getNativeObject() {
        return object;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }
}
