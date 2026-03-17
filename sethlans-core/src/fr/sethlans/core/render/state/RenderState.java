package fr.sethlans.core.render.state;

import fr.sethlans.core.render.state.blend.ColorBlendState;
import fr.sethlans.core.render.state.depth.DepthStencilState;
import fr.sethlans.core.render.state.multisample.MultisampleState;
import fr.sethlans.core.render.state.raster.RasterizationState;

public class RenderState {

    private final RasterizationState rasterizationState = new RasterizationState();

    private final MultisampleState multisampleState = new MultisampleState();

    private final DepthStencilState depthStencilState = new DepthStencilState();
    
    private final ColorBlendState colorBlendState = new ColorBlendState();

    public RasterizationState getRasterizationState() {
        return rasterizationState;
    }

    public MultisampleState getMultisampleState() {
        return multisampleState;
    }

    public DepthStencilState getDepthStencilState() {
        return depthStencilState;
    }

    public ColorBlendState getColorBlendState() {
        return colorBlendState;
    }
}
