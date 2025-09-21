package cloud.gamja.user.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users", indexes=@Index(name="uk_users_external_id", columnList="externalId", unique=true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;
    @Column(nullable=false, unique=true, name="external_id")
    private String externalId; // Keycloak sub
    private String email;
    private String displayName;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void onC(){createdAt=updatedAt=Instant.now();}
    @PreUpdate  void onU(){updatedAt=Instant.now();}
}