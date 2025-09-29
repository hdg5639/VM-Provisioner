package cloud.gamja.vm.vmevent.record;

import cloud.gamja.vm.vmevent.enums.Actions;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventInfo(
        Integer vmid,
        UUID actorUserId,
        Actions action,
        Map<String, Object> payload,
        Instant timestamp
) {
}
