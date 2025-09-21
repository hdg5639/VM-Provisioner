package cloud.gamja.vm.vms;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VmController {
    private String sub(JwtAuthenticationToken auth) {
        return auth.getToken().getSubject();
    }
}
