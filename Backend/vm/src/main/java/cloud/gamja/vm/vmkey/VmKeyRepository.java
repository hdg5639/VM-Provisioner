package cloud.gamja.vm.vmkey;

import cloud.gamja.vm.vmkey.domain.VmKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VmKeyRepository extends JpaRepository<VmKey, UUID> {
}
