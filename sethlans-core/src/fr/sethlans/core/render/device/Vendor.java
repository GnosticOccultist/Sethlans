package fr.sethlans.core.render.device;

public enum Vendor {

    UNKNOWN,

    NVIDIA,

    AMD,

    INTEL,

    APPLE,

    QUALCOMM,

    RASPBERRY_PI;

    public static Vendor fromID(int vendorID) {
        Vendor vendor = switch (vendorID) {
        case 0x10DE -> Vendor.NVIDIA;
        case 0x1002 -> Vendor.AMD;
        case 0x8086 -> Vendor.INTEL;
        case 0x106b -> Vendor.APPLE;
        case 0x17cb -> Vendor.QUALCOMM;
        case 0x1de4 -> Vendor.RASPBERRY_PI;
        default -> vendor = Vendor.UNKNOWN;

        };

        return vendor;
    }
}
