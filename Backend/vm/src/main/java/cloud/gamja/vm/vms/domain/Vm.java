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
    UUID id;

    @Column("owner_user_id")
    UUID ownerUserId;

    @Column("owner_tenant_id")
    UUID ownerTenantId;

    @Column("detail")
    VmDetail detail;

    Boolean active;

    @CreatedDate
    @Column("created_at")
    Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    Instant updatedAt;
}
