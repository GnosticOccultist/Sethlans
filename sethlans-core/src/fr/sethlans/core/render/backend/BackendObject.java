package fr.sethlans.core.render.backend;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BackendObject {

    private static final int INVALID_ID = -1;

    private int internalId = INVALID_ID;

    private final AtomicBoolean dirty = new AtomicBoolean(true);

    private final AtomicBoolean toDestroy = new AtomicBoolean(false);

    public boolean hasBackendObject() {
        return internalId > INVALID_ID;
    }

    public int backendId() {
        return internalId;
    }

    public void assignId(int id) {
        this.internalId = id;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    protected void setDirty() {
        this.dirty.set(true);
    }

    public void clean() {
        this.dirty.set(false);
    }

    public void destroy() {
        this.toDestroy.set(true);
    }

    @Override
    public String toString() {
        return "BackendObject [internalId=" + internalId + "]";
    }
}
