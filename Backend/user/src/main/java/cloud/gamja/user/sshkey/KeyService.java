package cloud.gamja.user.sshkey;

import cloud.gamja.user.sshkey.domain.SshKey;
import cloud.gamja.user.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KeyService {
    private final SshKeyRepository sshKeyRepository;
    private final SshKeyUtil util;

    @Transactional(readOnly = true)
    public List<SshKey> listByUserId(Long userId) {
        return sshKeyRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public SshKey addKeyForUser(User user, String name, String publicKey) throws NoSuchAlgorithmException {
        util.validate(publicKey);
        if (sshKeyRepository.existsByPublicKey(publicKey))
            throw new IllegalArgumentException("Duplicate key");
        String fp = util.fingerprint(publicKey);
        return sshKeyRepository.save(SshKey.builder()
                .user(user).name(name).publicKey(publicKey).fingerprint(fp).build());
    }

    @Transactional
    public void deleteKey(Long keyId, User requester, boolean isAdmin) throws AccessDeniedException {
        SshKey k = sshKeyRepository.findById(keyId)
                .orElseThrow(() -> new NoSuchElementException("Not found"));
        if (!isAdmin && !Objects.equals(k.getUser().getId(), requester.getId()))
            throw new AccessDeniedException("Not your key");
        sshKeyRepository.delete(k);
    }
}