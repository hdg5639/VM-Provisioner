package cloud.gamja.vm.vmkey.domain;

import cloud.gamja.vm.vms.domain.Vm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vm_keys")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VmKey {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vm_id", nullable = false)
    private Vm vm;

    @Column(nullable=false)
    private String keyName;

    @Column(nullable=false, unique=true)
    private String fingerprint;

    private Instant createdAt;

    @PrePersist
    void onC(){createdAt= Instant.now();}
}
