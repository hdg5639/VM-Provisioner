package cloud.gamja.user.sshkey;

import cloud.gamja.user.sshkey.domain.SshKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SshKeyRepository extends JpaRepository<SshKey, Long> {
    List<SshKey> findByUser_IdOrderByCreatedAtDesc(Long userId);
    boolean existsByPublicKey(String publicKey);
}
