package fr.sethlans.core.render.struct.foreign;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import org.joml.Matrix4f;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.struct.GpuStructLayout;
import fr.sethlans.core.render.struct.ValueType;

public class ForeignStructLayout implements GpuStructLayout {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.context");

    private final MemoryLayout structLayout;

    private final LayoutType layout;

    ForeignStructLayout(MemoryLayout structLayout, LayoutType layout) {
        this.structLayout = structLayout;
        this.layout = layout;
    }

    @Override
    public <T> T get(String name, NativeBuffer buffer) {
        var handle = structLayout.varHandle(MemoryLayout.PathElement.groupElement(name),
                MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.sequenceElement());
        try (var map = buffer.map()) {
            var seg = MemorySegment.ofAddress(map.getAddress());
        }
        return null;
    }

    @Override
    public void set(NativeBuffer buffer, String name, Object value) {
        var type = ValueType.typeOf(value.getClass());
        try (var m = buffer.map()) {
            switch (type) {
            case BOOLEAN:
                break;
            case FLOAT:
                break;
            case INTEGER:
                break;
            case MATRIX3F:
                break;
            case MATRIX4F:
                var v = (Matrix4f) value;
                v.get(0, m.getBytes());
                break;
            case VECTOR2F:
                break;
            case VECTOR3F:
                break;
            case VECTOR4F:
                break;
            default:
                throw new IllegalArgumentException("Invalid ValueType " + type + " from " + value);
            }
        }
    }

    @Override
    public <T> void set(NativeBuffer buffer, String name, int frameIndex, T value) {
        var type = ValueType.typeOf(value.getClass());
        try (var m = buffer.map()) {
            switch (type) {
            case BOOLEAN:
                break;
            case FLOAT:
                break;
            case INTEGER:
                break;
            case MATRIX3F:
                break;
            case MATRIX4F:
                var v = (Matrix4f) value;
                v.get(0, m.getBytes());
                break;
            case VECTOR2F:
                break;
            case VECTOR3F:
                break;
            case VECTOR4F:
                break;
            default:
                throw new IllegalArgumentException("Invalid ValueType " + type + " from " + value);
            }
        }
    }

    @Override
    public LayoutType type() {
        return layout;
    }

    @Override
    public MemorySize size() {
        return MemorySize.bytes(structLayout.byteSize());
    }
}
