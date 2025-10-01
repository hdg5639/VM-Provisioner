package cloud.gamja.vm.vmevent.record;

import cloud.gamja.vm.vmevent.enums.Actions;

import java.time.Instant;
import java.util.UUID;

public record EventInfo(
        Integer vmid,
        UUID actorUserId,
        Actions action,
        String payload,
        Instant timestamp
) {
}
