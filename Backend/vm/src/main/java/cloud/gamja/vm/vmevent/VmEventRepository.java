package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import cloud.gamja.vm.vms.domain.Vm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VmEventRepository extends JpaRepository<VmEvent, UUID> {
    List<VmEvent> findByVm(Vm vm);
}
