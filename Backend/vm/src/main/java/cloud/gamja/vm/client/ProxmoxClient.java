package cloud.gamja.vm.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;


@Component
@RequiredArgsConstructor
public class ProxmoxClient {
    private final WebClient webClient = WebClient.builder().build();

    @Value("${custom.proxmox.base-url}")
    private String baseUrl;
    @Value("${custom.proxmox.api-token-id}")
    private String tokenId;
    @Value("${custom.proxmox.api-token}")
    private String tokenValue;

    public Mono<Map<String,Object>> getNodes() {
        return webClient.get()
                .uri(baseUrl + "/nodes")
                .header(HttpHeaders.AUTHORIZATION,
                        "PVEAPIToken=" + tokenId + "=" + tokenValue)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }
}
