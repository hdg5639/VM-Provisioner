package cloud.gamja.user.user;

import cloud.gamja.user.sshkey.KeyService;
import cloud.gamja.user.sshkey.domain.SshKey;
import cloud.gamja.user.sshkey.record.CreateKeyReq;
import cloud.gamja.user.sshkey.record.KeyDto;
import cloud.gamja.user.user.domain.User;
import cloud.gamja.user.user.record.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final KeyService keyService;

    private String sub(JwtAuthenticationToken auth){
        return auth.getToken().getSubject();
    }

    private boolean isAdmin(JwtAuthenticationToken auth){
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
    }

    /** 최초 접근 시에도 보장되도록 me에서 upsert */
    @GetMapping("/users/me")
    public UserDto me(JwtAuthenticationToken auth) {
        String s = sub(auth);
        String email = auth.getToken().getClaimAsString("email");
        String name  = Optional.ofNullable(auth.getToken().getClaimAsString("name"))
                .orElseGet(() -> auth.getToken().getClaimAsString("preferred_username"));
        User u = userService.upsertBySub(s, email, name);
        return new UserDto(u.getId(), u.getExternalId(), u.getEmail(), u.getDisplayName());
    }

    @GetMapping("/users/{id}")
    public UserDto getById(@PathVariable Long id) {
        User u = userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto(u.getId(), u.getExternalId(), u.getEmail(), u.getDisplayName());
    }

    @GetMapping("/api/keys")
    public List<KeyDto> myKeys(JwtAuthenticationToken auth) {
        User me = userService.findBySub(sub(auth)).orElseThrow();
        return keyService.listByUserId(me.getId()).stream()
                .map(k -> new KeyDto(k.getId(), k.getName(), k.getFingerprint(), k.getPublicKey(), k.getCreatedAt()))
                .toList();
    }

    @PostMapping("/keys")
    public KeyDto addKey(@Valid @RequestBody CreateKeyReq req, JwtAuthenticationToken auth) throws NoSuchAlgorithmException {
        User me = userService.findBySub(sub(auth)).orElseThrow();
        SshKey k = keyService.addKeyForUser(me, req.name(), req.publicKey());
        return new KeyDto(k.getId(), k.getName(), k.getFingerprint(), k.getPublicKey(), k.getCreatedAt());
    }

    @DeleteMapping("/keys/{id}")
    public void delKey(@PathVariable Long id, JwtAuthenticationToken auth) throws AccessDeniedException {
        User me = userService.findBySub(sub(auth)).orElseThrow();
        keyService.deleteKey(id, me, isAdmin(auth));
    }
}