package fr.sethlans.core.render.struct.foreign;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.struct.ArraySize;
import fr.sethlans.core.render.struct.GpuStruct;
import fr.sethlans.core.render.struct.GpuStructLayout;
import fr.sethlans.core.render.struct.GpuStructLayout.LayoutType;
import fr.sethlans.core.render.struct.ValueType;

public class ForeignStructLayoutGenerator {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.struct.foreign");

    public static final Map<ValueType, MemoryLayout> MAPPER = new HashMap<>();

    private static final MemoryLayout VECTOR2F = MemoryLayout
            .structLayout(ValueLayout.JAVA_FLOAT.withName("x"), ValueLayout.JAVA_FLOAT.withName("y"))
            .withByteAlignment(8);

    private static final MemoryLayout VECTOR3F = MemoryLayout.structLayout(ValueLayout.JAVA_FLOAT.withName("x"),
            ValueLayout.JAVA_FLOAT.withName("y"), ValueLayout.JAVA_FLOAT.withName("z"), MemoryLayout.paddingLayout(4))
            .withByteAlignment(16);

    private static final MemoryLayout VECTOR4F = MemoryLayout
            .structLayout(ValueLayout.JAVA_FLOAT.withName("x"), ValueLayout.JAVA_FLOAT.withName("y"),
                    ValueLayout.JAVA_FLOAT.withName("z"), ValueLayout.JAVA_FLOAT.withName("w"))
            .withByteAlignment(16);

    private static final MemoryLayout MATRIX3F = MemoryLayout.sequenceLayout(3, VECTOR3F).withByteAlignment(16);

    private static final MemoryLayout MATRIX4F = MemoryLayout
            .sequenceLayout(4, MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_FLOAT)).withByteAlignment(16);

    static {
        MAPPER.put(ValueType.BOOLEAN, ValueLayout.JAVA_BOOLEAN.withByteAlignment(4));
        MAPPER.put(ValueType.INTEGER, ValueLayout.JAVA_INT.withByteAlignment(4));
        MAPPER.put(ValueType.FLOAT, ValueLayout.JAVA_FLOAT.withByteAlignment(4));
        MAPPER.put(ValueType.VECTOR2F, VECTOR2F);
        MAPPER.put(ValueType.VECTOR3F, VECTOR3F);
        MAPPER.put(ValueType.VECTOR4F, VECTOR4F);
        MAPPER.put(ValueType.MATRIX3F, MATRIX3F);
        MAPPER.put(ValueType.MATRIX4F, MATRIX4F);
    }
    
    public static <T extends GpuStruct> GpuStructLayout sequenceLayoutOf(Class<T> struct, int elementCount, LayoutType layout) {
        if (!struct.isRecord()) {
            throw new IllegalArgumentException("Provided GpuStruct must be a record!");
        }

        var structLayout = resolveRecord(struct, layout);
        
        var stride = (layout == LayoutType.STD140) ? align(structLayout.byteSize(), 16)
                : align(structLayout.byteSize(), structLayout.byteAlignment());
        
        var paddedLayout = padToStride(structLayout, stride);
        var sequenceLayout = MemoryLayout.sequenceLayout(elementCount, paddedLayout);
        
        logger.info(structLayout);

        return new ForeignStructLayout(sequenceLayout, layout);
    }

    public static <T extends GpuStruct> GpuStructLayout layoutOf(Class<T> struct, LayoutType layout) {
        if (!struct.isRecord()) {
            throw new IllegalArgumentException("Provided GpuStruct must be a record!");
        }

        var structLayout = resolveRecord(struct, layout);
        logger.info(structLayout);

        return new ForeignStructLayout(structLayout, layout);
    }

    private static StructLayout resolveRecord(Class<?> struct, LayoutType layout) {
        List<MemoryLayout> fields = new ArrayList<>();

        var maxAlign = 0L;

        for (var component : struct.getRecordComponents()) {

            var type = component.getType();
            var name = component.getName();

            var memLayout = resolveComponent(type, component, layout);
            memLayout.withName(name);

            fields.add(memLayout);

            maxAlign = Math.max(maxAlign, memLayout.byteAlignment());
        }

        var structAlign = (layout == LayoutType.STD140) ? Math.max(16, maxAlign) : maxAlign;

        var recordLayout = MemoryLayout.structLayout(fields.toArray(MemoryLayout[]::new)).withByteAlignment(structAlign)
                .withName(struct.getSimpleName());
        return recordLayout;
    }

    private static MemoryLayout resolveComponent(Class<?> type, RecordComponent component, LayoutType layout) {

        // Nested struct.
        if (type.isRecord()) {
            return resolveRecord(type, layout);
        }

        if (type.isArray()) {
            var arraySize = component.getAnnotation(ArraySize.class);
            if (arraySize == null) {
                throw new IllegalStateException("Array requires fixed length annotation!");
            }

            var componentType = type.componentType();
            MemoryLayout memLayout = null;
            if (componentType.isRecord()) {
                memLayout = resolveRecord(componentType, layout);

            } else {
                var valueType = ValueType.typeOf(componentType);
                memLayout = MAPPER.get(valueType);
            }

            var stride = (layout == LayoutType.STD140) ? align(memLayout.byteSize(), 16)
                    : align(memLayout.byteSize(), memLayout.byteAlignment());

            var paddedLayout = padToStride(memLayout, stride);
            return MemoryLayout.sequenceLayout(arraySize.value(), paddedLayout);
        }

        var valueType = ValueType.typeOf(type);
        var memLayout = MAPPER.get(valueType);
        return memLayout.withName(component.getName());
    }

    private static MemoryLayout padToStride(MemoryLayout element, long stride) {
        var size = element.byteSize();
        if (size == stride) {
            return element;
        }

        if (size > stride) {
            throw new IllegalStateException("Invalid stride!");
        }

        return MemoryLayout.structLayout(element, MemoryLayout.paddingLayout(stride - size))
                .withByteAlignment(element.byteAlignment());
    }

    private static long align(long v, long a) {
        return (v + a - 1) & ~(a - 1);
    }

    public interface GroupLayoutMapper {

        MemoryLayout map();
    }
}
