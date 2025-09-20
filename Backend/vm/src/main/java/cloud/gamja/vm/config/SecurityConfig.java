package cloud.gamja.vm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new RegexRequestMatcher("^/api/.*", null))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(new RegexRequestMatcher("^/api/users/me$", "GET")).authenticated()

                        .requestMatchers(new RegexRequestMatcher("^/api/users/\\d+$", "GET")).hasRole("admin")

                        .requestMatchers(new RegexRequestMatcher("^/api/keys/\\d+$", "DELETE")).authenticated()

                        .requestMatchers(new RegexRequestMatcher("^/api/keys(?:/.*)?$", null)).authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter()))
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
        return jwt -> {
            var authorities = new ArrayList<GrantedAuthority>();
            var realm = jwt.getClaimAsMap("realm_access");
            if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
                for (Object r : roles) authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
            }
            return new JwtAuthenticationToken(jwt, authorities);
        };
    }
}
