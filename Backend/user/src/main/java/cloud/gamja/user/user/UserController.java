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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final KeyService keyService;

    private String sub(JwtAuthenticationToken auth) {
        return auth.getToken().getSubject();
    }
    private String email(JwtAuthenticationToken auth) {
        return auth.getToken().getClaimAsString("email");
    }
    private String name(JwtAuthenticationToken auth) {
        String n = auth.getToken().getClaimAsString("name");
        return (n != null && !n.isBlank())
                ? n
                : auth.getToken().getClaimAsString("preferred_username");
    }
    private boolean isAdmin(JwtAuthenticationToken auth){
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
    }

    @GetMapping(value="/users/me", produces= MediaType.APPLICATION_JSON_VALUE)
    public UserDto me(JwtAuthenticationToken auth) {
        User u = userService.upsertBySub(sub(auth), email(auth), name(auth));
        return new UserDto(u.getId(), u.getExternalId(), u.getEmail(), u.getDisplayName());
    }

    @GetMapping("/users/{id}")
    public UserDto getById(@PathVariable Long id) {
        User u = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto(u.getId(), u.getExternalId(), u.getEmail(), u.getDisplayName());
    }

    @GetMapping(value="/keys", produces=MediaType.APPLICATION_JSON_VALUE)
    public List<KeyDto> myKeys(JwtAuthenticationToken auth) {
        User me = userService.getOrCreateBySub(sub(auth), email(auth), name(auth));
        return keyService.listByUserId(me.getId()).stream()
                .map(k -> new KeyDto(k.getId(), k.getName(), k.getFingerprint(), k.getPublicKey(), k.getCreatedAt()))
                .toList();
    }

    @PostMapping(value="/keys",
            consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KeyDto addKey(@Valid @RequestBody CreateKeyReq req, JwtAuthenticationToken auth) throws NoSuchAlgorithmException {
        User me = userService.getOrCreateBySub(sub(auth), email(auth), name(auth));
        SshKey k = keyService.addKeyForUser(me, req.name(), req.publicKey());
        return new KeyDto(k.getId(), k.getName(), k.getFingerprint(), k.getPublicKey(), k.getCreatedAt());
    }

    @DeleteMapping("/keys/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delKey(@PathVariable Long id, JwtAuthenticationToken auth) {
        User me = userService.getOrCreateBySub(sub(auth), email(auth), name(auth));
        keyService.deleteKey(id, me, isAdmin(auth));
    }
}
