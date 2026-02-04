package fr.sethlans.core.render.struct;

public interface LayoutFormatter {
    
    LayoutFormatter STD140 = new Std140Formatter();

    int size(ValueType valueType);

    int alignment(ValueType valueType);

    int getStructAlignment();
}
