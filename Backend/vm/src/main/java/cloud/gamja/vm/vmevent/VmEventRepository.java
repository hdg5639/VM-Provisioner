package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VmEventRepository extends ReactiveCrudRepository<VmEvent, UUID> {
    Flux<VmEvent> findByVmId(UUID vmId);
}
