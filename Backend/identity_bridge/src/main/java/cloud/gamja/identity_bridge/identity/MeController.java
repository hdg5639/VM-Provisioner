package cloud.gamja.identity_bridge.identity;

import cloud.gamja.identity_bridge.user.User;
import cloud.gamja.identity_bridge.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping("/api/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal OidcUser principal) {
        String sub = principal.getSubject();

        User user = userRepository.findByExternalId(sub)
                .orElse(null); // 최초 로그인 직후엔 SuccessHandler에서 이미 생성됨

        return ResponseEntity.ok(Map.of(
                "externalId", sub,
                "email", principal.getEmail(),
                "name", principal.getFullName(),
                "roles", principal.getAuthorities(),
                "user", Objects.requireNonNull(user)
        ));
    }
}
