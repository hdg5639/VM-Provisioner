package cloud.gamja.vm.vms.record;

import cloud.gamja.vm.vms.enums.VmType;

public record VmRequest(
        String fingerprint,
        VmType vmType,
        String name,
        Integer disk,
        String ide
) {
}
