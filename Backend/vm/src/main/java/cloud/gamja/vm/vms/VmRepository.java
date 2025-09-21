package cloud.gamja.vm.vms;

import cloud.gamja.vm.vms.domain.Vm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VmRepository extends JpaRepository<Vm, UUID> {
}
