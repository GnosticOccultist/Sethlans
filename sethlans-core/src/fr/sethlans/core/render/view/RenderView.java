package fr.sethlans.core.render.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.sethlans.core.scenegraph.Geometry;

public class RenderView {

    private final Camera camera;
    
    private boolean enabled = true;
    
    private final List<Geometry> geometries = new ArrayList<>();

    public RenderView(Camera camera) {
        this.camera = camera;
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
