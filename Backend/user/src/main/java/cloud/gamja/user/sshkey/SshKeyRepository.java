package cloud.gamja.user.sshkey;

import cloud.gamja.user.sshkey.domain.SshKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SshKeyRepository extends JpaRepository<SshKey, UUID> {
    List<SshKey> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    boolean existsByPublicKey(String publicKey);
}
