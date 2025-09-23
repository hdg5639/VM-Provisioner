package cloud.gamja.vm.vms;

import cloud.gamja.vm.vms.service.VmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VmController {
    private final VmService vmService;

    private String sub(JwtAuthenticationToken auth) {
        return auth.getToken().getSubject();
    }

    @GetMapping
    public Mono<Map<String, Object>> testCall() {
        return vmService.callTest();
    }
}
