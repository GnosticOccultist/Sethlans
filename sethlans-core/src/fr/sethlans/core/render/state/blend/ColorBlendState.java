package fr.sethlans.core.render.state.blend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorBlendState {
    
    public static final ColorBlendState DEFAULT = new ColorBlendState();

    private boolean logicOpEnable = false;
    
    private LogicOp logicOp = LogicOp.COPY;
    
    private final List<ColorBlendModeAttachment> blendAttachments = new ArrayList<>();
    
    private float[] blendConstants = new float[4];
    
    public ColorBlendState reset() {
        return set(DEFAULT);
    }

    public ColorBlendState set(ColorBlendState state) {
        logicOpEnable = state.logicOpEnable;
        logicOp = state.logicOp;
        blendConstants = Arrays.copyOf(blendConstants, 4);
        
        blendAttachments.clear();
        for (var attachment : state.blendAttachments) {
            blendAttachments.add(attachment.copy());
        }
        return this;
    }

    public ColorBlendState copy() {
        var copy = new ColorBlendState();
        copy.logicOpEnable = logicOpEnable;
        copy.logicOp = logicOp;
        copy.blendConstants = Arrays.copyOf(blendConstants, 4);
        
        for (var attachment : blendAttachments) {
            copy.blendAttachments.add(attachment.copy());
        }
        return copy;
    }
}
