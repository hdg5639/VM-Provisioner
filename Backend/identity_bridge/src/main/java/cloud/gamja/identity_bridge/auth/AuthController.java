package cloud.gamja.identity_bridge.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/auth/login")
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }
}
