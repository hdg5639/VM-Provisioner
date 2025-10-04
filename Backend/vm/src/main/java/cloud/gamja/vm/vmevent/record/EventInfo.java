package cloud.gamja.vm.vmevent.record;

import java.time.Instant;
import java.util.UUID;

public record EventInfo(
        Integer vmid,
        UUID actorUserId,
        String action,
        String payload,
        Instant timestamp
) {
}
