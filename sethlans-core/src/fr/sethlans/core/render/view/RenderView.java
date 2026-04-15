package fr.sethlans.core.render.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.sethlans.core.scenegraph.Geometry;

public class RenderView {

    private final Camera camera;

    private boolean enabled = true;

    private Framebuffer framebuffer;
    
    private final Viewport viewport = new Viewport();
    
    private final Scissor scissor = new Scissor();

    private final List<Geometry> geometries = new ArrayList<>();

    public RenderView(Camera camera) {
        this(camera, null);
    }

    public RenderView(Camera camera, Framebuffer framebuffer) {
        this.camera = camera;
        this.framebuffer = framebuffer;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Scissor getScissor() {
        return scissor;
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public void setFramebuffer(Framebuffer framebuffer) {
        this.framebuffer = framebuffer;
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addGeometry(Geometry geometry) {
        this.geometries.add(geometry);
    }

    public void removeGeometry(Geometry geometry) {
        this.geometries.remove(geometry);
    }

    public List<Geometry> getGeometries() {
        return Collections.unmodifiableList(geometries);
    }
}
