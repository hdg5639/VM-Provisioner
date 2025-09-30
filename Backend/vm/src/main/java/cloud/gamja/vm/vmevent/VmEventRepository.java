package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vms.domain.Vm;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VmEventRepository extends R2dbcRepository<VmEvent, UUID> {
    Flux<VmEvent> findByVm(Vm vm);
}
