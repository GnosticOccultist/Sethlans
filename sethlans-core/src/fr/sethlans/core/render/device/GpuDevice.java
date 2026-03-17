package fr.sethlans.core.render.device;

public interface GpuDevice {

    default int getIntLimit(DeviceLimit limit) {
        return getLimit(limit).intValue();
    }

    default float getFloatLimit(DeviceLimit limit) {
        return getLimit(limit).floatValue();
    }

    default long getLongLimit(DeviceLimit limit) {
        return getLimit(limit).longValue();
    }

    Number getLimit(DeviceLimit limit);

    boolean supportsFeature(DeviceFeature feature);

    DeviceInfo getDeviceInfo();

    public enum Type {

        /**
         * The device does not match any other available types.
         */
        OTHER,
        /**
         * The device is typically one embedded in or tightly coupled with the host.
         */
        INTEGRATED_GPU,
        /**
         * The device is typically a separate processor connected to the host via an
         * interlink.
         */
        DISCRETE_GPU,
        /**
         * The device is typically a virtual node in a virtualization environment.
         */
        VIRTUAL_GPU,
        /**
         * The device is typically running on the same processors as the host.
         */
        CPU;
    }
}
