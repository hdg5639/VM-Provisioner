package cloud.gamja.vm.vmkey;

import cloud.gamja.vm.vmkey.domain.VmKey;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VmKeyRepository extends R2dbcRepository<VmKey, UUID> {
}
