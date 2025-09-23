package cloud.gamja.vm.client.record;

import java.util.UUID;

public record UserDto(
        UUID id,
        String externalId,
        String email,
        String displayName
) {}
