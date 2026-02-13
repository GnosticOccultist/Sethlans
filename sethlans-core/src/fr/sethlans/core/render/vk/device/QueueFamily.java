package fr.sethlans.core.render.vk.device;

import fr.sethlans.core.render.vk.util.VkFlag;

public record QueueFamily(int index, int queueCount, boolean supportPresentation, VkFlag<QueueFlag> flags)
        implements Comparable<QueueFamily> {

    QueueFamily(int index, int queueCount, boolean supportPresentation, int flagBits) {
        this(index, queueCount, supportPresentation, VkFlag.of(flagBits));
    }
    
    public boolean supportsGraphics() {
        return flags.contains(QueueFlag.GRAPHICS);
    }
    
    public boolean supportsTransfer() {
        return flags.contains(QueueFlag.TRANSFER);
    }
    
    public boolean supportsCompute() {
        return flags.contains(QueueFlag.COMPUTE);
    }

    @Override
    public int compareTo(QueueFamily o) {
        return Integer.compare(index, o.index);
    }

    @Override
    public String toString() {
        return "QueueFamily [index= " + index + ", queueCount= " + queueCount + ", supportPresentation= "
                + supportPresentation + ", flags= " + flags.toString(QueueFlag.class) + "]";
    }
}
