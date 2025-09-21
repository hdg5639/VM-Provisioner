package cloud.gamja.user.user.record;

import java.util.UUID;

public record UserDto(
        UUID id,
        String externalId,
        String email,
        String displayName
) {}
