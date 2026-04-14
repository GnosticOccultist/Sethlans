package fr.sethlans.core.render.vk.pass;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;

public class AttachmentReference {

    private final Attachment attachment;

    private final Layout layout;

    protected AttachmentReference(Attachment attachment, Layout layout) {
        this.attachment = attachment;
        this.layout = layout;
    }

    void describe(VkAttachmentReference pAttachmentRef) {
        pAttachmentRef.attachment(getAttachmentPosition())
                .layout(layout.vkEnum());
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public Layout getLayout() {
        return layout;
    }

    public int getAttachmentPosition() {
        return attachment != null ? attachment.getPosition() : VK10.VK_ATTACHMENT_UNUSED;
    }

    @Override
    public String toString() {
        return "AttachmentReference [attachment=" + attachment + ", layout=" + layout + "]";
    }
}
