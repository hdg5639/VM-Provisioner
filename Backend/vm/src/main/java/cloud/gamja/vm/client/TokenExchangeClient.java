package cloud.gamja.vm.client;

import cloud.gamja.vm.client.enums.Audience;
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
    private String userAudience;

    public String exchange(String subjectToken, Audience target) {
        MultiValueMap<String, String> form = getStringStringMultiValueMap(subjectToken, target);

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

    private MultiValueMap<String, String> getStringStringMultiValueMap(String subjectToken, Audience target) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("subject_token", subjectToken);
        form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.add("audience",
            switch (target) {
                // 이곳에 추가
                case USER -> userAudience;
            }
        );
        return form;
    }
}
