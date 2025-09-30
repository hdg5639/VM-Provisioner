package cloud.gamja.vm.vms;

import cloud.gamja.vm.vms.domain.Vm;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VmRepository extends R2dbcRepository<Vm, UUID> {
    Flux<Vm> findByOwnerUserId(UUID id);
}
