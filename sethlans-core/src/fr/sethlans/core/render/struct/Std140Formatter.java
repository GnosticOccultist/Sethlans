package fr.sethlans.core.render.struct;

final class Std140Formatter implements LayoutFormatter {
    
    Std140Formatter() {
        super();
    }

    @Override
    public int size(ValueType valueType) {
        return switch (valueType) {
        case BOOLEAN, INTEGER, FLOAT -> 4;
        case VECTOR2F -> 8;
        case VECTOR3F, VECTOR4F -> 16;
        case MATRIX3F -> 48;
        case MATRIX4F -> 64;
        default -> throw new IllegalArgumentException("Unexpected value type " + valueType + "!");
    };
    }

    @Override
    public int alignment(ValueType valueType) {
        return switch (valueType) {
        case BOOLEAN, INTEGER, FLOAT  -> Float.BYTES;
        case VECTOR2F -> 2 * Float.BYTES;
        case VECTOR3F, VECTOR4F, MATRIX3F, MATRIX4F -> 4 * Float.BYTES;
        default -> throw new IllegalArgumentException("Unexpected value type " + valueType + "!");
    };
    }

    @Override
    public int getStructAlignment() {
        return 16;
    }
}
