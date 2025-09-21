package cloud.gamja.vm.vms.record;

import cloud.gamja.vm.vms.enums.VmType;

public record VmDetail(
        VmType vmType,
        String name,
        Integer cpu,
        Integer ramMb,
        Integer diskGb,
        Integer node
) {
}
