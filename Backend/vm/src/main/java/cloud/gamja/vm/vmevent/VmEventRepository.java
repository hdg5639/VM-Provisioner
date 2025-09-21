package cloud.gamja.vm.vmevent;

import cloud.gamja.vm.vmevent.domain.VmEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VmEventRepository extends JpaRepository<VmEvent, UUID> {
}
