package cloud.gamja.identity_bridge.downstream;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MultiProxyController {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "transfer-encoding", "te", "trailer",
            "proxy-authorization", "proxy-authenticate", "upgrade",
            "host", "content-length", "authorization"
    );

    private final WebClient.Builder webClientBuilder;
    private final DownstreamRoutesProperties props;

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

        // 베이스 URL
        String base = props.getRoutes().get(target);
        if (base == null || base.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Unknown target: " + target).getBytes());
        }

        // 원본 경로
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
            // 요청 헤더 복사 + 토큰 부착
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

                        // 토큰
                        h.setBearerAuth(client.getAccessToken().getTokenValue());
                    });

            // 한 번에 응답과 바디를 처리
            Mono<ResponseEntity<byte[]>> responseMono;

            if ((method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)
                    && body != null && body.length > 0) {
                log.info("Sending request body with {} bytes", body.length);

                responseMono = spec.bodyValue(body)
                        .exchangeToMono(this::handleResponse);
            } else {
                responseMono = spec.exchangeToMono(this::handleResponse);
            }

            ResponseEntity<byte[]> response = responseMono.block();

            if (response == null) {
                log.error("No response from downstream: {}", url);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(("No response from downstream: " + url).getBytes(StandardCharsets.UTF_8));
            }

            // 응답 로깅
            byte[] responseBody = response.getBody();
            int bodyLength = responseBody != null ? responseBody.length : 0;
            log.info("Response received - Status: {}, Body length: {}", response.getStatusCode(), bodyLength);

            if (bodyLength > 0 && bodyLength < 2000) {
                try {
                    String bodyContent = new String(responseBody, StandardCharsets.UTF_8);
                    log.debug("Response body: {}", bodyContent);
                } catch (Exception e) {
                    log.debug("Response body is not UTF-8 text, length: {}", bodyLength);
                }
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
                    // 응답 헤더
                    HttpHeaders outHeaders = new HttpHeaders();
                    clientResponse.headers().asHttpHeaders().forEach((k, v) -> {
                        String lower = k.toLowerCase();
                        if (!HOP_BY_HOP.contains(lower)) {
                            outHeaders.put(k, v);
                        }
                    });

                    // 진단용 헤더
                    outHeaders.set("X-DS-STATUS", String.valueOf(clientResponse.statusCode().value()));
                    outHeaders.set("X-DS-LEN", String.valueOf(body.length));
                    clientResponse.headers().contentType()
                            .ifPresent(ct -> outHeaders.set("X-DS-CT", ct.toString()));

                    HttpStatus status = HttpStatus.resolve(clientResponse.statusCode().value());
                    return new ResponseEntity<>(body, outHeaders, status != null ? status : HttpStatus.OK);
                });
    }
}