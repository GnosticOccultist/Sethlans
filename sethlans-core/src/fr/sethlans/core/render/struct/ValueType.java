package fr.sethlans.core.render.struct;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public enum ValueType {

    BOOLEAN(boolean.class, Boolean.class),

    INTEGER(int.class, Integer.class),

    FLOAT(float.class, Float.class),

    VECTOR2F(Vector2f.class),

    VECTOR3F(Vector3f.class),

    VECTOR4F(Vector4f.class),

    MATRIX3F(Matrix3f.class),

    MATRIX4F(Matrix4f.class);

    private final Class<?>[] types;

    ValueType(Class<?>... types) {
        this.types = (types != null) ? types : new Class<?>[0];
    }

    public boolean isOfType(Class<?> type) {
        for (var c : types) {
            if (c.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    public Class<?>[] getTypes() {
        return types;
    }

    public static ValueType typeOf(Class<?> type) {
        for (var t : values()) {
            if (t.isOfType(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unexpected value type '" + type + "'!");
    }
}
