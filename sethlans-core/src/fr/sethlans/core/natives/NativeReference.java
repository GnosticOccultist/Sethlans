package fr.sethlans.core.natives;

public interface NativeReference {

    void addDependent(NativeReference reference);

    boolean isDestroyed();

    void destroy();
}
