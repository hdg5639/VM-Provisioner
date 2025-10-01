package cloud.gamja.vm.vms;

import cloud.gamja.vm.vms.domain.Vm;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VmRepository extends ReactiveCrudRepository<Vm, UUID> {
    Flux<Vm> findByOwnerUserId(UUID id);
}
