package cloud.gamja.identity_bridge.downstream;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MultiProxyController {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection","keep-alive","transfer-encoding","te","trailer",
            "proxy-authorization","proxy-authenticate","upgrade",
            "host","content-length","authorization"
    );

    private final WebClient.Builder webClientBuilder;
    private final DownstreamRoutesProperties props;
    private final TokenExchanger tokenExchanger;

    // Audience 리스트
    @Value("${downstream.auth.aud.user}")
    private String userAudience;
    @Value("${downstream.auth.aud.vm}")
    private String vmAudience;

    @RequestMapping(
            value = "/api/ds/{target}/**",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.ALL_VALUE
    )
    public ResponseEntity<byte[]> proxy(
            @PathVariable String target,
            HttpServletRequest req,
            @RequestBody(required = false) byte[] body,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) {
        String base = props.getRoutes().get(target);
        if (base == null || base.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Unknown target: " + target).getBytes(StandardCharsets.UTF_8));
        }

        String prefix = "/api/ds/" + target + "/";
        String uri = req.getRequestURI();
        int idx = uri.indexOf(prefix);
        String tail = (idx >= 0) ? uri.substring(idx + prefix.length()) : "";
        String qs = (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        String url = base.replaceAll("/+$", "") + "/" + tail + qs;

        HttpMethod method = HttpMethod.valueOf(req.getMethod());
        WebClient webClient = webClientBuilder.build();

        log.info("Proxying {} {} to {}", method, req.getRequestURI(), url);

        try {
            // 1) 원본 토큰 추출(요청 헤더 우선, 없으면 OAuth2AuthorizedClient)
            String incomingAuth = Optional.ofNullable(req.getHeader(HttpHeaders.AUTHORIZATION))
                    .map(h -> h.replaceFirst("Bearer ", "").trim())
                    .orElseGet(() -> client != null ? client.getAccessToken().getTokenValue() : null);

            if (incomingAuth == null || incomingAuth.isBlank()) {
                log.warn("No incoming Authorization token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2) 타깃별 audience 결정
            // 추가될 때 마다 여기 작성
            String audience = switch (target) {
                case "user" -> userAudience;
                case "vm"   -> vmAudience;
                default     -> null;
            };

            // 3) 필요 시 토큰 교환
            String outboundToken = incomingAuth;
            if (audience != null && !audience.isBlank()) {
                String exchanged = tokenExchanger.exchange(incomingAuth, audience).block();
                if (exchanged == null || exchanged.isBlank()) {
                    log.warn("Token exchange failed for audience={}", audience);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(("Token exchange failed for audience=" + audience).getBytes(StandardCharsets.UTF_8));
                }
                outboundToken = exchanged;
                log.debug("Token exchanged for audience={}", audience);
            }
            final String tokenToSend = outboundToken;

            // 4) 요청 전송
            WebClient.RequestBodySpec spec = webClient
                    .method(method)
                    .uri(url)
                    .headers(h -> {
                        Collections.list(req.getHeaderNames()).forEach(name -> {
                            if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                                Collections.list(req.getHeaders(name)).forEach(v -> h.add(name, v));
                            }
                        });
                        h.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
                        h.setBearerAuth(tokenToSend);
                    });

            Mono<ResponseEntity<byte[]>> responseMono =
                    (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)
                            && body != null && body.length > 0
                            ? spec.bodyValue(body).exchangeToMono(this::handleResponse)
                            : spec.exchangeToMono(this::handleResponse);

            ResponseEntity<byte[]> response = responseMono.block();
            if (response == null) {
                log.error("No response from downstream: {}", url);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(("No response from downstream: " + url).getBytes(StandardCharsets.UTF_8));
            }

            byte[] responseBody = response.getBody();
            int bodyLength = responseBody != null ? responseBody.length : 0;
            log.info("Response received - Status: {}, Body length: {}", response.getStatusCode(), bodyLength);

            if (bodyLength > 0 && bodyLength < 2000) {
                try { log.debug("Response body: {}", new String(responseBody, StandardCharsets.UTF_8)); }
                catch (Exception ignored) { log.debug("Response body is not UTF-8 text, len={}", bodyLength); }
            }

            return response;

        } catch (Exception e) {
            log.error("Error proxying request to {}: {}", url, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("Error proxying request: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private Mono<ResponseEntity<byte[]>> handleResponse(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .map(body -> {
                    HttpHeaders outHeaders = new HttpHeaders();
                    clientResponse.headers().asHttpHeaders().forEach((k, v) -> {
                        if (!HOP_BY_HOP.contains(k.toLowerCase())) outHeaders.put(k, v);
                    });
                    outHeaders.set("X-DS-STATUS", String.valueOf(clientResponse.statusCode().value()));
                    outHeaders.set("X-DS-LEN", String.valueOf(body.length));
                    clientResponse.headers().contentType().ifPresent(ct -> outHeaders.set("X-DS-CT", ct.toString()));
                    HttpStatus status = HttpStatus.resolve(clientResponse.statusCode().value());
                    return new ResponseEntity<>(body, outHeaders, status != null ? status : HttpStatus.OK);
                });
    }
}