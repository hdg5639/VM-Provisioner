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
import cloud.gamja.vm.vms.VmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmService {
    private final ProxmoxClient proxmoxClient;
    private final UserServiceClient userServiceClient;
    private final TokenExchangeClient tokenExchangeClient;
    private final VmRepository vmRepository;
    private final VmEventRepository vmEventRepository;

    public Mono<Map<String, Object>> listNodes() {
        return proxmoxClient.getNodes();
    }

    /*-----------------------VM 생성-----------------------*/
    public Mono<EventInfo> createVm( String subjectToken,
                                               String fingerprint,
                                               VmType vmType,
                                               String name,
                                               Integer disk,
                                               String ide) {

        Mono<Vm> created = proxmoxClient.createVmOptimize(subjectToken, fingerprint, vmType, name, disk, ide);
        Mono<UserDto> userInfo = tokenExchangeClient.exchange(subjectToken, Audience.USER)
                .flatMap(response -> userServiceClient.getMe(response.get("access_token")));

        Mono<VmEvent> vmEventMono = Mono.zip(created, userInfo)
                .flatMap(tuple ->
                        vmEventRepository.save(VmEvent.builder()
                                .id(UUID.randomUUID())
                                .vmId(tuple.getT1().getId())
                                .actorUserId(tuple.getT2().id())
                                .action(Actions.CREATE)
                                .payload("test payload")
                                .build())
                );
        return vmEventMono.flatMap(saved -> vmRepository.findById(saved.getVmId())
                        .map(body -> new EventInfo(
                                body.getDetail().vmid(),
                                saved.getActorUserId(),
                                saved.getAction(),
                                saved.getPayload(),
                                saved.getTimestamp()
                        )));
    }

    /*-----------------------VM 리스트 조회-----------------------*/
    public Flux<Vm> vmList(String subjectToken) {
        Mono<String> accessToken = tokenExchangeClient.exchange(subjectToken, Audience.USER)
                .map(m -> m.get("access_token"))
                .cache(Duration.ofSeconds(30));

        Mono<UUID> ownerId = accessToken
                .flatMap(userServiceClient::getMe)
                .map(UserDto::id);

        return ownerId.flatMapMany(vmRepository::findByOwnerUserId);
    }
}
