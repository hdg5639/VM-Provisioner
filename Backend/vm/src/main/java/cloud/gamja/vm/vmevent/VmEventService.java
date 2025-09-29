package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vmevent.enums.Actions;
import cloud.gamja.vm.vmevent.record.EventInfo;
import cloud.gamja.vm.vms.domain.Vm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmEventService {
    private final VmEventRepository vmEventRepository;

    public EventInfo record(Vm vm, UUID actorUserId, Actions action, Map<String, Object> payload) {
        VmEvent saved = vmEventRepository.save(VmEvent.builder()
                .vm(vm)
                .actorUserId(actorUserId)
                .action(action)
                .payload(payload)
                .build());

        return convert(saved);
    }

    public List<EventInfo> getVmEvents(Vm vm) {
        return vmEventRepository.findByVm(vm)
                .stream().map(this::convert)
                .toList();
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
