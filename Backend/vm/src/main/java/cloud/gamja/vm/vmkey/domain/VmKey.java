package cloud.gamja.vm.vmkey.domain;

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
    UUID id;

    @Column("vm_id")
    UUID vmId;

    @Column("key_name")
    String keyName;

    @Column("fingerprint")
    String fingerprint;

    @CreatedDate
    @Column("created_at")
    Instant createdAt;
}