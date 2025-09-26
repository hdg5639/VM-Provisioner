package cloud.gamja.vm.vms.service;

import cloud.gamja.vm.client.ProxmoxClient;
import cloud.gamja.vm.vms.VmRepository;
import cloud.gamja.vm.vms.enums.VmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmService {
    private final VmRepository vmRepository;
    private final ProxmoxClient proxmoxClient;

    public Mono<Map<String, Object>> listNodes() {
        return proxmoxClient.getNodes();
    }

    public Mono<Map<String, Object>> createVm( String subjectToken,
                                               String userId,
                                               String fingerprint,
                                               VmType vmType,
                                               String name,
                                               Integer disk,
                                               String ide) {
        return proxmoxClient.createVmOptimize(subjectToken, userId, fingerprint, vmType, name, disk, ide);
    }
}
