package cloud.gamja.vm.vms.domain;

import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.*;

import java.util.UUID;

@Enabled
@Table(name = "vms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vm {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;
}
