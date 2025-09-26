package cloud.gamja.vm.vms;

import cloud.gamja.vm.vms.enums.VmType;
import cloud.gamja.vm.vms.service.VmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VmController {
    private final VmService vmService;

    private String sub(JwtAuthenticationToken auth) {
        return auth.getToken().getSubject();
    }

    @GetMapping("/test")
    public Mono<Map<String, Object>> testCall() {
        return vmService.listNodes();
    }

    // Test
    @PostMapping("/vm")
    public Mono<Map<String, Object>> createVm( JwtAuthenticationToken auth,
                                               String userId,
                                               String fingerprint,
                                               VmType vmType,
                                               String name,
                                               Integer disk,
                                               String ide) {
        log.info("Create vm start");
        log.info("userId = " + userId);
        log.info("fingerprint = " + fingerprint);
        log.info("vmType = " + vmType);
        log.info("name = " + name);
        log.info("disk = " + disk);
        log.info("ide = " + ide);
        return vmService.createVm(sub(auth), userId, fingerprint, vmType, name, disk, ide);
    }
}
