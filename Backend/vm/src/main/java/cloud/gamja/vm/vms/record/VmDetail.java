package cloud.gamja.vm.vms.record;

import cloud.gamja.vm.vms.enums.VmType;

public record VmDetail(
        VmType vmType,
        Integer vmid,
        String name,
        Integer cpu,
        Integer ram,
        Integer disk,
        String ipv4
) {
}
