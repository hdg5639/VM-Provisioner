package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vmevent.enums.Actions;
import cloud.gamja.vm.vmevent.record.EventInfo;
import cloud.gamja.vm.vms.domain.Vm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmEventService {
    private final VmEventRepository vmEventRepository;

    public Mono<EventInfo> record(Vm vm, UUID actorUserId, Actions action, Map<String, Object> payload) {
        return vmEventRepository.save(VmEvent.builder()
                .vm(vm)
                .actorUserId(actorUserId)
                .action(action)
                .payload(payload)
                .build())
                .map(this::convert);

    }

    public Flux<EventInfo> getVmEvents(Vm vm) {
        return vmEventRepository.findByVm(vm)
                .map(this::convert);
    }

    private EventInfo convert(VmEvent vmEvent) {
        return new EventInfo(
                vmEvent.getVm().getDetail().vmid(),
                vmEvent.getActorUserId(),
                vmEvent.getAction(),
                vmEvent.getPayload(),
                vmEvent.getTimestamp()
        );
    }
}
