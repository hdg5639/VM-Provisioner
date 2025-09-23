package cloud.gamja.vm.vms.service.infra;

import cloud.gamja.vm.client.ProxmoxClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class VmOperator {
    private final ProxmoxClient proxmoxClient;

    public Mono<Map<String, Object>> listNodes() {
        return proxmoxClient.getNodes();
    }
}