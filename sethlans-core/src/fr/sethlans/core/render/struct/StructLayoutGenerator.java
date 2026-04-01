package fr.sethlans.core.render.struct;

import java.util.ArrayList;
import java.util.List;

public final class StructLayoutGenerator {

    public static <T extends GpuStruct> StructLayout generate(Class<T> struct, LayoutFormatter formatter) {
        if (!struct.isRecord()) {
            throw new IllegalArgumentException("Provided GpuStruct must be a record!");
        }

        var offset = 0;
        List<StructField> fields = new ArrayList<>();

        for (var component : struct.getRecordComponents()) {
            var type = component.getType();
            var valueType = ValueType.typeOf(type);
            var alignment = formatter.alignment(valueType);
            var size = formatter.size(valueType);

            offset = align(offset, alignment);

            fields.add(new SimpleStructField(component.getName(), offset, size, alignment, type));
        }

        var structAlignment = formatter.getStructAlignment();
        var structSize = align(offset, structAlignment);
        var layout = new StructLayout(struct.getSimpleName(), 0, structSize, fields);

        return layout;
    }

    static int align(int offset, int alignment) {
        return (offset + alignment - 1) & ~(alignment - 1);
    }
    
    public interface StructField {
        
        String name();
        
        int offset();
        
        int size();
    }

    public record SimpleStructField(String name, int offset, int size, int alignment, Class<?> type) implements StructField {
    }

    public record StructLayout(String name, int offset, int size, List<StructField> fields) implements StructField {

        public StructField getField(String name) {
            return fields.stream().filter(field -> field.name().equals(name)).findFirst().orElseThrow();
        }
    }
}
