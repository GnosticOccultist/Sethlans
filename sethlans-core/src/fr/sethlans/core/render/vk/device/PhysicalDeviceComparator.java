package fr.sethlans.core.render.vk.device;

import java.util.Comparator;

public interface PhysicalDeviceComparator extends Comparator<PhysicalDevice> {

    float evaluate(PhysicalDevice physicalDevice);

    @Override
    default int compare(PhysicalDevice o1, PhysicalDevice o2) {
        var s1 = evaluate(o1);
        var s2 = evaluate(o2);
        return Float.compare(s1, s2);
    }
}
