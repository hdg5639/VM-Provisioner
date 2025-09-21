package cloud.gamja.vm.vms.domain;

import cloud.gamja.vm.vms.record.VmDetail;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vms", indexes = {
        @Index(name = "uk_users_owner_id", columnList = "ownerUserId", unique = true)
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vm {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID ownerUserId;

    // 팀/프로젝트 단위의 공유/격리/청구/쿼터를 위한 id -> 당장은 필요없지만, 미리 생성
    @Column(name = "owner_tenant_id", unique = true)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID ownerTenantId;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private VmDetail detail;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onC(){createdAt=updatedAt=Instant.now();}
    @PreUpdate  void onU(){updatedAt=Instant.now();}
}
