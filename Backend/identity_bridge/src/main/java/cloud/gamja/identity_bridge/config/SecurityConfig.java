package cloud.gamja.identity_bridge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final ClientRegistrationRepository clients;

    /**
     * 1) API 전용 체인: /api/** 에만 매칭
     * - 인증 안 되어 있으면 "무조건 401" (절대 302로 리다이렉트하지 않음)
     * - 요청 캐시 저장도 금지(로그인 후 API로 재리다이렉트 방지)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new RegexRequestMatcher("^/api/.*", null))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .requestCache(c -> c.requestCache(new NullRequestCache()));
        return http.build();
    }

    /**
     * 2) 웹 기본 체인: 나머지 모든 경로
     * - 로그인은 OIDC 리다이렉트(302) 유지
     * - 실패/401 등은 홈(/)로 돌려 UX 단순화(선택)
     * - OIDC 로그아웃 후 /로 복귀
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        var oidcLogout = new OidcClientInitiatedLogoutSuccessHandler(clients);
        oidcLogout.setPostLogoutRedirectUri("{baseUrl}/");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/assets/**", "/favicon.ico", "/actuator/health").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2Login(o -> o
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((req, res, ex) -> res.sendRedirect("/"))
                )
                .oauth2Client(Customizer.withDefaults())
                .logout(l -> l.logoutSuccessHandler(oidcLogout))
                // (선택) 웹 영역의 익명 접근에서 인증 필요할 때는 로그인 페이지로
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"))
                );
        return http.build();
    }
}
