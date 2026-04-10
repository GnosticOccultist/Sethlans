package fr.sethlans.core.render.view;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface Camera {
    
    Vector3f getLocation();
    
    default Camera setLocation(Vector3f location) {
        return setLocation(location.x(), location.y(), location.z());
    }

    Camera setLocation(float x, float y, float z);
    
    default void lookAlong(Vector3f direction, Vector3f up) {
        setRotation(getRotation().lookAlong(direction, up));
    }
    
    Quaternionf getRotation();

    Camera setRotation(Quaternionf rotation);
    
    Matrix4f getViewMatrix();

    Matrix4f getProjectionMatrix();

    Matrix4f getViewProjectionMatrix();
    
    Camera setNearDistance(float near);

    float getNearDistance();

    Camera setFarDistance(float far);

    float getFarDistance();
    
    /**
     * <code>CameraDirtyFields</code> is an enumeration to represent the fields
     * which can be dirty during the life-cycle of the {@link Camera}.
     * 
     * @author GnosticOccultist
     */
    enum CameraDirtyFields {
        /**
         * The depth range fields are dirty.
         */
        DEPTH_RANGE,
        /**
         * The view matrix is dirty and needs to be recomputed and probably resent to
         * the renderer.
         */
        VIEW_MATRIX,
        /**
         * The projection matrix is dirty and needs to be recomputed and probably resent
         * to the renderer.
         */
        PROJECTION_MATRIX,
        /**
         * The view-projection matrix is dirty and needs to be recomputed and probably
         * resent to the renderer.
         */
        VIEW_PROJECTION_MATRIX;
    }
}
