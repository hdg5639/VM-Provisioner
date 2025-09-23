package cloud.gamja.identity_bridge.downstream;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenExchanger {

    private final WebClient web = WebClient.builder().build();

    @Value("${downstream.auth.token-endpoint}") String tokenEndpoint;
    @Value("${downstream.auth.client-id}")      String clientId;
    @Value("${downstream.auth.client-secret}")  String clientSecret;

    public Mono<String> exchange(String subjectToken, String audience) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("subject_token", subjectToken);
        form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("audience", audience);

        return web.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(m -> (String) m.get("access_token"));
    }
}
