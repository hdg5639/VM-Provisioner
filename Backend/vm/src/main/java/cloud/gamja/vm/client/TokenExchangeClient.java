package cloud.gamja.vm.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenExchangeClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${custom.kc.token-endpoint}")
    private String tokenEndpoint;
    @Value("${custom.kc.client-id}")
    private String clientId;
    @Value("${custom.kc.client-secret}")
    private String clientSecret;
    @Value("${custom.user.audience}")
    private String audience;

    public String userExchange(String userAccessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("subject_token", userAccessToken);
        form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("audience", audience);

        var resp = webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (resp == null || !resp.containsKey("access_token")) {
            throw new IllegalStateException("Token exchange failed: " + resp);
        }
        return (String) resp.get("access_token");
    }
}
