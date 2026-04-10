package fr.sethlans.core.render.view;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PerspectiveCamera implements Camera {

    protected final Vector3f location = new Vector3f();
    protected final Quaternionf rotation = new Quaternionf();
    protected float fov = 45f;
    protected float aspect = 1.33f;
    protected float near = 0.1f;
    protected float far = 100.0f;
    protected final Matrix4f viewMatrix = new Matrix4f();
    protected final Matrix4f projectionMatrix = new Matrix4f();
    protected final Matrix4f viewProjectionMatrix = new Matrix4f();

    private final EnumSet<CameraDirtyFields> dirtyFields = EnumSet.of(
            CameraDirtyFields.PROJECTION_MATRIX, CameraDirtyFields.VIEW_MATRIX);

    public PerspectiveCamera() {
        // Make sure all the rendering data such as matrices is computed before the
        // first rendering pass
        // and applied to the renderer.
        this.dirtyFields.addAll(Arrays.asList(CameraDirtyFields.values()));
    }

    @Override
    public Vector3f getLocation() {
        return location;
    }

    @Override
    public Camera setLocation(float x, float y, float z) {
        this.location.set(x, y, z);
        this.dirtyFields.add(CameraDirtyFields.VIEW_MATRIX);
        return this;
    }

    @Override
    public Quaternionf getRotation() {
        return rotation;
    }

    @Override
    public Camera setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        this.dirtyFields.add(CameraDirtyFields.VIEW_MATRIX);
        return this;
    }

    @Override
    public Camera setNearDistance(float near) {
        if (Objects.equals(this.near, near)) {
            return this;
        }
        
        this.near = near;
        this.dirtyFields.add(CameraDirtyFields.PROJECTION_MATRIX);
        return this;
    }

    @Override
    public float getNearDistance() {
        return near;
    }

    @Override
    public Camera setFarDistance(float far) {
        if (Objects.equals(this.far, far)) {
            return this;
        }
        
        this.far = far;
        this.dirtyFields.add(CameraDirtyFields.PROJECTION_MATRIX);
        return this;
    }

    @Override
    public float getFarDistance() {
        return far;
    }

    @Override
    public Matrix4f getViewMatrix() {
        updateView();
        return viewMatrix;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        updateProjection();
        return projectionMatrix;
    }

    @Override
    public Matrix4f getViewProjectionMatrix() {
        updateViewProjection();
        return viewProjectionMatrix;
    }

    public float getFov() {
        return fov;
    }

    public Camera setFov(float fov) {
        if (Objects.equals(this.fov, fov)) {
            return this;
        }
        
        this.fov = fov;
        this.dirtyFields.add(CameraDirtyFields.PROJECTION_MATRIX);
        return this;
    }
    
    public float getAspect() {
        return aspect;
    }

    public Camera setAspect(float aspect) {
        if (Objects.equals(this.aspect, aspect)) {
            return this;
        }
        
        this.aspect = aspect;
        this.dirtyFields.add(CameraDirtyFields.PROJECTION_MATRIX);
        return this;
    }

    protected void updateProjection() {
        if (!dirtyFields.contains(CameraDirtyFields.PROJECTION_MATRIX)) {
            return;
        }
        
        this.projectionMatrix.identity().perspective((float) Math.toRadians(fov), aspect, near,
                far, true);
        this.dirtyFields.remove(CameraDirtyFields.PROJECTION_MATRIX);
        
        this.dirtyFields.addAll(EnumSet.of(CameraDirtyFields.VIEW_MATRIX, CameraDirtyFields.VIEW_PROJECTION_MATRIX));
    }

    protected void updateView() {
        updateProjection();
        
        if (!dirtyFields.contains(CameraDirtyFields.VIEW_MATRIX)) {
            return;
        }
        
        this.viewMatrix.identity().translate(-location.x(), -location.y(), -location.z())
                .rotate(rotation);
        
        this.dirtyFields.remove(CameraDirtyFields.VIEW_MATRIX);
        
        this.dirtyFields.add(CameraDirtyFields.VIEW_PROJECTION_MATRIX);
    }
    
    protected void updateViewProjection() {
        updateView();
        if (dirtyFields.contains(CameraDirtyFields.VIEW_PROJECTION_MATRIX)) {
            viewProjectionMatrix.set(getProjectionMatrix()).mul(getViewMatrix());
            dirtyFields.remove(CameraDirtyFields.VIEW_PROJECTION_MATRIX);
        }
    }
}
