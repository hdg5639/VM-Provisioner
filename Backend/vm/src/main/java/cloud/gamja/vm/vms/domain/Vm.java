package cloud.gamja.vm.vms.domain;

import cloud.gamja.vm.vms.record.VmDetail;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("vms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vm {

    @Id
    private UUID id;

    @Column("owner_user_id")
    private UUID ownerUserId;

    @Column("owner_tenant_id")
    private UUID ownerTenantId;

    @Column("detail")
    private VmDetail detail;

    private Boolean active;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}
