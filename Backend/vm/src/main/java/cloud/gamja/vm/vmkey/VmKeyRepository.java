package cloud.gamja.vm.vmkey;

import cloud.gamja.vm.vmkey.domain.VmKey;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VmKeyRepository extends ReactiveCrudRepository<VmKey, UUID> {
}
