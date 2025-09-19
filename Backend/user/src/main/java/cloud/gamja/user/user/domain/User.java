package cloud.gamja.user.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="users", indexes=@Index(name="uk_users_external_id", columnList="externalId", unique=true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, unique=true, name="external_id") String externalId; // Keycloak sub
    String email;
    String displayName;
    Instant createdAt; Instant updatedAt;
    @PrePersist void onC(){createdAt=updatedAt=Instant.now();}
    @PreUpdate  void onU(){updatedAt=Instant.now();}
}