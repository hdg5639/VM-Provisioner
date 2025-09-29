package cloud.gamja.vm.vms;

import cloud.gamja.vm.vmevent.record.EventInfo;
import cloud.gamja.vm.vms.record.VmRequest;
import cloud.gamja.vm.vms.service.VmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VmController {
    private final VmService vmService;

    private String acessToken(JwtAuthenticationToken auth) {
        return auth.getToken().getTokenValue();
    }
    private String sub(JwtAuthenticationToken auth) {
        return auth.getToken().getSubject();
    }

    @GetMapping("/test")
    public Mono<Map<String, Object>> testCall() {
        return vmService.listNodes();
    }

    // Test
    @PostMapping("/vm")
    public Mono<EventInfo> createVm(JwtAuthenticationToken auth,
                                    @RequestBody VmRequest vmRequest) {
        log.info("Create vm start");
        log.info("Request: {}", vmRequest);
        log.info("Token sub: {}", sub(auth));
        return vmService.createVm(acessToken(auth),
                vmRequest.fingerprint(),
                vmRequest.vmType(),
                vmRequest.name(),
                vmRequest.disk(),
                vmRequest.ide());
    }
}
