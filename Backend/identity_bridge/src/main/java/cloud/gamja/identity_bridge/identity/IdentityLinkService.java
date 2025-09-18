package cloud.gamja.identity_bridge.identity;

import cloud.gamja.identity_bridge.user.User;
import cloud.gamja.identity_bridge.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityLinkService {

    private final UserRepository userRepository;

    @Transactional
    public void linkOrCreate(OidcUser oidcUser) {
        String sub = oidcUser.getSubject();              // Keycloak 'sub'
        String email = oidcUser.getEmail();              // 있을 때만
        String name = oidcUser.getFullName();            // 있을 때만

        userRepository.findByExternalId(sub)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .externalId(sub)
                                .email(email)
                                .displayName(name)
                                .build()
                ));
    }
}
