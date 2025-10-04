package cloud.gamja.vm.vmevent.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("vm_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmEvent {

    @Id
    private UUID id;

    @Column("vm_id")
    private UUID vmId;

    @Column("actor_user_id")
    private UUID actorUserId;

    @Column("action")
    private String action;

    @Column("payload")
    private String payload;

    @CreatedDate
    @Column("timestamp")
    private Instant timestamp;
}
