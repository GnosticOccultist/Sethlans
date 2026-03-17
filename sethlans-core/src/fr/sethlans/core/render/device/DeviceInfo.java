package fr.sethlans.core.render.device;

import fr.sethlans.core.render.device.GpuDevice.Type;

public record DeviceInfo(String name, Vendor vendor, Type type, String driverName, String driverInfo) {
    
}
