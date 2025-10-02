package cloud.gamja.vm.vmkey.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("vm_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column("vm_id")
    private UUID vmId;

    @Column("key_name")
    private String keyName;

    @Column("fingerprint")
    private String fingerprint;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}