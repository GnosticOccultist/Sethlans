package fr.sethlans.core.render.vk.command;

import java.util.HashMap;
import java.util.Map;

import fr.sethlans.core.render.view.Scissor;
import fr.sethlans.core.render.view.Viewport;

public class DynamicRenderCache {

    private final Map<Class<?>, DynamicRenderCommand<?>> renderCommands = new HashMap<>();

    public DynamicRenderCache(CommandBuffer command) {
        var viewport = new DynamicRenderCommand<Viewport>("Viewport") {

            @Override
            protected void apply(Viewport state) {
                command.setViewport(state);
            }
        };
        var scissor = new DynamicRenderCommand<Scissor>("Scissor") {

            @Override
            protected void apply(Scissor state) {
                command.setScissor(state);
            }
        };
        
        renderCommands.put(Viewport.class, viewport);
        renderCommands.put(Scissor.class, scissor);
    }

    public void applyAll() {
        for (var renderCommand : renderCommands.values()) {
            renderCommand.apply();
        }
    }
    
    public void invalidateAll() {
        for (var renderCommand : renderCommands.values()) {
            renderCommand.invalidate();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void push(T state) {
        Class<T> type = (Class<T>) state.getClass();
        var renderCommand = getRenderCommand(type);
        renderCommand.push(state);
    }

    public <T> T pop(Class<T> type) {
        DynamicRenderCommand<T> renderCommand = getRenderCommand(type);
        return renderCommand.pop();
    }

    public <T> T peek(Class<T> type) {
        DynamicRenderCommand<T> renderCommand = getRenderCommand(type);
        return renderCommand.peek();
    }

    @SuppressWarnings("unchecked")
    public <T> DynamicRenderCommand<T> getRenderCommand(Class<T> type) {
        return (DynamicRenderCommand<T>) renderCommands.get(type);
    }
}
