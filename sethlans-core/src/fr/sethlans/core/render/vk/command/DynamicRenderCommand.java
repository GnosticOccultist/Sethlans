package fr.sethlans.core.render.vk.command;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class DynamicRenderCommand<T> {

    private final String name;
    private final Deque<T> states = new ArrayDeque<>();
    private boolean dirty = false;

    protected DynamicRenderCommand(String name) {
        this.name = name;
    }

    public void push(T value) {
        this.dirty = dirty || states.isEmpty() || !states.peek().equals(value);
        states.push(value);
    }

    public T peek() {
        return states.peek();
    }

    public T pop() {
        var current = states.pop();
        this.dirty = dirty || (!states.isEmpty() && !current.equals(states.peek()));
        return current;
    }

    public void apply() {
        if (dirty) {
            apply(states.peek());
            dirty = false;
        }
    }
    
    protected abstract void apply(T state);

    protected void invalidate() {
        this.dirty = true;
    }

    @Override
    public String toString() {
        return "DynamicRenderCommand [name=" + name + ", dirty=" + dirty + ", states=" + states + "]";
    }
}
