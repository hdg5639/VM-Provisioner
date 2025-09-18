package cloud.gamja.identity_bridge.config;

import cloud.gamja.identity_bridge.identity.IdentityLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final IdentityLinkService identityLinkService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {
        OidcUser principal = (OidcUser) authentication.getPrincipal();

        // Keycloak sub → users.external_id 업서트
        identityLinkService.linkOrCreate(principal);

        // 로그인 후 이동할 경로
        response.sendRedirect("/");
    }
}
