package cloud.gamja.user.sshkey.record;

import jakarta.validation.constraints.NotBlank;

public record CreateKeyReq(
        @NotBlank
        String name,
        @NotBlank
        String publicKey) {}

