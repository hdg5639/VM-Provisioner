package cloud.gamja.identity_bridge.downstream;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenExchanger {

    private final WebClient web = WebClient.builder().build();

    @Value("${downstream.auth.token-endpoint}") String tokenEndpoint;
    @Value("${downstream.auth.client-id}")      String clientId;
    @Value("${downstream.auth.client-secret}")  String clientSecret;

    public Mono<String> exchange(String subjectToken,
                                 @Nullable Collection<String> audiences,
                                 @Nullable String scope) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("subject_token", subjectToken);
        form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

        if (audiences != null) {
            audiences.stream().filter(a -> a != null && !a.isBlank())
                    .forEach(a -> form.add("audience", a));
        }

        if (scope != null && !scope.isBlank()) {
            form.add("scope", scope);
        }

        return web.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .map(m -> (String) m.get("access_token"));
                    }
                    return resp.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "Token exchange failed: HTTP " + resp.statusCode().value() + " body=" + body)));
                });
    }

    public Mono<String> exchange(String subjectToken, String audience) {
        return exchange(subjectToken,
                audience == null || audience.isBlank() ? null : java.util.List.of(audience),
                null);
    }
}
