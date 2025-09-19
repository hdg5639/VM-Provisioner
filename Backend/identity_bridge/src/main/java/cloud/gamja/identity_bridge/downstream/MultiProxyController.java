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

        // 1) 대상 베이스 URL 찾기
        String base = props.getRoutes().get(target);
        if (base == null || base.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Unknown target: " + target).getBytes());
        }

        // 2) 원래 경로/쿼리 재구성
        String prefix = "/api/ds/" + target + "/";
        String uri = req.getRequestURI();
        int idx = uri.indexOf(prefix);
        String tail = (idx >= 0) ? uri.substring(idx + prefix.length()) : "";
        String qs = (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        String url = base.replaceAll("/+$", "") + "/" + tail + qs;

        HttpMethod method = HttpMethod.valueOf(req.getMethod());
        WebClient webClient = webClientBuilder.build();

        // 3) 요청 헤더 복사 + Bearer 토큰 부착
        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(url)
                .headers(h -> {
                    Collections.list(req.getHeaderNames()).forEach(name -> {
                        if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                            Collections.list(req.getHeaders(name)).forEach(v -> h.add(name, v));
                        }
                    });
                    h.set(HttpHeaders.ACCEPT_ENCODING, "identity");
                    // 사용자 access_token 부착
                    h.setBearerAuth(client.getAccessToken().getTokenValue());
                });

        ClientResponse cr = (
                (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)
                        && body != null && body.length > 0
        )
                ? spec.bodyValue(body).exchangeToMono(Mono::just).block()
                : spec.exchangeToMono(Mono::just).block();

        if (cr == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("No response from downstream: " + url).getBytes(StandardCharsets.UTF_8));
        }

        // 바디를 확실히 흡수
        byte[] resp = cr.bodyToMono(byte[].class).blockOptional().orElse(new byte[0]);

        log.info("resp: {}", resp);

        // 응답 헤더 구성
        HttpHeaders out = new HttpHeaders();
        cr.headers().asHttpHeaders().forEach((k, v) -> {
            String lower = k.toLowerCase();
            if (!HOP_BY_HOP.contains(lower)) out.put(k, v);
        });

        // 진단용 헤더
        out.set("X-DS-URL", url);
        out.set("X-DS-LEN", String.valueOf(resp.length));
        cr.headers().contentType().ifPresent(ct -> out.set("X-DS-CT", ct.toString()));

        HttpStatus status = HttpStatus.resolve(cr.statusCode().value());
        return new ResponseEntity<>(resp, out, (status != null ? status : HttpStatus.OK));
    }
}
