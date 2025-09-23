package cloud.gamja.vm.vms.service;

import cloud.gamja.vm.vms.VmRepository;
import cloud.gamja.vm.vms.service.infra.VmOperator;
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
    private final VmOperator vmOperator;

    public Mono<Map<String, Object>> callTest() {
        Mono<Map<String, Object>> result = vmOperator.listNodes();
        log.debug(result.toString());
        return result;
    }
}
