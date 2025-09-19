package cloud.gamja.user.user.record;

public record UserDto(
        Long id,
        String externalId,
        String email,
        String displayName) {}
