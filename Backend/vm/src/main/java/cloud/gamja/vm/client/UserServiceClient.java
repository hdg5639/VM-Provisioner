package cloud.gamja.vm.client;

import cloud.gamja.vm.client.record.KeyDto;
import cloud.gamja.vm.client.record.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${custom.user.base-url}")
    private String baseUrl;

    public Mono<UserDto> getUser(String exchangedToken, String userId) {
        return webClient.get()
                .uri(baseUrl + "/api/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + exchangedToken)
                .retrieve()
                .bodyToMono(UserDto.class);
    }

    public Mono<UserDto> getMe(String exchangedToken) {
        return webClient.get()
                .uri(baseUrl + "/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + exchangedToken)
                .retrieve()
                .bodyToMono(UserDto.class);
    }

    public Mono<List<KeyDto>> getSshKeys(String exchangedToken) {
        return webClient.get()
                .uri(baseUrl + "/api/keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + exchangedToken)
                .retrieve()
                .bodyToFlux(KeyDto.class)
                .collectList();
    }
}
