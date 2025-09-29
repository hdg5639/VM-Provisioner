package cloud.gamja.vm.vms.service;

import cloud.gamja.vm.client.ProxmoxClient;
import cloud.gamja.vm.client.TokenExchangeClient;
import cloud.gamja.vm.client.UserServiceClient;
import cloud.gamja.vm.client.enums.Audience;
import cloud.gamja.vm.client.record.UserDto;
import cloud.gamja.vm.vmevent.VmEventRepository;
import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vmevent.enums.Actions;
import cloud.gamja.vm.vmevent.record.EventInfo;
import cloud.gamja.vm.vms.domain.Vm;
import cloud.gamja.vm.vms.enums.VmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmService {
    private final VmEventRepository vmEventRepository;
    private final ProxmoxClient proxmoxClient;
    private final UserServiceClient userServiceClient;
    private final TokenExchangeClient tokenExchangeClient;

    public Mono<Map<String, Object>> listNodes() {
        return proxmoxClient.getNodes();
    }

    public Mono<EventInfo> createVm( String subjectToken,
                                               String fingerprint,
                                               VmType vmType,
                                               String name,
                                               Integer disk,
                                               String ide) {

        Mono<Vm> created = proxmoxClient.createVmOptimize(subjectToken, fingerprint, vmType, name, disk, ide);
        Mono<UserDto> userInfo = tokenExchangeClient.exchange(subjectToken, Audience.USER)
                .flatMap(response -> userServiceClient.getMe(response.get("access_token")));
        return Mono.zip(created, userInfo)
                .flatMap(tuple -> Mono.fromCallable(() -> vmEventRepository.save(VmEvent.builder()
                                .vm(tuple.getT1())
                                .actorUserId(tuple.getT2().id())
                                .action(Actions.CREATE)
                                .payload(Map.of("test", "test payload"))
                                .build()))
                .subscribeOn(Schedulers.boundedElastic()))
                .map(saved -> new EventInfo(
                        saved.getVm().getDetail().vmid(),
                        saved.getActorUserId(),
                        saved.getAction(),
                        saved.getPayload(),
                        saved.getTimestamp()
                ));
    }
}
