package fr.sethlans.core.render.state;

import fr.sethlans.core.render.state.raster.RasterizationState;

public class RenderState {

    private final RasterizationState rasterizationState = new RasterizationState();

    public RasterizationState getRasterizationState() {
        return rasterizationState;
    }
}
