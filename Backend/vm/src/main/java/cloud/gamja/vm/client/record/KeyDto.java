package cloud.gamja.vm.client.record;

import java.time.Instant;
import java.util.UUID;

public record KeyDto(
        UUID id,
        String name,
        String fingerprint,
        String publicKey,
        Instant createdAt
) {}
