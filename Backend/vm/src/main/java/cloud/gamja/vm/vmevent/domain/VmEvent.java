package cloud.gamja.vm.vmevent.domain;

import cloud.gamja.vm.vmevent.enums.Actions;
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
    UUID id;

    @Column("vm_id")
    UUID vmId;

    @Column("actor_user_id")
    UUID actorUserId;

    @Column("action")
    Actions action;

    @Column("payload")
    String payload;

    @CreatedDate
    @Column("timestamp")
    Instant timestamp;
}
