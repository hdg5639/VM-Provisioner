package cloud.gamja.user.sshkey.record;

import java.time.Instant;

public record KeyDto(
        Long id,
        String name,
        String fingerprint,
        String publicKey,
        Instant createdAt
) {}
