package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vmevent.enums.Actions;
import cloud.gamja.vm.vmevent.record.EventInfo;
import cloud.gamja.vm.vms.VmRepository;
import cloud.gamja.vm.vms.domain.Vm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmEventService {
    private final VmEventRepository vmEventRepository;
    private final VmRepository vmRepository;

    public Mono<EventInfo> record(Vm vm, UUID actorUserId, Actions action, String payload) {
        return vmEventRepository.save(VmEvent.builder()
                .vmId(vm.getId())
                .actorUserId(actorUserId)
                .action(action)
                .payload(payload)
                .build())
                .flatMap(this::convert);

    }

    public Flux<EventInfo> getVmEvents(Vm vm) {
        return vmEventRepository.findByVm(vm)
                .flatMap(this::convert);
    }

    private Mono<EventInfo> convert(VmEvent vmEvent) {
        return vmRepository.findById(vmEvent.getVmId())
                .map(vm -> new EventInfo(
                        vm.getDetail().vmid(),
                        vmEvent.getActorUserId(),
                        vmEvent.getAction(),
                        vmEvent.getPayload(),
                        vmEvent.getTimestamp()
                ));
    }
}
