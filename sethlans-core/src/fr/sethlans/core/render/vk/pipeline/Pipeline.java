package fr.sethlans.core.render.vk.pipeline;

import fr.sethlans.core.render.vk.device.VulkanResource;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline.BindPoint;

public interface Pipeline extends VulkanResource {

    BindPoint getBindPoint();

    PipelineLayout getLayout();
}
