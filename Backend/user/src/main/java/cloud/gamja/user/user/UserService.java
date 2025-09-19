package cloud.gamja.user.user;

import cloud.gamja.user.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    @Transactional
    public User upsertBySub(String sub, String email, String name) {
        return users.findByExternalId(sub).orElseGet(() ->
                users.save(User.builder().externalId(sub).email(email).displayName(name).build()));
    }
    public Optional<User> findById(Long id){ return users.findById(id); }
    public Optional<User> findBySub(String sub){ return users.findByExternalId(sub); }
}