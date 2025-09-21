package cloud.gamja.vm.vmevent.domain;

import cloud.gamja.vm.vmevent.enums.Actions;
import cloud.gamja.vm.vms.domain.Vm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vm_events")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VmEvent {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "vm_id", nullable = false)
    private Vm vm;

    @Column(name = "actor_user_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID actorUserId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Actions action;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    private Instant timestamp;

    @PrePersist void onCreate() {timestamp = Instant.now();}
}
