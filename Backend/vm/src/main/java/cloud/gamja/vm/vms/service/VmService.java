package cloud.gamja.vm.vms.service;

import cloud.gamja.vm.client.ProxmoxClient;
import cloud.gamja.vm.client.TokenExchangeClient;
import cloud.gamja.vm.client.UserServiceClient;
import cloud.gamja.vm.client.enums.Audience;
import cloud.gamja.vm.client.record.UserDto;
import cloud.gamja.vm.vms.VmRepository;
import cloud.gamja.vm.vms.domain.Vm;
import cloud.gamja.vm.vms.enums.VmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmService {
    private final VmRepository vmRepository;
    private final ProxmoxClient proxmoxClient;
    private final UserServiceClient userServiceClient;
    private final TokenExchangeClient tokenExchangeClient;

    public Mono<Map<String, Object>> listNodes() {
        return proxmoxClient.getNodes();
    }

    @Transactional
    public Mono<Map<String, Object>> createVm( String subjectToken,
                                               String fingerprint,
                                               VmType vmType,
                                               String name,
                                               Integer disk,
                                               String ide) {

        Mono<Map<String, Object>> created = proxmoxClient.createVmOptimize(subjectToken, fingerprint, vmType, name, disk, ide);
//        Mono<UserDto> userInfo = tokenExchangeClient.exchange(subjectToken, Audience.USER)
//                .flatMap(userServiceClient.getUser(this, ))
        return created;
    }
}
